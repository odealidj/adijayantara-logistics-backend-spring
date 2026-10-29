package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
public class CreateShipmentRequest {

    @NotNull(message = "Tanggal pengiriman wajib diisi")
    @JsonProperty("date_of_entry")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dateOfEntry;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_name_raw")
    private String vendorNameRaw;

    @JsonProperty("customer_id")
    private Integer customerId;

    @JsonProperty("customer_name_raw")
    private String customerNameRaw;

    @JsonProperty("act_information")
    private String actInformation;

    @JsonProperty("act_explaination")
    private String actExplaination;

    @JsonProperty("grand_cost")
    @Builder.Default
    private BigDecimal grandCost = BigDecimal.ZERO;

    @JsonProperty("grand_selling")
    @Builder.Default
    private BigDecimal grandSelling = BigDecimal.ZERO;

    @JsonProperty("debit")
    private BigDecimal debit;

    @JsonProperty("top_days")
    @Builder.Default
    private Integer topDays = 0;

    @JsonProperty("due_date")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dueDate;

    @JsonProperty("remarks")
    private PaymentStatus remarks;
}
