package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;

public class Duration {
    @SerializedName("text")
    public String text;

    @SerializedName("value")
    public int value; // in seconds
}