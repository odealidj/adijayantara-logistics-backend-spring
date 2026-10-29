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
public class CashflowSummaryDto {

    @JsonProperty("current_saldo")
    @Builder.Default
    private BigDecimal currentSaldo = BigDecimal.ZERO;

    @JsonProperty("total_kredit")
    @Builder.Default
    private BigDecimal totalKredit = BigDecimal.ZERO;

    @JsonProperty("total_debit")
    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @JsonProperty("total_profit")
    @Builder.Default
    private BigDecimal totalProfit = BigDecimal.ZERO;

    @JsonProperty("avg_margin_pct")
    @Builder.Default
    private BigDecimal avgMarginPct = BigDecimal.ZERO;

    @JsonProperty("unpaid_count")
    @Builder.Default
    private Long unpaidCount = 0L;

    @JsonProperty("unpaid_amount")
    @Builder.Default
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
}
