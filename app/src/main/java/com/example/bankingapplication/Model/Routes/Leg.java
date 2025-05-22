package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;

public class Leg {
    @SerializedName("distance")
    public Distance distance;

    @SerializedName("duration")
    public Duration duration;
}