package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleInvoiceRequest {

    @JsonProperty("payment_date")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate paymentDate;

    @JsonProperty("reference_no")
    private String referenceNo;

    @JsonProperty("proof_url")
    private String proofUrl;

    @JsonProperty("notes")
    private String notes;
}

