package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("routes")
    public List<Route> routes;

    @SerializedName("status")
    public String status;
}
