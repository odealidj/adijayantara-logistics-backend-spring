package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.Invoice;
import com.adijayantara.cashflow.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceNoAndDeletedAtIsNull(String invoiceNo);

    boolean existsByInvoiceNoAndDeletedAtIsNull(String invoiceNo);

    Page<Invoice> findByStatusAndDeletedAtIsNull(InvoiceStatus status, Pageable pageable);

    Optional<Invoice> findByCashflowEntryId(Integer cashflowEntryId);

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT 
            TO_CHAR(shipment_date, 'YYYY-MM') AS month_key,
            EXTRACT(YEAR FROM shipment_date)::INT AS year,
            EXTRACT(MONTH FROM shipment_date)::INT AS month,
            COUNT(*)::BIGINT AS total_count
        FROM invoices
        WHERE deleted_at IS NULL
        GROUP BY 1, 2, 3
        ORDER BY month_key DESC
    """, nativeQuery = true)
    java.util.List<Object[]> findAvailablePeriodsRaw();
}

