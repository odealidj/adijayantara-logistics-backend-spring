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
public class CustomerDisciplineAndParetoResponse {

    @JsonProperty("total_clients")
    private int totalClients;

    @JsonProperty("total_revenue")
    private double totalRevenue;

    @JsonProperty("top_80_percent_count")
    private int top80PercentCount;

    @JsonProperty("customers")
    private List<CustomerDisciplineItem> customers;

    @JsonProperty("overall_avg_dso")
    private double overallAvgDso;

    @JsonProperty("high_risk_client_count")
    private int highRiskClientCount;

    @JsonProperty("churn_alert_count")
    private int churnAlertCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDisciplineItem {
        @JsonProperty("client_name")
        private String clientName;

        @JsonProperty("total_invoices")
        private int totalInvoices;

        @JsonProperty("total_billed")
        private double totalBilled;

        @JsonProperty("avg_top_days")
        private int avgTopDays;

        @JsonProperty("avg_actual_dso")
        private double avgActualDso;

        @JsonProperty("overdue_count")
        private int overdueCount;

        @JsonProperty("reschedule_count")
        private int rescheduleCount;

        @JsonProperty("discipline_status")
        private String disciplineStatus;

        @JsonProperty("pareto_percent")
        private double paretoPercent;

        @JsonProperty("cumulative_percent")
        private double cumulativePercent;

        @JsonProperty("recent_trips_count")
        private int recentTripsCount;

        @JsonProperty("previous_trips_count")
        private int previousTripsCount;

        @JsonProperty("trip_change_pct")
        private double tripChangePct;

        @JsonProperty("is_churn_warning")
        private boolean isChurnWarning;
    }
}
