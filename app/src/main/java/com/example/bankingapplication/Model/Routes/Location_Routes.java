package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;

public class Location_Routes {
    @SerializedName("latLng")
    private RoutesLatLng latLng;

    public Location_Routes(RoutesLatLng latLng) {
        this.latLng = latLng;
    }
    // Getter
    public RoutesLatLng getLatLng() { return latLng; }
}