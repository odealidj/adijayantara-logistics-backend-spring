package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSummaryDto {

    @JsonProperty("total_amount")
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @JsonProperty("total_count")
    @Builder.Default
    private Long totalCount = 0L;

    @JsonProperty("paid_amount")
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @JsonProperty("paid_count")
    @Builder.Default
    private Long paidCount = 0L;

    @JsonProperty("unpaid_amount")
    @Builder.Default
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    @JsonProperty("unpaid_count")
    @Builder.Default
    private Long unpaidCount = 0L;

    @JsonProperty("overdue_amount")
    @Builder.Default
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    @JsonProperty("overdue_count")
    @Builder.Default
    private Long overdueCount = 0L;
}
