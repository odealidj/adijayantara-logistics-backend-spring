package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashflowFilterRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer limit = 50;

    @Builder.Default
    private String sort = "DESC";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date_from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date_to;

    private EntryType entry_type;

    private PaymentStatus remarks;

    private String vendor_name;

    public int getPage() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getLimit() {
        return (limit == null || limit < 1) ? 50 : limit;
    }

    public String getSort() {
        return (sort != null && sort.equalsIgnoreCase("ASC")) ? "ASC" : "DESC";
    }
}

