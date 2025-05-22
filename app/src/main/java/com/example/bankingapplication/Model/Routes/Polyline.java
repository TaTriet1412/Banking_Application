package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;

public class Polyline {
    @SerializedName("encodedPolyline")
    private String encodedPolyline;

    // Getter
    public String getEncodedPolyline() { return encodedPolyline; }
}