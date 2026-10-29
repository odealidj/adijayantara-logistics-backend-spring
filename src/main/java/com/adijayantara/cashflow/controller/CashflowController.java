package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.service.CashflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cashflow")
@RequiredArgsConstructor
@Tag(name = "Cashflow & Shipment Management", description = "Endpoint operasional arus kas, shipment logistik, dan rolling balance")
@SecurityRequirement(name = "BearerAuth")
public class CashflowController {

    private final CashflowService cashflowService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.view')")
    @Operation(summary = "Daftar Transaksi Kas Berjalan", description = "Menampilkan data arus kas berpaginasi dengan filter periode, tipe, remarks, dan vendor")
    public ResponseEntity<ApiResponse<List<CashflowEntryResponseDto>>> list(@ModelAttribute CashflowFilterRequest filter) {
        Map<String, Object> data = cashflowService.getDashboardData(filter);

        @SuppressWarnings("unchecked")
        List<CashflowEntryResponseDto> items = (List<CashflowEntryResponseDto>) data.get("items");
        long total = (long) data.get("total");
        int page = (int) data.get("page");
        int limit = (int) data.get("limit");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Cashflow data retrieved", items, meta));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.view')")
    @Operation(summary = "Ringkasan Eksekutif Kas & Profit", description = "Menghitung saldo kas riil terkini, total kredit, total debit, akumulasi profit, dan hutang belum lunas")
    public ResponseEntity<ApiResponse<CashflowSummaryDto>> getSummary(@ModelAttribute CashflowFilterRequest filter) {
        CashflowSummaryDto summary = cashflowService.getSummary(filter);
        return ResponseEntity.ok(ApiResponse.success("Summary retrieved", summary));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.view')")
    @Operation(summary = "Periode Transaksi Tersedia", description = "Menampilkan daftar bulan-tahun yang memiliki riwayat transaksi aktif")
    public ResponseEntity<ApiResponse<List<AvailablePeriodDto>>> getAvailablePeriods() {
        List<AvailablePeriodDto> periods = cashflowService.getAvailablePeriods();
        return ResponseEntity.ok(ApiResponse.success("Available periods retrieved", periods));
    }

    @PostMapping("/topup")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.create')")
    @Operation(summary = "Catat Kas Masuk (Top-Up)", description = "Injeksi modal kas masuk dengan penghitungan saldo kas berjalan secara berurutan")
    public ResponseEntity<ApiResponse<CashflowEntryResponseDto>> createTopUp(
            @Valid @RequestBody CreateTopUpRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        CashflowEntryResponseDto result = cashflowService.recordTopUp(req, session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Top-Up recorded successfully", result));
    }

    @PostMapping("/shipment")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.create')")
    @Operation(summary = "Catat Transaksi Pengiriman (Shipment)", description = "Pencatatan biaya transportasi (HPP), pendapatan kotor, jatuh tempo vendor, margin, dan auto-register vendor/customer")
    public ResponseEntity<ApiResponse<CashflowEntryResponseDto>> createShipment(
            @Valid @RequestBody CreateShipmentRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        CashflowEntryResponseDto result = cashflowService.recordShipment(req, session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment recorded successfully", result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.edit')")
    @Operation(summary = "Perbarui Transaksi Kas", description = "Koreksi baris transaksi dengan penyesuaian selisih saldo berjalan pada entri berikutnya dan audit log")
    public ResponseEntity<ApiResponse<CashflowEntryResponseDto>> update(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateCashflowRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        CashflowEntryResponseDto result = cashflowService.updateEntry(id, req, session);
        return ResponseEntity.ok(ApiResponse.success("Entry updated successfully", result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.delete')")
    @Operation(summary = "Hapus Transaksi Kas", description = "Pengarsipan ke riwayat dan penghapusan transaksi dengan penyesuaian saldo berjalan pada baris-baris setelahnya")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        cashflowService.deleteEntry(id, session);
        return ResponseEntity.ok(ApiResponse.success("Entry deleted successfully", null));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.edit')")
    @Operation(summary = "Ubah Status Pembayaran (Remarks)", description = "Memperbarui status PAID / UNPAID / PENDING pada transaksi kas/shipment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateStatusRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        cashflowService.updatePaymentStatus(id, req.getRemarks(), session);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", Map.of(
                "id", id,
                "remarks", req.getRemarks()
        )));
    }
}
