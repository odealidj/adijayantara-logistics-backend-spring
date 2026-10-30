package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceHistorySummaryDto {

    @JsonProperty("invoice")
    private InvoiceResponseDto invoice;

    @JsonProperty("reschedule_logs")
    @Builder.Default
    private List<InvoiceDueDateHistoryDto> rescheduleLogs = new ArrayList<>();

    @JsonProperty("payment_logs")
    @Builder.Default
    private List<InvoicePaymentHistoryDto> paymentLogs = new ArrayList<>();
}

