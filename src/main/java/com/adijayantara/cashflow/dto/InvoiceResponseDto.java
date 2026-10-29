package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.entity.Invoice;
import com.adijayantara.cashflow.domain.enums.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceResponseDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("invoice_no")
    private String invoiceNo;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("shipment_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime shipmentDate;

    @JsonProperty("top_terms")
    private String topTerms;

    @JsonProperty("top_days")
    private Integer topDays;

    @JsonProperty("due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime dueDate;

    @JsonProperty("original_due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime originalDueDate;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("status")
    private InvoiceStatus status;

    @JsonProperty("paid_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime paidAt;

    @JsonProperty("paid_by")
    private UUID paidBy;

    @JsonProperty("paid_by_name")
    @Builder.Default
    private String paidByName = "";

    @JsonProperty("payment_reference")
    @Builder.Default
    private String paymentReference = "";

    @JsonProperty("payment_proof_url")
    @Builder.Default
    private String paymentProofUrl = "";

    @JsonProperty("payment_notes")
    @Builder.Default
    private String paymentNotes = "";

    @JsonProperty("notes")
    @Builder.Default
    private String notes = "";

    @JsonProperty("cashflow_entry_id")
    private Integer cashflowEntryId;

    @JsonProperty("created_by")
    private UUID createdBy;

    @JsonProperty("created_by_name")
    @Builder.Default
    private String createdByName = "";

    @JsonProperty("reschedule_count")
    @Builder.Default
    private Integer rescheduleCount = 0;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;

    public static InvoiceResponseDto fromEntity(Invoice entity, int rescheduleCount, String createdByName, String paidByName) {
        if (entity == null) return null;

        return InvoiceResponseDto.builder()
                .id(entity.getId())
                .invoiceNo(entity.getInvoiceNo())
                .clientName(entity.getClientName())
                .shipmentDate(entity.getShipmentDate() != null ? entity.getShipmentDate().atStartOfDay() : null)
                .topTerms(entity.getTopTerms())
                .topDays(entity.getTopDays())
                .dueDate(entity.getDueDate() != null ? entity.getDueDate().atStartOfDay() : null)
                .originalDueDate(entity.getOriginalDueDate() != null 
                        ? entity.getOriginalDueDate().atStartOfDay() 
                        : (entity.getDueDate() != null ? entity.getDueDate().atStartOfDay() : null))
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .paidAt(entity.getPaidAt())
                .paidBy(entity.getPaidBy())
                .paidByName(paidByName != null ? paidByName : "")
                .paymentReference(entity.getPaymentReference() != null ? entity.getPaymentReference() : "")
                .paymentProofUrl(entity.getPaymentProofUrl() != null ? entity.getPaymentProofUrl() : "")
                .paymentNotes(entity.getPaymentNotes() != null ? entity.getPaymentNotes() : "")
                .notes(entity.getNotes() != null ? entity.getNotes() : "")
                .cashflowEntryId(entity.getCashflowEntryId())
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName != null ? createdByName : "")
                .rescheduleCount(rescheduleCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
