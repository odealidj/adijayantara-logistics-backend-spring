package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashflowEntryResponseDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("sequence_no")
    private Integer sequenceNo;

    @JsonProperty("entry_type")
    private EntryType entryType;

    @JsonProperty("kredit")
    private BigDecimal kredit;

    @JsonProperty("debit")
    private BigDecimal debit;

    @JsonProperty("saldo")
    private BigDecimal saldo;

    @JsonProperty("date_of_entry")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime dateOfEntry;

    @JsonProperty("act_information")
    private String actInformation;

    @JsonProperty("act_explaination")
    private String actExplaination;

    @JsonProperty("vendor_id")
    private Integer vendorId;

    @JsonProperty("vendor_name_raw")
    private String vendorNameRaw;

    @JsonProperty("customer_id")
    private Integer customerId;

    @JsonProperty("customer_name_raw")
    private String customerNameRaw;

    @JsonProperty("top_days")
    private Integer topDays;

    @JsonProperty("due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime dueDate;

    @JsonProperty("grand_cost")
    private BigDecimal grandCost;

    @JsonProperty("grand_selling")
    private BigDecimal grandSelling;

    @JsonProperty("profit")
    private BigDecimal profit;

    @JsonProperty("margin_pct")
    private BigDecimal marginPct;

    @JsonProperty("remarks")
    private PaymentStatus remarks;

    @JsonProperty("invoice_id")
    private Integer invoiceId;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("updated_by")
    private UUID updatedBy;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;

    public static CashflowEntryResponseDto fromEntity(CashflowEntry entity) {
        if (entity == null) return null;

        LocalDateTime entryDateTime = entity.getDateOfEntry() != null 
                ? entity.getDateOfEntry().atStartOfDay() 
                : null;
        LocalDateTime dueDateTime = entity.getDueDate() != null 
                ? entity.getDueDate().atStartOfDay() 
                : null;

        return CashflowEntryResponseDto.builder()
                .id(entity.getId())
                .sequenceNo(entity.getSequenceNo())
                .entryType(entity.getEntryType())
                .kredit(entity.getKredit())
                .debit(entity.getDebit())
                .saldo(entity.getSaldo())
                .dateOfEntry(entryDateTime)
                .actInformation(entity.getActInformation())
                .actExplaination(entity.getActExplaination())
                .vendorId(entity.getVendorId())
                .vendorNameRaw(entity.getVendorNameRaw())
                .customerId(entity.getCustomerId())
                .customerNameRaw(entity.getCustomerNameRaw())
                .topDays(entity.getTopDays())
                .dueDate(dueDateTime)
                .grandCost(entity.getGrandCost())
                .grandSelling(entity.getGrandSelling())
                .profit(entity.getProfit())
                .marginPct(entity.getMarginPct())
                .remarks(entity.getRemarks())
                .invoiceId(entity.getInvoiceId())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
