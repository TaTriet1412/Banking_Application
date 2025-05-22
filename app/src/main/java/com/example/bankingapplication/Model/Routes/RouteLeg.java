package com.example.bankingapplication.Model.Routes;
import com.google.gson.annotations.SerializedName;

// Tương tự Leg của Directions API, nhưng có thể có trường khác
public class RouteLeg {
    @SerializedName("distanceMeters")
    public int distanceMeters;

    @SerializedName("duration")
    public String duration; // "Xs"

    @SerializedName("staticDuration") // Thời gian không tính traffic
    public String staticDuration;

    // ... các trường khác như startLocation, endLocation nếu cần
}