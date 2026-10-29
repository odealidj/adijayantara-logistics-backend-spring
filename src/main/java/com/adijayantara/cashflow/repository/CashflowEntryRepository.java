package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface CashflowEntryRepository extends JpaRepository<CashflowEntry, Integer>, JpaSpecificationExecutor<CashflowEntry> {

    Optional<CashflowEntry> findTopByOrderBySequenceNoDesc();

    @Modifying
    @Query("UPDATE CashflowEntry c SET c.saldo = c.saldo + :diff WHERE c.sequenceNo > :sequenceNo")
    int updateBalancesAfter(@Param("sequenceNo") Integer sequenceNo, @Param("diff") BigDecimal diff);

    Optional<CashflowEntry> findByInvoiceId(Integer invoiceId);
}
