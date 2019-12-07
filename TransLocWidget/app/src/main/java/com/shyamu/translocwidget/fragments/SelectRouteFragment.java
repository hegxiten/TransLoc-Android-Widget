package com.shyamu.translocwidget.fragments;

import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.shyamu.translocwidget.R;
import com.shyamu.translocwidget.bl.ArrivalTimeWidget;
import com.shyamu.translocwidget.bl.Utils;
import com.shyamu.translocwidget.rest.model.TransLocRoute;
import com.shyamu.translocwidget.rest.service.ServiceGenerator;
import com.shyamu.translocwidget.rest.service.TransLocClient;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import static com.shyamu.translocwidget.bl.Utils.TransLocDataType.ROUTE;
import static com.shyamu.translocwidget.bl.Utils.TransLocDataType.VEHICLE;

public class SelectRouteFragment extends BaseFragment {

    private final String TAG = this.getTag();
    private ListView routeListView;
    private ProgressBar progressBar;

    private ArrivalTimeWidget atw;
    private Subscription routesSub;

    public SelectRouteFragment() {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_route, container, false);

        Bundle args = getArguments();
        if (args  != null && args.containsKey("atw")) {
            atw = (ArrivalTimeWidget) args.getSerializable("atw");
        } else {
            throw new IllegalStateException("No atw received from SelectAgencyFragment");
        }

        setHasOptionsMenu(true);
        getActivity().setTitle("Select a Route");
        routeListView = (ListView) rootView.findViewById(R.id.lvRouteList);
        progressBar = (ProgressBar) getActivity().findViewById(R.id.pbLoading);
        progressBar.setVisibility(View.VISIBLE);
        TransLocClient client =
                ServiceGenerator.createService(TransLocClient.class,
                        Utils.BASE_URL,
                        TRANSLOC_API_KEY,
                        atw.getAgencyID(),
                        ROUTE);
        routesSub = client.routes(atw.getAgencyID())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::populateRoutesListView,
                        e -> handleServiceErrors(ROUTE, e, progressBar)
                );

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(routesSub != null) routesSub.unsubscribe();
    }

    private void populateRoutesListView(List<TransLocRoute> routes) {
        progressBar.setVisibility(View.INVISIBLE);
        if (routes != null && !routes.isEmpty()) {
            ArrayAdapter<TransLocRoute> routeArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, routes);
            routeListView.setAdapter(routeArrayAdapter);

            // Animate
            TranslateAnimation animate = new TranslateAnimation(routeListView.getWidth(),0,0,0);
            animate.setDuration(250);
            animate.setFillAfter(true);
            routeListView.startAnimation(animate);
            routeListView.setVisibility(View.VISIBLE);

            // Set onclicklistener to open select stops fragment
            routeListView.setOnItemClickListener((parent, view, position, id) -> {
                TransLocRoute selectedRoute = (TransLocRoute) parent.getItemAtPosition(position);
                atw.setRouteID(Integer.toString(selectedRoute.routeID));
                atw.setRouteName(selectedRoute.toString());
                Log.d(TAG, "color: " + selectedRoute.color);
                if(selectedRoute.color != null ) atw.setBackgroundColor(Color.parseColor("#" + selectedRoute.color));

                SelectStopFragment selectStopFragment = new SelectStopFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable("atw", atw);
                selectStopFragment.setArguments(bundle);

                // Insert the fragment by replacing any existing fragment
                FragmentManager fragmentManager = getActivity().getFragmentManager();
                fragmentManager.beginTransaction()
                        .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                        .replace(R.id.widget_container, selectStopFragment)
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            Log.e(TAG, "Routes data was null or empty!");
            Utils.showAlertDialog(getActivity(), "No routes available", "Please select another agency or try again later.", true);
        }
    }
}
