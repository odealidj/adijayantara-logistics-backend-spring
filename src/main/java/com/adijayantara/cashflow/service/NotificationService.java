package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Notification;
import com.adijayantara.cashflow.domain.enums.NotificationCategory;
import com.adijayantara.cashflow.domain.enums.NotificationSeverity;
import com.adijayantara.cashflow.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyLowCashBalance(BigDecimal currentSaldo, BigDecimal threshold) {
        if (currentSaldo == null || threshold == null) return;
        if (currentSaldo.compareTo(threshold) < 0) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            String msg = String.format("Peringatan: Saldo kas berjalan (%s) berada di bawah ambang batas aman (%s). Segera lakukan top-up kas operasional.",
                    nf.format(currentSaldo), nf.format(threshold));

            Notification notif = Notification.builder()
                    .targetRole("owner")
                    .title("⚠️ Saldo Kas Menipis")
                    .message(msg)
                    .category(NotificationCategory.CASHFLOW)
                    .severity(NotificationSeverity.WARNING)
                    .actionUrl("/dashboard")
                    .build();

            notificationRepository.save(notif);
        }
    }

    @Transactional
    public void notifyNegativeMargin(String actInfo, String actExplain, BigDecimal grandCost, BigDecimal grandSelling, BigDecimal profit, BigDecimal marginPct) {
        String msg = String.format("Transaksi pengiriman '%s' (%s) mencatat margin negatif/rendah! Cost: %s, Selling: %s, Profit: %s (%.2f%%)",
                actInfo, actExplain, grandCost, grandSelling, profit, marginPct.multiply(BigDecimal.valueOf(100)).doubleValue());

        Notification notif = Notification.builder()
                .targetRole("owner")
                .title("🚨 Alert Margin Negatif / Rendah")
                .message(msg)
                .category(NotificationCategory.CASHFLOW)
                .severity(NotificationSeverity.CRITICAL)
                .actionUrl("/dashboard")
                .build();

        notificationRepository.save(notif);
    }
}
