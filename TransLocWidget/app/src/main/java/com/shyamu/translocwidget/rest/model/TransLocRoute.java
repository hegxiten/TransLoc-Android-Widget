package com.shyamu.translocwidget.rest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TransLocRoute{

    @SerializedName("route_id")
    public int routeID;

    @SerializedName("short_name")
    private String shortName;

    @SerializedName("long_name")
    private String longName;

    @SerializedName("color")
    public String color;

    @SerializedName("segments")
    public List<List<String>> segments;

    public String toString() {
        return segments.toString();
    }
}