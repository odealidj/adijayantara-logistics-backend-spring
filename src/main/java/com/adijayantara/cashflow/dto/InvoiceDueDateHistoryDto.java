package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.entity.InvoiceDueDateHistory;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDueDateHistoryDto {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("invoice_id")
    private Integer invoiceId;

    @JsonProperty("previous_due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime previousDueDate;

    @JsonProperty("new_due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime newDueDate;

    @JsonProperty("days_added")
    private Integer daysAdded;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("changed_by")
    private UUID changedBy;

    @JsonProperty("changed_by_name")
    private String changedByName;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    public static InvoiceDueDateHistoryDto fromEntity(InvoiceDueDateHistory entity) {
        if (entity == null) return null;
        return InvoiceDueDateHistoryDto.builder()
                .id(entity.getId())
                .invoiceId(entity.getInvoiceId())
                .previousDueDate(entity.getPreviousDueDate() != null ? entity.getPreviousDueDate().atStartOfDay() : null)
                .newDueDate(entity.getNewDueDate() != null ? entity.getNewDueDate().atStartOfDay() : null)
                .daysAdded(entity.getDaysAdded())
                .reason(entity.getReason())
                .changedBy(entity.getChangedBy())
                .changedByName(entity.getChangedByName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

