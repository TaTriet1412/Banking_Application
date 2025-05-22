package com.example.bankingapplication.Model.Routes;

import com.google.gson.annotations.SerializedName;

public class ComputeRoutesRequest {
    @SerializedName("origin")
    private Waypoint origin;

    @SerializedName("destination")
    private Waypoint destination;

    @SerializedName("travelMode")
    private String travelMode; // Ví dụ: "DRIVE", "WALK", "BICYCLE", "TWO_WHEELER"

    @SerializedName("routingPreference")
    private String routingPreference; // Ví dụ: "TRAFFIC_AWARE_OPTIMAL", "TRAFFIC_UNAWARE"

    @SerializedName("computeAlternativeRoutes")
    private boolean computeAlternativeRoutes;

    @SerializedName("languageCode")
    private String languageCode; // Ví dụ: "vi-VN"

    // Constructor
    public ComputeRoutesRequest(Waypoint origin, Waypoint destination, String travelMode) {
        this.origin = origin;
        this.destination = destination;
        this.travelMode = travelMode;
        this.routingPreference = "TRAFFIC_AWARE_OPTIMAL"; // Mặc định
        this.computeAlternativeRoutes = false; // Mặc định
        this.languageCode = "vi-VN"; // Mặc định
    }

    // Getters and Setters (chủ yếu là setters nếu bạn muốn tùy chỉnh thêm sau khi tạo)
    public Waypoint getOrigin() { return origin; }
    public void setOrigin(Waypoint origin) { this.origin = origin; }
    public Waypoint getDestination() { return destination; }
    public void setDestination(Waypoint destination) { this.destination = destination; }
    public String getTravelMode() { return travelMode; }
    public void setTravelMode(String travelMode) { this.travelMode = travelMode; }
    public String getRoutingPreference() { return routingPreference; }
    public void setRoutingPreference(String routingPreference) { this.routingPreference = routingPreference; }
    public boolean isComputeAlternativeRoutes() { return computeAlternativeRoutes; }
    public void setComputeAlternativeRoutes(boolean computeAlternativeRoutes) { this.computeAlternativeRoutes = computeAlternativeRoutes; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
}