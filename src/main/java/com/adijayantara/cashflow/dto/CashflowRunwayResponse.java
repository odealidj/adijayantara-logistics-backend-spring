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
public class CashflowRunwayResponse {

    @JsonProperty("current_cash_balance")
    private double currentCashBalance;

    @JsonProperty("minimum_cash_buffer")
    private double minimumCashBuffer;

    @JsonProperty("total_pending_inflow")
    private double totalPendingInflow;

    @JsonProperty("total_pending_outflow")
    private double totalPendingOutflow;

    @JsonProperty("net_projected_60_days")
    private double netProjected60Days;

    @JsonProperty("lowest_point_date")
    private String lowestPointDate;

    @JsonProperty("lowest_point_balance")
    private double lowestPointBalance;

    @JsonProperty("runway_days")
    private int runwayDays;

    @JsonProperty("buckets")
    private List<RunwayBucket> buckets;

    @JsonProperty("avg_customer_dso")
    private double avgCustomerDso;

    @JsonProperty("avg_vendor_dpo")
    private double avgVendorDpo;

    @JsonProperty("financing_gap_days")
    private double financingGapDays;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunwayBucket {
        @JsonProperty("date")
        private String date;

        @JsonProperty("week_label")
        private String weekLabel;

        @JsonProperty("day_offset")
        private int dayOffset;

        @JsonProperty("inflow_amount")
        private double inflowAmount;

        @JsonProperty("outflow_amount")
        private double outflowAmount;

        @JsonProperty("net_change")
        private double netChange;

        @JsonProperty("projected_cash")
        private double projectedCash;

        @JsonProperty("is_in_danger")
        private boolean isInDanger;
    }
}

