package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.DecimalMin;
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
public class CreateTopUpRequest {

    @NotNull(message = "Tanggal transaksi kas masuk wajib diisi")
    @JsonProperty("date_of_entry")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dateOfEntry;

    @NotNull(message = "Nominal kas masuk (kredit) wajib diisi")
    @DecimalMin(value = "0.01", message = "Nominal kredit harus lebih besar dari 0")
    @JsonProperty("kredit")
    private BigDecimal kredit;

    @JsonProperty("act_information")
    private String actInformation;

    @JsonProperty("act_explaination")
    private String actExplaination;
}
