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
public class VendorEfficiencyResponse {

    @JsonProperty("total_vendors")
    private int totalVendors;

    @JsonProperty("total_cost_all")
    private double totalCostAll;

    @JsonProperty("total_trips_all")
    private int totalTripsAll;

    @JsonProperty("top_vendor_name")
    private String topVendorName;

    @JsonProperty("top_concentration_pct")
    private double topConcentrationPct;

    @JsonProperty("highest_margin_vendor")
    private String highestMarginVendor;

    @JsonProperty("highest_margin_pct")
    private double highestMarginPct;

    @JsonProperty("vendors")
    private List<VendorEfficiencyItem> vendors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorEfficiencyItem {
        @JsonProperty("vendor_name")
        private String vendorName;

        @JsonProperty("trip_count")
        private int tripCount;

        @JsonProperty("total_cost")
        private double totalCost;

        @JsonProperty("total_selling")
        private double totalSelling;

        @JsonProperty("total_profit")
        private double totalProfit;

        @JsonProperty("avg_margin_pct")
        private double avgMarginPct;

        @JsonProperty("concentration_ratio")
        private double concentrationRatio;

        @JsonProperty("is_dominant")
        private boolean isDominant;
    }
}

