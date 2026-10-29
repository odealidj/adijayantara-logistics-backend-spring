package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
public class UpdateCashflowRequest {

    @JsonProperty("entry_type")
    private EntryType entryType;

    @JsonProperty("date_of_entry")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dateOfEntry;

    @JsonProperty("act_information")
    private String actInformation;

    @JsonProperty("act_explaination")
    private String actExplaination;

    @JsonProperty("remarks")
    private PaymentStatus remarks;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_name_raw")
    private String vendorNameRaw;

    @JsonProperty("customer_id")
    private Integer customerId;

    @JsonProperty("customer_name_raw")
    private String customerNameRaw;

    @JsonProperty("grand_cost")
    private BigDecimal grandCost;

    @JsonProperty("grand_selling")
    private BigDecimal grandSelling;

    @JsonProperty("debit")
    private BigDecimal debit;

    @JsonProperty("kredit")
    private BigDecimal kredit;

    @JsonProperty("top_days")
    private Integer topDays;

    @JsonProperty("due_date")
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    private LocalDate dueDate;
}
