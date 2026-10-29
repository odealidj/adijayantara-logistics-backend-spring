package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashflowEntryRepository extends JpaRepository<CashflowEntry, Integer>, JpaSpecificationExecutor<CashflowEntry> {

    Optional<CashflowEntry> findTopByOrderBySequenceNoDesc();

    @Query(value = "SELECT COALESCE((SELECT saldo FROM cashflow_entries ORDER BY sequence_no DESC LIMIT 1 FOR UPDATE), 0)", nativeQuery = true)
    BigDecimal getLastSaldoForUpdate();

    @Query(value = "SELECT COALESCE((SELECT saldo FROM cashflow_entries ORDER BY sequence_no DESC LIMIT 1), 0)", nativeQuery = true)
    BigDecimal getCurrentSaldo();

    @Modifying
    @Query("UPDATE CashflowEntry c SET c.saldo = c.saldo + :diff WHERE c.sequenceNo > :sequenceNo")
    int updateBalancesAfter(@Param("sequenceNo") Integer sequenceNo, @Param("diff") BigDecimal diff);

    @Modifying
    @Query("UPDATE CashflowEntry c SET c.remarks = :remarks, c.updatedBy = :updatedBy, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    int updateRemarks(@Param("id") Integer id, @Param("remarks") PaymentStatus remarks, @Param("updatedBy") UUID updatedBy);

    Optional<CashflowEntry> findByInvoiceId(Integer invoiceId);

    @Query(value = """
        SELECT 
            TO_CHAR(date_of_entry, 'YYYY-MM') AS month_key,
            EXTRACT(YEAR FROM date_of_entry)::INT AS year,
            EXTRACT(MONTH FROM date_of_entry)::INT AS month,
            COUNT(*)::BIGINT AS total_count
        FROM cashflow_entries
        GROUP BY 1, 2, 3
        ORDER BY month_key DESC
    """, nativeQuery = true)
    List<Object[]> findAvailablePeriodsRaw();
}


