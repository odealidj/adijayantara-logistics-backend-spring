package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Identifier (email atau nomor telepon) wajib diisi")
    @JsonProperty("identifier")
    private String identifier;

    @NotBlank(message = "Password wajib diisi")
    @JsonProperty("password")
    private String password;
}

