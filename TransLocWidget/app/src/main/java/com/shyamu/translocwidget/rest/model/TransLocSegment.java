package com.shyamu.translocwidget.rest.model;

import com.google.gson.annotations.SerializedName;

public class TransLocSegment {
    @SerializedName("segs")
    private String segmentDetail;

    public String toString() {
        return segmentDetail;
    }
}