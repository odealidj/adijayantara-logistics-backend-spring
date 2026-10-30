package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {

    @NotBlank(message = "Nomor invoice wajib diisi")
    @JsonProperty("invoice_no")
    private String invoiceNo;

    @NotBlank(message = "Nama klien / perusahaan wajib diisi")
    @JsonProperty("client_name")
    private String clientName;

    @NotNull(message = "Tanggal pengiriman / penerbitan invoice wajib diisi")
    @JsonProperty("shipment_date")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate shipmentDate;

    @JsonProperty("top_terms")
    private String topTerms;

    @JsonProperty("top_days")
    @Builder.Default
    private Integer topDays = 0;

    @NotNull(message = "Nominal tagihan wajib diisi")
    @DecimalMin(value = "0.01", message = "Nominal tagihan harus lebih dari 0")
    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("cashflow_entry_id")
    private Integer cashflowEntryId;
}

