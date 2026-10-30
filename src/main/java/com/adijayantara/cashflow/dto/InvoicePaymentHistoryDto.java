package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.entity.InvoicePaymentHistory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentHistoryDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("invoice_id")
    private Integer invoiceId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("payment_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime paymentDate;

    @JsonProperty("reference_no")
    private String referenceNo;

    @JsonProperty("proof_url")
    private String proofUrl;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("created_by_name")
    private String createdByName;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    public static InvoicePaymentHistoryDto fromEntity(InvoicePaymentHistory entity) {
        if (entity == null) return null;
        return InvoicePaymentHistoryDto.builder()
                .id(entity.getId())
                .invoiceId(entity.getInvoiceId())
                .action(entity.getAction())
                .amount(entity.getAmount())
                .paymentDate(entity.getPaymentDate())
                .referenceNo(entity.getReferenceNo() != null ? entity.getReferenceNo() : "")
                .proofUrl(entity.getProofUrl() != null ? entity.getProofUrl() : "")
                .notes(entity.getNotes() != null ? entity.getNotes() : "")
                .createdBy(entity.getCreatedBy())
                .createdByName(entity.getCreatedByName() != null ? entity.getCreatedByName() : "")
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

