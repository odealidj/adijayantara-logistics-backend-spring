package com.adijayantara.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditFilterRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer limit = 50;

    private UUID actor_id;
    private String actor_name;
    private String action;
    private String entity_type;
    private String severity;
    private Integer min_lag_days;
    private String search;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate start_date;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate end_date;

    public int getPage() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int getLimit() {
        return (limit == null || limit < 1) ? 50 : limit;
    }
}

