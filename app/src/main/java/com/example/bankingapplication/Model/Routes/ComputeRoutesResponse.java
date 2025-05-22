package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ComputeRoutesResponse {
    @SerializedName("routes")
    private List<Route> routes;

    @SerializedName("error") // Thêm trường này để bắt lỗi từ API
    private RouteError error;

    // Getters
    public List<Route> getRoutes() { return routes; }
    public RouteError getError() { return error; }
}