package com.adijayantara.cashflow.domain.entity;

import com.adijayantara.cashflow.domain.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "invoice_no", nullable = false, unique = true)
    private String invoiceNo;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "shipment_date", nullable = false)
    private LocalDate shipmentDate;

    @Column(name = "top_terms", nullable = false)
    private String topTerms;

    @Column(name = "top_days", nullable = false)
    private Integer topDays;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "original_due_date")
    private LocalDate originalDueDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "cashflow_entry_id")
    private Integer cashflowEntryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashflow_entry_id", insertable = false, updatable = false)
    private CashflowEntry cashflowEntry;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "paid_by")
    private UUID paidBy;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "payment_proof_url", columnDefinition = "text")
    private String paymentProofUrl;

    @Column(name = "payment_notes", columnDefinition = "text")
    private String paymentNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

