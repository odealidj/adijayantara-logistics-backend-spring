package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.CashflowEntryHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashflowEntryHistoryRepository extends JpaRepository<CashflowEntryHistory, Integer> {
    List<CashflowEntryHistory> findByEntryIdOrderByArchivedAtDesc(Integer entryId);
    Page<CashflowEntryHistory> findByEntryId(Integer entryId, Pageable pageable);
}
