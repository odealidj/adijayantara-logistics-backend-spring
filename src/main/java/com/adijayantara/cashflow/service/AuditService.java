package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.AuditLog;
import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.enums.AuditAction;
import com.adijayantara.cashflow.domain.enums.AuditSeverity;
import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.dto.UserSessionDto;
import com.adijayantara.cashflow.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize audit log values to JSON", e);
            return "{}";
        }
    }
}
