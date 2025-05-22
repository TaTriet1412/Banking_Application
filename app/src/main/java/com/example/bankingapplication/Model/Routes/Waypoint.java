package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;

public class Waypoint {
    @SerializedName("location")
    private Location_Routes location;
    // Bạn có thể thêm các trường khác cho Waypoint nếu cần (ví dụ: placeId)

    public Waypoint(Location_Routes location) {
        this.location = location;
    }
    // Getter
    public Location_Routes getLocation() { return location; }
}