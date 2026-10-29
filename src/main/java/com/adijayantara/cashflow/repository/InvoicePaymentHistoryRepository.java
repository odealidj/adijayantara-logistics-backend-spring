package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.InvoicePaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoicePaymentHistoryRepository extends JpaRepository<InvoicePaymentHistory, Integer> {
    List<InvoicePaymentHistory> findByInvoiceIdOrderByPaymentDateDesc(Integer invoiceId);
}
