package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSummaryKpiDto {

    @JsonProperty("total_logs")
    private long totalLogs;

    @JsonProperty("total_critical")
    private long totalCritical;

    @JsonProperty("total_warning")
    private long totalWarning;

    @JsonProperty("total_late_inputs")
    private long totalLateInputs;

    @JsonProperty("avg_cashflow_lag_days")
    private double avgCashflowLagDays;

    @JsonProperty("avg_invoice_lag_days")
    private double avgInvoiceLagDays;

    @JsonProperty("total_anomalies_count")
    private long totalAnomaliesCount;

    @JsonProperty("total_staff_monitored")
    private long totalStaffMonitored;
}

