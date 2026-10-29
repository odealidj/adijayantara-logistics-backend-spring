package com.adijayantara.cashflow.domain.entity;

import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cashflow_entries_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashflowEntryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @Column(name = "entry_id", nullable = false)
    private Integer entryId;

    @Column(name = "sequence_no")
    private Integer sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private EntryType entryType;

    @Column(name = "kredit", precision = 15, scale = 2)
    private BigDecimal kredit;

    @Column(name = "debit", precision = 15, scale = 2)
    private BigDecimal debit;

    @Column(name = "saldo", precision = 15, scale = 2)
    private BigDecimal saldo;

    @Column(name = "date_of_entry")
    private LocalDate dateOfEntry;

    @Column(name = "act_information")
    private String actInformation;

    @Column(name = "act_explaination")
    private String actExplaination;

    @Column(name = "vendor_id")
    private Integer vendorId;

    @Column(name = "vendor_name_raw")
    private String vendorNameRaw;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "customer_name_raw", nullable = false)
    @Builder.Default
    private String customerNameRaw = "";

    @Column(name = "top_days")
    private Integer topDays;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "grand_cost", precision = 15, scale = 2)
    private BigDecimal grandCost;

    @Column(name = "grand_selling", precision = 15, scale = 2)
    private BigDecimal grandSelling;

    @Column(name = "profit", precision = 15, scale = 2)
    private BigDecimal profit;

    @Column(name = "margin_pct", precision = 5, scale = 4)
    private BigDecimal marginPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "remarks")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private PaymentStatus remarks;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "original_created_at")
    private LocalDateTime originalCreatedAt;

    @Column(name = "original_updated_at")
    private LocalDateTime originalUpdatedAt;

    @CreationTimestamp
    @Column(name = "archived_at", updatable = false)
    private LocalDateTime archivedAt;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "archive_reason")
    @Builder.Default
    private String archiveReason = "import_upsert";
}
