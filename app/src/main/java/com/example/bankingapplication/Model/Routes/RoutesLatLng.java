package com.example.bankingapplication.Model.Routes; // Hoặc com.example.bankingapplication.Object

import com.google.gson.annotations.SerializedName;

public class RoutesLatLng {
    @SerializedName("latitude")
    private double latitude;
    @SerializedName("longitude")
    private double longitude;

    public RoutesLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters (Không cần setters nếu chỉ dùng để gửi đi)
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}