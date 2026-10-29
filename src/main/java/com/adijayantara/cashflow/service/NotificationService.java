package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Notification;
import com.adijayantara.cashflow.domain.enums.NotificationCategory;
import com.adijayantara.cashflow.domain.enums.NotificationSeverity;
import com.adijayantara.cashflow.dto.CreateNotificationInput;
import com.adijayantara.cashflow.dto.NotificationResponseDto;
import com.adijayantara.cashflow.repository.InvoiceRepository;
import com.adijayantara.cashflow.repository.NotificationRepository;
import com.adijayantara.cashflow.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final InvoiceRepository invoiceRepository;
    private final JdbcClient jdbcClient;

    private static final DecimalFormat RUPIAH_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        RUPIAH_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private String formatRupiah(BigDecimal val) {
        if (val == null) return "0";
        return RUPIAH_FORMAT.format(val);
    }

    @Transactional
    public NotificationResponseDto create(CreateNotificationInput input) {
        Notification notification = Notification.builder()
                .userId(input.getUserId())
                .targetRole(input.getTargetRole())
                .title(input.getTitle())
                .message(input.getMessage())
                .category(input.getCategory())
                .severity(input.getSeverity())
                .actionUrl(input.getActionUrl())
                .metadata(input.getMetadata())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        return NotificationResponseDto.fromEntity(saved);
    }

    public Map<String, Object> list(UUID userId, String userRole, int page, int limit,
                                    String category, String severity, boolean unreadOnly, String search) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 20;
        int offset = (page - 1) * limit;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (!"super_admin".equalsIgnoreCase(userRole)) {
            if (userId != null) {
                where.append(" AND (user_id = :userId OR (user_id IS NULL AND (target_role = :userRole OR target_role IS NULL))) ");
                params.put("userId", userId);
                params.put("userRole", userRole);
            } else {
                where.append(" AND (user_id IS NULL AND (target_role = :userRole OR target_role IS NULL)) ");
                params.put("userRole", userRole);
            }
        }

        if (category != null && !category.isBlank()) {
            where.append(" AND category::text = :category ");
            params.put("category", category.trim().toUpperCase());
        }

        if (severity != null && !severity.isBlank()) {
            where.append(" AND severity::text = :severity ");
            params.put("severity", severity.trim().toUpperCase());
        }

        if (unreadOnly) {
            where.append(" AND is_read = false ");
        }

        if (search != null && !search.isBlank()) {
            where.append(" AND (title ILIKE :searchPattern OR message ILIKE :searchPattern) ");
            params.put("searchPattern", "%" + search.trim() + "%");
        }

        String countSql = "SELECT COUNT(*) FROM notifications " + where;
        var countQuery = jdbcClient.sql(countSql);
        params.forEach(countQuery::param);
        long total = countQuery.query(Long.class).single();

        String dataSql = """
            SELECT id, user_id, target_role, title, message, category::text as category,
                   severity::text as severity, action_url, metadata, is_read, read_at, created_at
            FROM notifications
            """ + where + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset ";

        params.put("limit", limit);
        params.put("offset", offset);

        var dataQuery = jdbcClient.sql(dataSql);
        params.forEach(dataQuery::param);

        List<NotificationResponseDto> items = dataQuery.query((rs, rowNum) -> {
            Timestamp readAtTs = rs.getTimestamp("read_at");
            Timestamp createdAtTs = rs.getTimestamp("created_at");
            return NotificationResponseDto.builder()
                    .id((UUID) rs.getObject("id"))
                    .userId((UUID) rs.getObject("user_id"))
                    .targetRole(rs.getString("target_role"))
                    .title(rs.getString("title"))
                    .message(rs.getString("message"))
                    .category(NotificationCategory.valueOf(rs.getString("category")))
                    .severity(NotificationSeverity.valueOf(rs.getString("severity")))
                    .actionUrl(rs.getString("action_url"))
                    .metadata(rs.getString("metadata"))
                    .isRead(rs.getBoolean("is_read"))
                    .readAt(readAtTs != null ? readAtTs.toLocalDateTime() : null)
                    .createdAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null)
                    .build();
        }).list();

        return Map.of(
            "data", items,
            "total", total,
            "page", page,
            "limit", limit
        );
    }

    public int getUnreadCount(UUID userId, String userRole) {
        StringBuilder where = new StringBuilder(" WHERE is_read = false ");
        Map<String, Object> params = new HashMap<>();

        if (!"super_admin".equalsIgnoreCase(userRole)) {
            if (userId != null) {
                where.append(" AND (user_id = :userId OR (user_id IS NULL AND (target_role = :userRole OR target_role IS NULL))) ");
                params.put("userId", userId);
                params.put("userRole", userRole);
            } else {
                where.append(" AND (user_id IS NULL AND (target_role = :userRole OR target_role IS NULL)) ");
                params.put("userRole", userRole);
            }
        }

        String sql = "SELECT COUNT(*) FROM notifications " + where;
        var query = jdbcClient.sql(sql);
        params.forEach(query::param);
        return query.query(Integer.class).single();
    }

    @Transactional
    public void markAsRead(UUID id, UUID userId) {
        int rows = jdbcClient.sql("UPDATE notifications SET is_read = true, read_at = NOW() WHERE id = :id")
                .param("id", id)
                .update();
        if (rows == 0) {
            throw new IllegalArgumentException("Notifikasi tidak ditemukan");
        }
    }

    @Transactional
    public void markAllAsRead(UUID userId, String userRole) {
        StringBuilder where = new StringBuilder(" WHERE is_read = false ");
        Map<String, Object> params = new HashMap<>();

        if (!"super_admin".equalsIgnoreCase(userRole)) {
            if (userId != null) {
                where.append(" AND (user_id = :userId OR (user_id IS NULL AND (target_role = :userRole OR target_role IS NULL))) ");
                params.put("userId", userId);
                params.put("userRole", userRole);
            } else {
                where.append(" AND (user_id IS NULL AND (target_role = :userRole OR target_role IS NULL)) ");
                params.put("userRole", userRole);
            }
        }

        String sql = "UPDATE notifications SET is_read = true, read_at = NOW() " + where;
        var query = jdbcClient.sql(sql);
        params.forEach(query::param);
        query.update();
    }

    @Transactional
    public void delete(UUID id) {
        int rows = jdbcClient.sql("DELETE FROM notifications WHERE id = :id")
                .param("id", id)
                .update();
        if (rows == 0) {
            throw new IllegalArgumentException("Notifikasi tidak ditemukan");
        }
    }

    @Transactional
    public int checkInvoiceDueDates() {
        String sql = """
            SELECT id, invoice_no, client_name, amount, due_date, status
            FROM invoices
            WHERE deleted_at IS NULL AND status::text IN ('UNPAID', 'OVERDUE')
        """;

        List<Map<String, Object>> invoices = jdbcClient.sql(sql).query().listOfRows();
        LocalDate today = LocalDate.now();
        int countDispatched = 0;

        for (Map<String, Object> inv : invoices) {
            Object invIdObj = inv.get("id");
            String invIdStr = String.valueOf(invIdObj);
            String invoiceNo = (String) inv.get("invoice_no");
            String clientName = (String) inv.get("client_name");
            Object amountObj = inv.get("amount");
            BigDecimal amount = amountObj instanceof BigDecimal bd ? bd : new BigDecimal(amountObj != null ? amountObj.toString() : "0");
            Object dueDateObj = inv.get("due_date");
            LocalDate dueDate = dueDateObj instanceof java.sql.Date sd ? sd.toLocalDate() : (LocalDate) dueDateObj;

            long daysDiff = ChronoUnit.DAYS.between(today, dueDate);
            String actionUrl = "/dashboard/invoices?search=" + invoiceNo;

            // 1. H-3 Jatuh Tempo (Warning)
            if (daysDiff == 3) {
                create(CreateNotificationInput.builder()
                        .targetRole("finance")
                        .title("⏳ Invoice Jatuh Tempo dalam 3 Hari: " + invoiceNo)
                        .message(String.format("Tagihan Invoice %s kepada '%s' senilai Rp %s akan jatuh tempo pada %s. Siapkan berkas tanda terima & penagihan.",
                                invoiceNo, clientName, formatRupiah(amount), DateUtil.formatDateIndo(dueDate)))
                        .category(NotificationCategory.INVOICE)
                        .severity(NotificationSeverity.WARNING)
                        .actionUrl(actionUrl)
                        .metadata(String.format("{\"invoice_id\":\"%s\",\"invoice_no\":\"%s\",\"amount\":%s,\"days_diff\":3}",
                                invIdStr, invoiceNo, amount))
                        .build());
                countDispatched++;
            }

            // 2. Hari-H Jatuh Tempo (Warning)
            if (daysDiff == 0) {
                create(CreateNotificationInput.builder()
                        .targetRole("finance")
                        .title("📅 Hari Ini Jatuh Tempo Invoice: " + invoiceNo)
                        .message(String.format("Hari ini adalah batas akhir pembayaran tagihan Invoice %s oleh '%s' senilai Rp %s. Segera hubungi bagian keuangan klien.",
                                invoiceNo, clientName, formatRupiah(amount)))
                        .category(NotificationCategory.INVOICE)
                        .severity(NotificationSeverity.WARNING)
                        .actionUrl(actionUrl)
                        .metadata(String.format("{\"invoice_id\":\"%s\",\"invoice_no\":\"%s\",\"amount\":%s,\"days_diff\":0}",
                                invIdStr, invoiceNo, amount))
                        .build());
                countDispatched++;
            }

            // 3. Overdue / Melewati Jatuh Tempo (Critical)
            if (daysDiff < 0) {
                long overdueDays = Math.abs(daysDiff);
                create(CreateNotificationInput.builder()
                        .targetRole("finance")
                        .title(String.format("🚨 Invoice Menunggak %d Hari: %s", overdueDays, invoiceNo))
                        .message(String.format("Invoice %s atas nama '%s' senilai Rp %s telah melewati tanggal jatuh tempo (%s). Lakukan tindakan penagihan intensif.",
                                invoiceNo, clientName, formatRupiah(amount), DateUtil.formatDateIndo(dueDate)))
                        .category(NotificationCategory.INVOICE)
                        .severity(NotificationSeverity.CRITICAL)
                        .actionUrl(actionUrl)
                        .metadata(String.format("{\"invoice_id\":\"%s\",\"invoice_no\":\"%s\",\"amount\":%s,\"days_diff\":%d}",
                                invIdStr, invoiceNo, amount, daysDiff))
                        .build());

                create(CreateNotificationInput.builder()
                        .targetRole("direktur")
                        .title(String.format("🚨 Piutang Macet: Invoice %s Menunggak %d Hari", invoiceNo, overdueDays))
                        .message(String.format("Tagihan Invoice %s ('%s') senilai Rp %s tertunggak %d hari dari jadwal jatuh tempo. Perlu tinjauan manajemen.",
                                invoiceNo, clientName, formatRupiah(amount), overdueDays))
                        .category(NotificationCategory.INVOICE)
                        .severity(NotificationSeverity.CRITICAL)
                        .actionUrl(actionUrl)
                        .metadata(String.format("{\"invoice_id\":\"%s\",\"invoice_no\":\"%s\",\"amount\":%s,\"days_diff\":%d}",
                                invIdStr, invoiceNo, amount, daysDiff))
                        .build());

                countDispatched += 2;
            }
        }

        return countDispatched;
    }

    public void notifyLowCashBalance(BigDecimal currentSaldo, BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0) {
            threshold = new BigDecimal("15000000");
        }
        if (currentSaldo != null && currentSaldo.compareTo(threshold) < 0) {
            List<String> roles = List.of("finance", "direktur", "owner");
            String title = "🚨 PERINGATAN KAS KRITIS: Saldo Menipis";
            String msg = String.format("Sisa saldo kas operasional berjalan saat ini tersisa Rp %s (di bawah batas aman Rp %s). Segera lakukan pengajuan Top-Up kas untuk menjamin kelancaran uang jalan armada supir.",
                    formatRupiah(currentSaldo), formatRupiah(threshold));

            for (String r : roles) {
                try {
                    create(CreateNotificationInput.builder()
                            .targetRole(r)
                            .title(title)
                            .message(msg)
                            .category(NotificationCategory.CASHFLOW)
                            .severity(NotificationSeverity.CRITICAL)
                            .actionUrl("/dashboard/cashflow")
                            .metadata(String.format("{\"saldo\":%s,\"threshold\":%s}", currentSaldo, threshold))
                            .build());
                } catch (Exception e) {
                    log.error("Failed to dispatch low cash notification: {}", e.getMessage());
                }
            }
        }
    }

    public void notifyNegativeMargin(String actInfo, String actExplain, BigDecimal cost, BigDecimal selling, BigDecimal profit, BigDecimal marginPct) {
        notifyMarginMinus(actInfo != null ? actInfo : actExplain, cost, selling, profit, marginPct);
    }

    public void notifyMarginMinus(String actInfo, BigDecimal cost, BigDecimal selling, BigDecimal profit, BigDecimal marginPct) {
        List<String> roles = List.of("finance", "direktur", "owner");
        String title = "⚠️ PERINGATAN: Transaksi Shipment Margin Minus!";
        String msg = String.format("Pengiriman '%s' mencatatkan kerugian operasional (Biaya: Rp %s, Tagihan: Rp %s, Defisit: Rp %s). Segera verifikasi kesepakatan tarif.",
                actInfo, formatRupiah(cost), formatRupiah(selling), formatRupiah(profit != null ? profit.abs() : BigDecimal.ZERO));

        for (String r : roles) {
            try {
                create(CreateNotificationInput.builder()
                        .targetRole(r)
                        .title(title)
                        .message(msg)
                        .category(NotificationCategory.SHIPMENT)
                        .severity(NotificationSeverity.WARNING)
                        .actionUrl("/dashboard/cashflow")
                        .metadata(String.format("{\"cost\":%s,\"selling\":%s,\"profit\":%s,\"margin_pct\":%s}",
                                cost, selling, profit, marginPct))
                        .build());
            } catch (Exception e) {
                log.error("Failed to dispatch margin minus notification: {}", e.getMessage());
            }
        }
    }
}
