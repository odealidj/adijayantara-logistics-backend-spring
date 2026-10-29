package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Forensic Audit & SLA Monitoring", description = "Endpoint pengawasan audit forensik, deteksi fraud, margin anomali, dan scorecard SLA staf")
@SecurityRequirement(name = "BearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('*', 'audit.view')")
    @Operation(summary = "Ringkasan Eksekutif KPI Audit", description = "Menghitung total logs, entri kritis, radar anomali, rata-rata lag kas dan invoice")
    public ResponseEntity<ApiResponse<AuditSummaryKpiDto>> getSummary(@ModelAttribute AuditFilterRequest filter) {
        AuditSummaryKpiDto summary = auditService.getSummaryKPI(filter);
        return ResponseEntity.ok(ApiResponse.success("Audit summary KPI retrieved", summary));
    }

    @GetMapping("/staff-scorecard")
    @PreAuthorize("hasAnyAuthority('*', 'audit.view')")
    @Operation(summary = "Scorecard Kepatuhan SLA Staf", description = "Rapor kedisiplinan penginputan kasir dan finance, persentase tepat waktu, dan rata-rata lag penginputan")
    public ResponseEntity<ApiResponse<List<StaffSlaItemDto>>> getStaffScorecard(@ModelAttribute AuditFilterRequest filter) {
        List<StaffSlaItemDto> scorecard = auditService.getStaffSLAPerformance(filter);
        return ResponseEntity.ok(ApiResponse.success("Staff SLA performance scorecard retrieved", scorecard));
    }

    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyAuthority('*', 'audit.view')")
    @Operation(summary = "Radar Anomali Margin & Fraud", description = "Daftar entri transaksi janggal, margin di bawah batas aman, void, dan penghapusan data berpaginasi")
    public ResponseEntity<ApiResponse<List<FraudAnomalyItemDto>>> getAnomalies(@ModelAttribute AuditFilterRequest filter) {
        Map<String, Object> data = auditService.getFraudAnomalies(filter);

        @SuppressWarnings("unchecked")
        List<FraudAnomalyItemDto> items = (List<FraudAnomalyItemDto>) data.get("items");
        long total = (long) data.get("total");
        int page = (int) data.get("page");
        int limit = (int) data.get("limit");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Fraud and margin anomalies retrieved", items, meta));
    }

    @GetMapping("/input-lag")
    @PreAuthorize("hasAnyAuthority('*', 'audit.view')")
    @Operation(summary = "Catatan Keterlambatan Penginputan Data", description = "Daftar transaksi yang diinput melampaui batas SLA toleransi (backlog input lag)")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getInputLag(@ModelAttribute AuditFilterRequest filter) {
        Map<String, Object> data = auditService.getInputLagRecords(filter);

        @SuppressWarnings("unchecked")
        List<AuditLogResponseDto> items = (List<AuditLogResponseDto>) data.get("items");
        long total = (long) data.get("total");
        int page = (int) data.get("page");
        int limit = (int) data.get("limit");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Input lag records retrieved", items, meta));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('*', 'audit.view')")
    @Operation(summary = "Log Forensik Audit Lengkap", description = "Daftar rekaman ledger forensik append-only dengan filter multivariat")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> listLogs(@ModelAttribute AuditFilterRequest filter) {
        Map<String, Object> data = auditService.listLogs(filter);

        @SuppressWarnings("unchecked")
        List<AuditLogResponseDto> items = (List<AuditLogResponseDto>) data.get("items");
        long total = (long) data.get("total");
        int page = (int) data.get("page");
        int limit = (int) data.get("limit");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Forensic audit logs retrieved", items, meta));
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'audit.view')")
    @Operation(summary = "Detail Entri Log Audit Forensik", description = "Melihat satu rekaman audit secara spesifik termasuk snapshot perbandingan nilai sebelum dan sesudah perubahan")
    public ResponseEntity<ApiResponse<AuditLogResponseDto>> getLogDetail(@PathVariable("id") Long id) {
        AuditLogResponseDto detail = auditService.getLogDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Audit log detail retrieved", detail));
    }
}
