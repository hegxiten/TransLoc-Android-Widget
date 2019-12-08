package com.shyamu.translocwidget.rest.model;

import com.google.gson.annotations.SerializedName;

public class TransLocVehicle {
    @SerializedName("vehicle_id")
    public int vehicleId;

    @SerializedName("speed")
    public Double speed;

    @SerializedName("last_updated_on")
    public String lastUpdatedOn;

    @SerializedName("location")
    public TransLocLocation location;

    @SerializedName("heading")
    public int heading;

    public String toString(){
        return Integer.toString(vehicleId);
    }
}