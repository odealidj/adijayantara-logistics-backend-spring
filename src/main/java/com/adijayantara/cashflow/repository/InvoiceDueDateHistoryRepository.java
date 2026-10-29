package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.InvoiceDueDateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDueDateHistoryRepository extends JpaRepository<InvoiceDueDateHistory, Integer> {
    List<InvoiceDueDateHistory> findByInvoiceIdOrderByCreatedAtDesc(Integer invoiceId);
    int countByInvoiceId(Integer invoiceId);
}

