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
public class PaginationMeta {

    @JsonProperty("page")
    private int page;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("total_items")
    private int totalItems;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("has_next")
    private boolean hasNext;

    @JsonProperty("has_prev")
    private boolean hasPrev;

    public static PaginationMeta of(int page, int limit, int totalItems) {
        int totalPages = limit > 0 ? (totalItems + limit - 1) / limit : 0;
        return PaginationMeta.builder()
                .page(page)
                .limit(limit)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .hasNext(page < totalPages)
                .hasPrev(page > 1)
                .build();
    }
}

