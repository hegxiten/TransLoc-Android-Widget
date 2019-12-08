package com.shyamu.translocwidget.bl;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shyamu.translocwidget.R;
import com.shyamu.translocwidget.widget.Provider;

import org.joda.time.DateTime;
import org.joda.time.Minutes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;


public class Utils {

    private static final String TAG = "Utils";

    public enum TransLocDataType {
        AGENCY, ROUTE, STOP, ARRIVAL, VEHICLE, SEGMENT
    }

    public static final String GET_ARRIVAL_ESTIMATES_URL = "https://transloc-api-1-2.p.rapidapi.com/arrival-estimates.json?agencies=";
    public static final String BASE_URL = "https://transloc-api-1-2.p.rapidapi.com";
    private static final String FILE_NAME = "WidgetList";
    public static final String TAP_ON_WIDGET_ACTION = "TAPPED_ON_WIDGET";


    public static void showAlertDialog(Activity activity, String title, String message, boolean goBack) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("Ok", (dialog, which) -> {
                    Log.d("AlertDialog", "Neutral");
                    if (goBack) activity.onBackPressed();
                })
                .show();
    }

    public static int getMinutesBetweenTimes(DateTime currentTime, DateTime futureTime) {
        return Minutes.minutesBetween(currentTime, futureTime).getMinutes();
    }

    private static void writeData(Context context, String data) throws IOException {
        FileOutputStream fOut = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        osw.write(data);
        osw.flush();
        osw.close();
    }

    private static String readSavedData(Context context) throws IOException {
        StringBuilder datax = new StringBuilder("");
        FileInputStream fin = context.openFileInput(FILE_NAME);
        InputStreamReader isr = new InputStreamReader(fin);
        BufferedReader reader = new BufferedReader(isr);

        String readString = reader.readLine();
        while (readString != null) {
            datax.append(readString);
            readString = reader.readLine();
        }
        isr.close();
        return datax.toString();
    }

    public static ArrayList<ArrivalTimeWidget> getArrivalTimeWidgetsFromStorage(Context context) throws IOException {
        String widgetListJsonStr = Utils.readSavedData(context);
        return new Gson().fromJson(widgetListJsonStr, new TypeToken<ArrayList<ArrivalTimeWidget>>() {
        }.getType());
    }

    public static void writeArrivalTimeWidgetsToStorage(Context context, ArrayList<ArrivalTimeWidget> widgets) throws IOException {
        String value = new Gson().toJson(widgets);
        Utils.writeData(context, value);
    }

    public static ArrivalTimeWidget getArrivalTimeWidgetFromWidgetId(ArrayList<ArrivalTimeWidget> widgets, int appWidgetId) {
        for (ArrivalTimeWidget widget : widgets) {
            if (widget.getAppWidgetId() == appWidgetId) {
                return widget;
            }
        }
        return null;
    }

    // Utility method that creates RemoteViews for the widget based on the given ArrivalTimeWidget
    public static RemoteViews createRemoteViews(Context context, ArrivalTimeWidget atw, int appWidgetId) {

        RemoteViews remoteViews;
        int widgetSize = 3;
        // for resizable widgets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            Bundle widgetOptions = manager.getAppWidgetOptions(appWidgetId);
            if (widgetOptions != null) {
                int minWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                //int minHeightDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
                if (minWidthDp <= 72) widgetSize = 1;
                else if (minWidthDp <= 160) widgetSize = 2;
                else if (minWidthDp <= 248) widgetSize = 3;
                else widgetSize = 4;
            } else {
                Log.e(TAG, "widget options is null");
            }
            remoteViews = drawWidgetForJellyBean(context, widgetSize);
        } else {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        }

        int minutesUntilArrival = atw.getMinutesUntilArrival();
        // Error state
        if (minutesUntilArrival == -1) {
            remoteViews.setTextViewText(R.id.tvRemainingTime, "?");
            remoteViews.setTextViewText(R.id.tvMins, "Tap to refresh");
        } else {
            remoteViews.setTextViewText(R.id.tvRemainingTime, Integer.toString(minutesUntilArrival));
            if (minutesUntilArrival < 1) remoteViews.setTextViewText(R.id.tvRemainingTime, "<1");
            if (minutesUntilArrival < 2) remoteViews.setTextViewText(R.id.tvMins, "min away");
            else remoteViews.setTextViewText(R.id.tvMins, "mins away");
        }

        // Set on tap pending intent
        Intent widgetTapIntent = new Intent(context, Provider.class);
        widgetTapIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        widgetTapIntent.setAction(Utils.TAP_ON_WIDGET_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, widgetTapIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.rlWidgetLayout, pendingIntent);

        // set colors
        remoteViews.setInt(R.id.rlWidgetLayout, "setBackgroundColor", atw.getBackgroundColor());
        remoteViews.setTextColor(R.id.tvRoute, atw.getTextColor());
        remoteViews.setTextColor(R.id.tvStop, atw.getTextColor());
        remoteViews.setTextColor(R.id.tvRemainingTime, atw.getTextColor());
        remoteViews.setTextColor(R.id.tvMins, atw.getTextColor());

        // set text content
        if (widgetSize == 1) {
            remoteViews.setTextViewText(R.id.tvRouteAndStop, atw.getRouteName() + " - " + atw.getStopName());
            remoteViews.setTextColor(R.id.tvRouteAndStop, atw.getTextColor());
        } else {
            remoteViews.setTextViewText(R.id.tvRoute, atw.getRouteName());
            remoteViews.setTextViewText(R.id.tvStop, atw.getStopName());
        }

        return remoteViews;
    }

    // If we are on Jelly Bean or higher add the ability to have dynamically changing widget layouts based on size
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static RemoteViews drawWidgetForJellyBean(Context context, int widgetSize) {

        RemoteViews newView = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        newView.removeAllViews(R.id.rlWidgetLayout);

        if (widgetSize == 3) {
            RemoteViews threeWidget = new RemoteViews(context.getPackageName(), R.layout.widget_layout_three);
            newView.addView(R.id.rlWidgetLayout, threeWidget);
        } else if (widgetSize == 2) {
            RemoteViews twoWidget = new RemoteViews(context.getPackageName(), R.layout.widget_layout_two);
            newView.addView(R.id.rlWidgetLayout, twoWidget);
        } else if (widgetSize == 1) {
            RemoteViews oneWidget = new RemoteViews(context.getPackageName(), R.layout.widget_layout_one);
            newView.addView(R.id.rlWidgetLayout, oneWidget);
        } else {
            Log.v(TAG, "widget size not found");
            RemoteViews fourWidget = new RemoteViews(context.getPackageName(), R.layout.widget_layout_three);
            newView.addView(R.id.rlWidgetLayout, fourWidget);
        }
        return newView;
    }

}


