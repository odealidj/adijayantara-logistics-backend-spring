package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteMatrixResponse {

    @JsonProperty("total_routes")
    private int totalRoutes;

    @JsonProperty("avg_trip_threshold")
    private double avgTripThreshold;

    @JsonProperty("avg_margin_threshold")
    private double avgMarginThreshold;

    @JsonProperty("routes")
    private List<RouteMatrixItem> routes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteMatrixItem {
        @JsonProperty("route_name")
        private String routeName;

        @JsonProperty("trip_count")
        private int tripCount;

        @JsonProperty("total_revenue")
        private double totalRevenue;

        @JsonProperty("total_cost")
        private double totalCost;

        @JsonProperty("total_profit")
        private double totalProfit;

        @JsonProperty("avg_margin_pct")
        private double avgMarginPct;

        @JsonProperty("quadrant")
        private String quadrant;

        @JsonProperty("quadrant_name")
        private String quadrantName;
    }
}
