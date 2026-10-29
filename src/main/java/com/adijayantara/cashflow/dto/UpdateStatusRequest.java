package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Status pembayaran (remarks) wajib diisi")
    @JsonProperty("remarks")
    private PaymentStatus remarks;
}

