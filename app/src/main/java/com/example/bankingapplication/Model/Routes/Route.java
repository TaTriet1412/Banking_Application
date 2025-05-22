package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Route { // Tên lớp Route này có thể trùng với Route của Directions API cũ, cẩn thận import
    @SerializedName("polyline")
    private Polyline polyline;

    @SerializedName("duration")
    private String duration; // Ví dụ: "300s"

    @SerializedName("distanceMeters")
    private int distanceMeters;

    @SerializedName("legs") // Routes API V2 cũng có legs, nhưng cấu trúc có thể hơi khác
    private List<RouteLeg> legs;


    // Getters
    public Polyline getPolyline() { return polyline; }
    public String getDuration() { return duration; }
    public int getDistanceMeters() { return distanceMeters; }
    public List<RouteLeg> getLegs() { return legs; }
}