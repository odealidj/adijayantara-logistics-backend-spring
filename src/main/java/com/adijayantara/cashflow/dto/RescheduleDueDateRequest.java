package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleDueDateRequest {

    @NotNull(message = "Tanggal jatuh tempo baru wajib diisi")
    @JsonProperty("new_due_date")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate newDueDate;

    @NotBlank(message = "Alasan perpanjangan jatuh tempo wajib diisi demi akuntabilitas audit")
    @JsonProperty("reason")
    private String reason;
}

