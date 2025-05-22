package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;

public class RouteError {
    @SerializedName("code")
    private int code;
    @SerializedName("message")
    private String message;
    @SerializedName("status")
    private String status;

    // Getters
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
}