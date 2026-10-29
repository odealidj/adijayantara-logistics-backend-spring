package com.adijayantara.cashflow.domain.entity;

import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
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
@Table(name = "cashflow_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashflowEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private EntryType entryType = EntryType.SHIPMENT;

    @Column(name = "kredit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal kredit = BigDecimal.ZERO;

    @Column(name = "debit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldo = BigDecimal.ZERO;

    @Column(name = "date_of_entry", nullable = false)
    private LocalDate dateOfEntry;

    @Column(name = "act_information")
    private String actInformation;

    @Column(name = "act_explaination")
    private String actExplaination;

    @Column(name = "vendor_id")
    private Integer vendorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", insertable = false, updatable = false)
    private Vendor vendor;

    @Column(name = "vendor_name_raw")
    private String vendorNameRaw;

    @Column(name = "customer_id")
    private Integer customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Column(name = "customer_name_raw", nullable = false)
    @Builder.Default
    private String customerNameRaw = "";

    @Column(name = "top_days")
    @Builder.Default
    private Integer topDays = 0;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "grand_cost", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal grandCost = BigDecimal.ZERO;

    @Column(name = "grand_selling", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal grandSelling = BigDecimal.ZERO;

    @Column(name = "profit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal profit = BigDecimal.ZERO;

    @Column(name = "margin_pct", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal marginPct = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "remarks")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private PaymentStatus remarks = PaymentStatus.UNPAID;

    @Column(name = "invoice_id")
    private Integer invoiceId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
