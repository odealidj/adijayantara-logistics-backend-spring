package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.enums.InvoiceStatus;
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
public class InvoiceFilterRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer limit = 50;

    @Builder.Default
    private String sort_by = "created_at";

    @Builder.Default
    private String sort_dir = "DESC";

    private InvoiceStatus status;

    private String client_name;

    private String invoice_no;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date_from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date_to;

    public int getPage() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getLimit() {
        return (limit == null || limit < 1) ? 50 : limit;
    }

    public String getSortDir() {
        return (sort_dir != null && sort_dir.equalsIgnoreCase("ASC")) ? "ASC" : "DESC";
    }

    public String getSortBy() {
        return (sort_by != null && !sort_by.trim().isEmpty()) ? sort_by.trim().toLowerCase() : "created_at";
    }
}
