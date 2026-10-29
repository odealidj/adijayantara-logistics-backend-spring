package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.AuditLog;
import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.enums.AuditAction;
import com.adijayantara.cashflow.domain.enums.AuditSeverity;
import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(AuditLog auditLog) {
        if (auditLog.getActorName() == null || auditLog.getActorName().isEmpty()) {
            auditLog.setActorName("Sistem");
        }
        if (auditLog.getActorRole() == null || auditLog.getActorRole().isEmpty()) {
            auditLog.setActorRole("system");
        }
        if (auditLog.getSystemTimestamp() == null) {
            auditLog.setSystemTimestamp(LocalDateTime.now());
        }

        // Hitung SLA lag days
        if (auditLog.getEventDate() != null) {
            LocalDate sysDate = auditLog.getSystemTimestamp().toLocalDate();
            long diff = ChronoUnit.DAYS.between(auditLog.getEventDate(), sysDate);
            if (diff > 0) {
                auditLog.setLagDays((int) diff);
            }
        }

        // Auto-eskalasi severity & flag reason jika belum diset
        if (auditLog.getSeverity() == null || auditLog.getSeverity() == AuditSeverity.NORMAL) {
            if (auditLog.getAction() == AuditAction.DELETE || auditLog.getAction() == AuditAction.VOID) {
                auditLog.setSeverity(AuditSeverity.CRITICAL);
                if (auditLog.getFlagReason() == null || auditLog.getFlagReason().isEmpty()) {
                    auditLog.setFlagReason("Penghapusan data " + auditLog.getEntityType() + " (" + auditLog.getEntityReference() + ")");
                }
            } else if (auditLog.getLagDays() > 3) {
                auditLog.setSeverity(AuditSeverity.CRITICAL);
                if (auditLog.getFlagReason() == null || auditLog.getFlagReason().isEmpty()) {
                    auditLog.setFlagReason("Keterlambatan input " + auditLog.getLagDays() + " hari (melebihi toleransi SLA 3 hari)");
                }
            } else if (auditLog.getLagDays() >= 2) {
                auditLog.setSeverity(AuditSeverity.WARNING);
                if (auditLog.getFlagReason() == null || auditLog.getFlagReason().isEmpty()) {
                    auditLog.setFlagReason("Keterlambatan input " + auditLog.getLagDays() + " hari");
                }
            } else {
                auditLog.setSeverity(AuditSeverity.NORMAL);
            }
        }

        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void logCashflowCreate(CashflowEntry entry, UserSessionDto session) {
        if (entry == null) return;

        Map<String, Object> values = new HashMap<>();
        values.put("sequence_no", entry.getSequenceNo());
        values.put("entry_type", entry.getEntryType());
        values.put("act_info", entry.getActInformation());
        values.put("vendor_name", entry.getVendorNameRaw());
        values.put("kredit", entry.getKredit());
        values.put("debit", entry.getDebit());
        values.put("saldo", entry.getSaldo());
        values.put("grand_cost", entry.getGrandCost());
        values.put("grand_selling", entry.getGrandSelling());
        values.put("profit", entry.getProfit());
        values.put("margin_pct", entry.getMarginPct());

        String jsonValues = toJson(values);

        AuditSeverity severity = AuditSeverity.NORMAL;
        String flagReason = null;
        if (entry.getEntryType() == EntryType.SHIPMENT 
                && entry.getMarginPct() != null 
                && entry.getMarginPct().doubleValue() < 0.08 
                && entry.getGrandSelling() != null 
                && entry.getGrandSelling().doubleValue() > 0) {
            severity = AuditSeverity.CRITICAL;
            flagReason = String.format("Margin pengiriman di bawah standar (%.1f%%)", entry.getMarginPct().doubleValue() * 100);
        }

        AuditLog auditLog = AuditLog.builder()
                .actorId(session != null ? session.getUserId() : null)
                .actorName(session != null && session.getFullName() != null ? session.getFullName() : "Sistem")
                .actorRole(session != null && session.getRole() != null ? session.getRole() : "system")
                .action(AuditAction.CREATE)
                .entityType("cashflow")
                .entityId(String.valueOf(entry.getId()))
                .entityReference(String.format("Seq#%d (%s)", entry.getSequenceNo(), entry.getActInformation()))
                .eventDate(entry.getDateOfEntry())
                .severity(severity)
                .flagReason(flagReason)
                .newValues(jsonValues)
                .build();

        record(auditLog);
    }

    @Transactional
    public void logCashflowUpdate(CashflowEntry oldEntry, CashflowEntry newEntry, UserSessionDto session) {
        if (oldEntry == null || newEntry == null) return;

        Map<String, Object> oldVals = new HashMap<>();
        oldVals.put("kredit", oldEntry.getKredit());
        oldVals.put("debit", oldEntry.getDebit());
        oldVals.put("saldo", oldEntry.getSaldo());
        oldVals.put("grand_cost", oldEntry.getGrandCost());
        oldVals.put("grand_selling", oldEntry.getGrandSelling());
        oldVals.put("profit", oldEntry.getProfit());
        oldVals.put("margin_pct", oldEntry.getMarginPct());
        oldVals.put("act_info", oldEntry.getActInformation());

        Map<String, Object> newVals = new HashMap<>();
        newVals.put("kredit", newEntry.getKredit());
        newVals.put("debit", newEntry.getDebit());
        newVals.put("saldo", newEntry.getSaldo());
        newVals.put("grand_cost", newEntry.getGrandCost());
        newVals.put("grand_selling", newEntry.getGrandSelling());
        newVals.put("profit", newEntry.getProfit());
        newVals.put("margin_pct", newEntry.getMarginPct());
        newVals.put("act_info", newEntry.getActInformation());

        AuditSeverity severity = AuditSeverity.WARNING;
        String reason = "Koreksi transaksi kas Seq#" + newEntry.getSequenceNo();

        if (oldEntry.getCreatedAt() != null && ChronoUnit.HOURS.between(oldEntry.getCreatedAt(), LocalDateTime.now()) > 72) {
            severity = AuditSeverity.CRITICAL;
            reason = "Pengeditan kas lampau (>3 hari sejak dicatat): Seq#" + newEntry.getSequenceNo();
        }

        AuditLog auditLog = AuditLog.builder()
                .actorId(session != null ? session.getUserId() : null)
                .actorName(session != null && session.getFullName() != null ? session.getFullName() : "Sistem")
                .actorRole(session != null && session.getRole() != null ? session.getRole() : "system")
                .action(AuditAction.UPDATE)
                .entityType("cashflow")
                .entityId(String.valueOf(newEntry.getId()))
                .entityReference(String.format("Seq#%d (%s)", newEntry.getSequenceNo(), newEntry.getActInformation()))
                .eventDate(newEntry.getDateOfEntry())
                .severity(severity)
                .flagReason(reason)
                .oldValues(toJson(oldVals))
                .newValues(toJson(newVals))
                .build();

        record(auditLog);
    }

    @Transactional
    public void logCashflowDelete(CashflowEntry entry, UserSessionDto session) {
        if (entry == null) return;

        Map<String, Object> oldVals = new HashMap<>();
        oldVals.put("kredit", entry.getKredit());
        oldVals.put("debit", entry.getDebit());
        oldVals.put("saldo", entry.getSaldo());
        oldVals.put("grand_cost", entry.getGrandCost());
        oldVals.put("grand_selling", entry.getGrandSelling());
        oldVals.put("profit", entry.getProfit());
        oldVals.put("margin_pct", entry.getMarginPct());
        oldVals.put("act_info", entry.getActInformation());

        AuditLog auditLog = AuditLog.builder()
                .actorId(session != null ? session.getUserId() : null)
                .actorName(session != null && session.getFullName() != null ? session.getFullName() : "Sistem")
                .actorRole(session != null && session.getRole() != null ? session.getRole() : "system")
                .action(AuditAction.DELETE)
                .entityType("cashflow")
                .entityId(String.valueOf(entry.getId()))
                .entityReference(String.format("Seq#%d (%s)", entry.getSequenceNo(), entry.getActInformation()))
                .eventDate(entry.getDateOfEntry())
                .severity(AuditSeverity.CRITICAL)
                .flagReason(String.format("Penghapusan data cashflow (Seq#%d %s)", entry.getSequenceNo(), entry.getActInformation()))
                .oldValues(toJson(oldVals))
                .build();

        record(auditLog);
    }

    @Transactional
    public void logInvoiceCreate(com.adijayantara.cashflow.domain.entity.Invoice inv, UserSessionDto session) {
        if (inv == null) return;

        Map<String, Object> values = new HashMap<>();
        values.put("invoice_no", inv.getInvoiceNo());
        values.put("client_name", inv.getClientName());
        values.put("amount", inv.getAmount());
        values.put("status", inv.getStatus());
        values.put("top_days", inv.getTopDays());
        values.put("due_date", inv.getDueDate());

        AuditLog auditLog = AuditLog.builder()
                .actorId(session != null ? session.getUserId() : null)
                .actorName(session != null && session.getFullName() != null ? session.getFullName() : "Sistem")
                .actorRole(session != null && session.getRole() != null ? session.getRole() : "system")
                .action(AuditAction.CREATE)
                .entityType("invoice")
                .entityId(String.valueOf(inv.getId()))
                .entityReference(String.format("%s (%s)", inv.getInvoiceNo(), inv.getClientName()))
                .eventDate(inv.getShipmentDate())
                .severity(AuditSeverity.NORMAL)
                .newValues(toJson(values))
                .build();

        record(auditLog);
    }

    @Transactional
    public void logInvoiceReschedule(com.adijayantara.cashflow.domain.entity.Invoice inv, LocalDate prevDate, LocalDate newDate, String reason, UserSessionDto session) {
        if (inv == null) return;

        Map<String, Object> oldVals = Map.of("due_date", prevDate.toString());
        Map<String, Object> newVals = Map.of("due_date", newDate.toString(), "reason", reason);

        String flagReason = String.format("Perpanjangan tempo invoice %s: %s", inv.getInvoiceNo(), reason);

        AuditLog auditLog = AuditLog.builder()
                .actorId(session != null ? session.getUserId() : null)
                .actorName(session != null && session.getFullName() != null ? session.getFullName() : "Sistem")
                .actorRole(session != null && session.getRole() != null ? session.getRole() : "system")
                .action(AuditAction.RESCHEDULE_DUE_DATE)
                .entityType("invoice")
                .entityId(String.valueOf(inv.getId()))
                .entityReference(String.format("%s (%s)", inv.getInvoiceNo(), inv.getClientName()))
                .eventDate(inv.getShipmentDate())
                .severity(AuditSeverity.WARNING)
                .flagReason(flagReason)
                .oldValues(toJson(oldVals))
                .newValues(toJson(newVals))
                .build();

        record(auditLog);
    }

    @Transactional
    public void logInvoiceSettle(com.adijayantara.cashflow.domain.entity.Invoice inv, java.math.BigDecimal amount, String refNo, UserSessionDto session) {
        if (inv == null) return;

        Map<String, Object> values = new HashMap<>();
        values.put("settled_amount", amount);
        values.put("reference_no", refNo != null ? refNo : "");

        String flagReason = String.format("Pelunasan invoice %s sebesar Rp %s", inv.getInvoiceNo(), amount);

        AuditLog auditLog = AuditLog.builder()
                .actorId(session != null ? session.getUserId() : null)
                .actorName(session != null && session.getFullName() != null ? session.getFullName() : "Sistem")
                .actorRole(session != null && session.getRole() != null ? session.getRole() : "system")
                .action(AuditAction.SETTLE_INVOICE)
                .entityType("invoice")
                .entityId(String.valueOf(inv.getId()))
                .entityReference(String.format("%s (%s)", inv.getInvoiceNo(), inv.getClientName()))
                .eventDate(inv.getShipmentDate())
                .severity(AuditSeverity.NORMAL)
                .flagReason(flagReason)
                .newValues(toJson(values))
                .build();

        record(auditLog);
    }

    @Transactional
    public void logInvoiceDelete(com.adijayantara.cashflow.domain.entity.Invoice inv, UserSessionDto session) {
        if (inv == null) return;

        Map<String, Object> oldVals = new HashMap<>();
        oldVals.put("invoice_no", inv.getInvoiceNo());
        oldVals.put("client_name", inv.getClientName());
        oldVals.put("amount", inv.getAmount());
        oldVals.put("status", inv.getStatus());

        String reason = String.format("Penghapusan invoice %s (%s)", inv.getInvoiceNo(), inv.getClientName());

        AuditLog auditLog = AuditLog.builder()
                .actorId(session != null ? session.getUserId() : null)
                .actorName(session != null && session.getFullName() != null ? session.getFullName() : "Sistem")
                .actorRole(session != null && session.getRole() != null ? session.getRole() : "system")
                .action(AuditAction.DELETE)
                .entityType("invoice")
                .entityId(String.valueOf(inv.getId()))
                .entityReference(String.format("%s (%s)", inv.getInvoiceNo(), inv.getClientName()))
                .eventDate(inv.getShipmentDate())
                .severity(AuditSeverity.CRITICAL)
                .flagReason(reason)
                .oldValues(toJson(oldVals))
                .build();

        record(auditLog);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize audit log values to JSON", e);
            return "{}";
        }
    }

    // -------------------------------------------------------------------------
    // Forensic Audit Queries & Scorecard Aggregations
    // -------------------------------------------------------------------------

    private String buildWhere(AuditFilterRequest filter, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        if (filter == null) {
            return "";
        }

        if (filter.getActor_id() != null) {
            conditions.add("actor_id = :actor_id");
            params.put("actor_id", filter.getActor_id());
        }
        if (filter.getActor_name() != null && !filter.getActor_name().trim().isEmpty()) {
            conditions.add("actor_name ILIKE :actor_name");
            params.put("actor_name", "%" + filter.getActor_name().trim() + "%");
        }
        if (filter.getAction() != null && !filter.getAction().trim().isEmpty()) {
            conditions.add("action::text = :action");
            params.put("action", filter.getAction().trim());
        }
        if (filter.getEntity_type() != null && !filter.getEntity_type().trim().isEmpty()) {
            conditions.add("entity_type = :entity_type");
            params.put("entity_type", filter.getEntity_type().trim());
        }
        if (filter.getSeverity() != null && !filter.getSeverity().trim().isEmpty()) {
            conditions.add("severity::text = :severity");
            params.put("severity", filter.getSeverity().trim().toUpperCase());
        }
        if (filter.getMin_lag_days() != null) {
            conditions.add("lag_days >= :min_lag_days");
            params.put("min_lag_days", filter.getMin_lag_days());
        }
        if (filter.getStart_date() != null) {
            conditions.add("event_date >= :start_date");
            params.put("start_date", filter.getStart_date());
        }
        if (filter.getEnd_date() != null) {
            conditions.add("event_date <= :end_date");
            params.put("end_date", filter.getEnd_date());
        }
        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            conditions.add("(entity_reference ILIKE :search OR flag_reason ILIKE :search OR actor_name ILIKE :search)");
            params.put("search", "%" + filter.getSearch().trim() + "%");
        }

        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    @Transactional(readOnly = true)
    public AuditSummaryKpiDto getSummaryKPI(AuditFilterRequest filter) {
        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(filter, params);

        String sql = """
            SELECT 
                COUNT(*) AS total_logs,
                COUNT(*) FILTER (WHERE severity = 'CRITICAL') AS total_critical,
                COUNT(*) FILTER (WHERE severity = 'WARNING') AS total_warning,
                COUNT(*) FILTER (WHERE lag_days > 1) AS total_late_inputs,
                COALESCE(AVG(lag_days) FILTER (WHERE entity_type = 'cashflow'), 0) AS avg_cashflow_lag_days,
                COALESCE(AVG(lag_days) FILTER (WHERE entity_type = 'invoice'), 0) AS avg_invoice_lag_days,
                COUNT(*) FILTER (WHERE severity IN ('WARNING', 'CRITICAL') OR action IN ('DELETE', 'VOID', 'RESCHEDULE_DUE_DATE')) AS total_anomalies_count,
                COUNT(DISTINCT COALESCE(actor_name, 'Sistem')) AS total_staff_monitored
            FROM audit_logs
            """ + where;

        var client = jdbcClient.sql(sql);
        params.forEach(client::param);

        return client.query((rs, rowNum) -> AuditSummaryKpiDto.builder()
                .totalLogs(rs.getLong("total_logs"))
                .totalCritical(rs.getLong("total_critical"))
                .totalWarning(rs.getLong("total_warning"))
                .totalLateInputs(rs.getLong("total_late_inputs"))
                .avgCashflowLagDays(Math.round(rs.getDouble("avg_cashflow_lag_days") * 10.0) / 10.0)
                .avgInvoiceLagDays(Math.round(rs.getDouble("avg_invoice_lag_days") * 10.0) / 10.0)
                .totalAnomaliesCount(rs.getLong("total_anomalies_count"))
                .totalStaffMonitored(rs.getLong("total_staff_monitored"))
                .build()
        ).single();
    }

    @Transactional(readOnly = true)
    public List<StaffSlaItemDto> getStaffSLAPerformance(AuditFilterRequest filter) {
        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(filter, params);

        String sql = """
            SELECT 
                actor_id,
                actor_name,
                actor_role,
                COUNT(*) AS total_entries,
                COUNT(*) FILTER (WHERE lag_days <= 1) AS on_time_entries,
                COUNT(*) FILTER (WHERE lag_days BETWEEN 2 AND 3) AS acceptable_entries,
                COUNT(*) FILTER (WHERE lag_days > 3) AS critical_late_entries,
                COALESCE(ROUND((COUNT(*) FILTER (WHERE lag_days <= 1)::numeric / NULLIF(COUNT(*), 0)) * 100, 1), 0) AS on_time_percentage,
                COALESCE(ROUND(AVG(lag_days)::numeric, 1), 0) AS avg_lag_days,
                COUNT(*) FILTER (WHERE action = 'UPDATE') AS total_edits,
                COUNT(*) FILTER (WHERE action IN ('DELETE', 'VOID')) AS total_deletions,
                TO_CHAR(MAX(created_at), 'YYYY-MM-DD"T"HH24:MI:SS') AS last_active_at
            FROM audit_logs
            """ + where + " GROUP BY actor_id, actor_name, actor_role ORDER BY critical_late_entries DESC, avg_lag_days DESC, total_entries DESC";

        var client = jdbcClient.sql(sql);
        params.forEach(client::param);

        return client.query((rs, rowNum) -> {
            UUID actorId = null;
            String idStr = rs.getString("actor_id");
            if (idStr != null) {
                try { actorId = UUID.fromString(idStr); } catch (Exception ignored) {}
            }
            return StaffSlaItemDto.builder()
                    .actorId(actorId)
                    .actorName(rs.getString("actor_name"))
                    .actorRole(rs.getString("actor_role"))
                    .totalEntries(rs.getLong("total_entries"))
                    .onTimeEntries(rs.getLong("on_time_entries"))
                    .acceptableEntries(rs.getLong("acceptable_entries"))
                    .criticalLateEntries(rs.getLong("critical_late_entries"))
                    .onTimePercentage(rs.getDouble("on_time_percentage"))
                    .avgLagDays(rs.getDouble("avg_lag_days"))
                    .totalEdits(rs.getLong("total_edits"))
                    .totalDeletions(rs.getLong("total_deletions"))
                    .lastActiveAt(rs.getString("last_active_at"))
                    .build();
        }).list();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFraudAnomalies(AuditFilterRequest filter) {
        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(filter, params);
        String anomalyCond = "(severity IN ('WARNING', 'CRITICAL') OR action IN ('DELETE', 'VOID', 'RESCHEDULE_DUE_DATE') OR flag_reason ILIKE '%margin%' OR lag_days > 3)";

        if (where.isEmpty()) {
            where = " WHERE " + anomalyCond;
        } else {
            where += " AND " + anomalyCond;
        }

        String countSql = "SELECT COUNT(*) FROM audit_logs" + where;
        var countClient = jdbcClient.sql(countSql);
        params.forEach(countClient::param);
        long total = countClient.query(Long.class).single();

        int page = filter != null ? filter.getPage() : 1;
        int limit = filter != null ? filter.getLimit() : 50;
        int offset = (page - 1) * limit;

        String selectSql = """
            SELECT 
                id, actor_name, actor_role, action, entity_type, entity_id, entity_reference,
                event_date, system_timestamp, lag_days, severity, COALESCE(flag_reason, '') AS flag_reason,
                COALESCE(old_values, '{}'::jsonb)::text AS old_values,
                COALESCE(new_values, '{}'::jsonb)::text AS new_values,
                created_at
            FROM audit_logs
            """ + where + " ORDER BY severity = 'CRITICAL' DESC, lag_days DESC, id DESC LIMIT :limit OFFSET :offset";

        params.put("limit", limit);
        params.put("offset", offset);

        var selectClient = jdbcClient.sql(selectSql);
        params.forEach(selectClient::param);

        List<FraudAnomalyItemDto> items = selectClient.query((rs, rowNum) -> FraudAnomalyItemDto.builder()
                .id(rs.getLong("id"))
                .actorName(rs.getString("actor_name"))
                .actorRole(rs.getString("actor_role"))
                .action(rs.getString("action"))
                .entityType(rs.getString("entity_type"))
                .entityId(rs.getString("entity_id"))
                .entityReference(rs.getString("entity_reference"))
                .eventDate(rs.getObject("event_date", LocalDate.class))
                .systemTimestamp(rs.getObject("system_timestamp", LocalDateTime.class))
                .lagDays(rs.getInt("lag_days"))
                .severity(rs.getString("severity"))
                .flagReason(rs.getString("flag_reason"))
                .oldValues(parseJson(rs.getString("old_values")))
                .newValues(parseJson(rs.getString("new_values")))
                .createdAt(rs.getObject("created_at", LocalDateTime.class))
                .build()
        ).list();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInputLagRecords(AuditFilterRequest filter) {
        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(filter, params);
        String lagCond = "lag_days > 0";

        if (where.isEmpty()) {
            where = " WHERE " + lagCond;
        } else {
            where += " AND " + lagCond;
        }

        String countSql = "SELECT COUNT(*) FROM audit_logs" + where;
        var countClient = jdbcClient.sql(countSql);
        params.forEach(countClient::param);
        long total = countClient.query(Long.class).single();

        int page = filter != null ? filter.getPage() : 1;
        int limit = filter != null ? filter.getLimit() : 50;
        int offset = (page - 1) * limit;

        String selectSql = """
            SELECT 
                id, actor_id, actor_name, actor_role, action, entity_type, entity_id, entity_reference,
                event_date, system_timestamp, lag_days, severity, flag_reason,
                COALESCE(old_values, '{}'::jsonb)::text AS old_values,
                COALESCE(new_values, '{}'::jsonb)::text AS new_values,
                ip_address, user_agent, created_at
            FROM audit_logs
            """ + where + " ORDER BY lag_days DESC, id DESC LIMIT :limit OFFSET :offset";

        params.put("limit", limit);
        params.put("offset", offset);

        var selectClient = jdbcClient.sql(selectSql);
        params.forEach(selectClient::param);

        List<AuditLogResponseDto> items = selectClient.query((rs, rowNum) -> {
            UUID actorId = null;
            String idStr = rs.getString("actor_id");
            if (idStr != null) {
                try { actorId = UUID.fromString(idStr); } catch (Exception ignored) {}
            }
            return AuditLogResponseDto.builder()
                    .id(rs.getLong("id"))
                    .actorId(actorId)
                    .actorName(rs.getString("actor_name"))
                    .actorRole(rs.getString("actor_role"))
                    .action(rs.getString("action"))
                    .entityType(rs.getString("entity_type"))
                    .entityId(rs.getString("entity_id"))
                    .entityReference(rs.getString("entity_reference"))
                    .eventDate(rs.getObject("event_date", LocalDate.class))
                    .systemTimestamp(rs.getObject("system_timestamp", LocalDateTime.class))
                    .lagDays(rs.getInt("lag_days"))
                    .severity(rs.getString("severity"))
                    .flagReason(rs.getString("flag_reason"))
                    .oldValues(parseJson(rs.getString("old_values")))
                    .newValues(parseJson(rs.getString("new_values")))
                    .ipAddress(rs.getString("ip_address"))
                    .userAgent(rs.getString("user_agent"))
                    .createdAt(rs.getObject("created_at", LocalDateTime.class))
                    .build();
        }).list();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listLogs(AuditFilterRequest filter) {
        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(filter, params);

        String countSql = "SELECT COUNT(*) FROM audit_logs" + where;
        var countClient = jdbcClient.sql(countSql);
        params.forEach(countClient::param);
        long total = countClient.query(Long.class).single();

        int page = filter != null ? filter.getPage() : 1;
        int limit = filter != null ? filter.getLimit() : 50;
        int offset = (page - 1) * limit;

        String selectSql = """
            SELECT 
                id, actor_id, actor_name, actor_role, action, entity_type, entity_id, entity_reference,
                event_date, system_timestamp, lag_days, severity, flag_reason,
                COALESCE(old_values, '{}'::jsonb)::text AS old_values,
                COALESCE(new_values, '{}'::jsonb)::text AS new_values,
                ip_address, user_agent, created_at
            FROM audit_logs
            """ + where + " ORDER BY id DESC LIMIT :limit OFFSET :offset";

        params.put("limit", limit);
        params.put("offset", offset);

        var selectClient = jdbcClient.sql(selectSql);
        params.forEach(selectClient::param);

        List<AuditLogResponseDto> items = selectClient.query((rs, rowNum) -> {
            UUID actorId = null;
            String idStr = rs.getString("actor_id");
            if (idStr != null) {
                try { actorId = UUID.fromString(idStr); } catch (Exception ignored) {}
            }
            return AuditLogResponseDto.builder()
                    .id(rs.getLong("id"))
                    .actorId(actorId)
                    .actorName(rs.getString("actor_name"))
                    .actorRole(rs.getString("actor_role"))
                    .action(rs.getString("action"))
                    .entityType(rs.getString("entity_type"))
                    .entityId(rs.getString("entity_id"))
                    .entityReference(rs.getString("entity_reference"))
                    .eventDate(rs.getObject("event_date", LocalDate.class))
                    .systemTimestamp(rs.getObject("system_timestamp", LocalDateTime.class))
                    .lagDays(rs.getInt("lag_days"))
                    .severity(rs.getString("severity"))
                    .flagReason(rs.getString("flag_reason"))
                    .oldValues(parseJson(rs.getString("old_values")))
                    .newValues(parseJson(rs.getString("new_values")))
                    .ipAddress(rs.getString("ip_address"))
                    .userAgent(rs.getString("user_agent"))
                    .createdAt(rs.getObject("created_at", LocalDateTime.class))
                    .build();
        }).list();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }

    @Transactional(readOnly = true)
    public AuditLogResponseDto getLogDetail(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Audit log tidak ditemukan dengan ID: " + id));

        return AuditLogResponseDto.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .actorRole(log.getActorRole())
                .action(log.getAction() != null ? log.getAction().name() : "")
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityReference(log.getEntityReference())
                .eventDate(log.getEventDate())
                .systemTimestamp(log.getSystemTimestamp())
                .lagDays(log.getLagDays())
                .severity(log.getSeverity() != null ? log.getSeverity().name() : "")
                .flagReason(log.getFlagReason())
                .oldValues(parseJson(log.getOldValues()))
                .newValues(parseJson(log.getNewValues()))
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}

