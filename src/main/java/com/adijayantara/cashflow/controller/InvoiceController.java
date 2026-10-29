package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.service.InvoiceService;
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
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoicing & Billing Management", description = "Endpoint penagihan piutang klien, pelunasan kas otomatis, dan perpanjangan jatuh tempo")
@SecurityRequirement(name = "BearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('*', 'invoices.view')")
    @Operation(summary = "Daftar Invoice Piutang", description = "Menampilkan daftar invoice berpaginasi dengan filter periode, status (UNPAID, PAID, OVERDUE), dan nama klien")
    public ResponseEntity<ApiResponse<List<InvoiceResponseDto>>> list(@ModelAttribute InvoiceFilterRequest filter) {
        Map<String, Object> data = invoiceService.listInvoices(filter);

        @SuppressWarnings("unchecked")
        List<InvoiceResponseDto> items = (List<InvoiceResponseDto>) data.get("items");
        long total = (long) data.get("total");
        int page = (int) data.get("page");
        int limit = (int) data.get("limit");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Invoices retrieved", items, meta));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('*', 'invoices.view')")
    @Operation(summary = "Ringkasan Eksekutif Piutang", description = "Menghitung total tagihan, nominal yang sudah lunas, belum lunas, dan tagihan macet/overdue")
    public ResponseEntity<ApiResponse<InvoiceSummaryDto>> getSummary(@ModelAttribute InvoiceFilterRequest filter) {
        InvoiceSummaryDto summary = invoiceService.getSummary(filter);
        return ResponseEntity.ok(ApiResponse.success("Invoice summary retrieved", summary));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAnyAuthority('*', 'invoices.view')")
    @Operation(summary = "Periode Invoice Tersedia", description = "Menampilkan daftar bulan-tahun penerbitan invoice aktif")
    public ResponseEntity<ApiResponse<List<AvailablePeriodDto>>> getAvailablePeriods() {
        List<AvailablePeriodDto> periods = invoiceService.getAvailablePeriods();
        return ResponseEntity.ok(ApiResponse.success("Available invoice periods retrieved", periods));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'invoices.view')")
    @Operation(summary = "Detail Invoice", description = "Mengambil rincian data invoice berdasarkan ID")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> getByID(@PathVariable("id") Integer id) {
        InvoiceResponseDto invoice = invoiceService.getInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice retrieved", invoice));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('*', 'invoices.view')")
    @Operation(summary = "Riwayat Audit Invoice", description = "Menampilkan rekam jejak pembayaran dan perpanjangan jatuh tempo (reschedule logs)")
    public ResponseEntity<ApiResponse<InvoiceHistorySummaryDto>> getHistory(@PathVariable("id") Integer id) {
        InvoiceHistorySummaryDto history = invoiceService.getInvoiceHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Riwayat invoice berhasil dimuat", history));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('*', 'invoices.create')")
    @Operation(summary = "Buat Invoice Baru", description = "Penerbitan tagihan klien dengan auto-kalkulasi tanggal jatuh tempo berdasarkan TOP days")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> create(
            @Valid @RequestBody CreateInvoiceRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        InvoiceResponseDto result = invoiceService.createInvoice(req, session);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice berhasil dibuat", result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'invoices.edit')")
    @Operation(summary = "Perbarui Invoice", description = "Memperbarui detail invoice klien dan menyesuaikan tanggal jatuh tempo")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> update(
            @PathVariable("id") Integer id,
            @Valid @RequestBody CreateInvoiceRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        InvoiceResponseDto result = invoiceService.updateInvoice(id, req, session);
        return ResponseEntity.ok(ApiResponse.success("Invoice berhasil diperbarui", result));
    }

    @RequestMapping(value = "/{id}/pay", method = {RequestMethod.POST, RequestMethod.PATCH})
    @PreAuthorize("hasAnyAuthority('*', 'invoices.mark_paid')")
    @Operation(summary = "Pelunasan Invoice (Settle)", description = "Mencatat pelunasan invoice, mencatat riwayat pembayaran, dan otomatis menginjeksi kas masuk (INVOICE_PAYMENT) ke buku kas operasional")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> markPaid(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) SettleInvoiceRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        if (req == null) {
            req = new SettleInvoiceRequest();
        }
        InvoiceResponseDto result = invoiceService.settleInvoice(id, req, session);
        return ResponseEntity.ok(ApiResponse.success("Invoice berhasil dilunasi", result));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyAuthority('*', 'invoices.edit')")
    @Operation(summary = "Perpanjang Jatuh Tempo (Reschedule)", description = "Memperpanjang tanggal jatuh tempo tagihan dengan pencatatan alasan audit resmi")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> rescheduleDueDate(
            @PathVariable("id") Integer id,
            @Valid @RequestBody RescheduleDueDateRequest req,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        InvoiceResponseDto result = invoiceService.rescheduleDueDate(id, req, session);
        return ResponseEntity.ok(ApiResponse.success("Tanggal jatuh tempo invoice berhasil diperpanjang", result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'invoices.delete')")
    @Operation(summary = "Hapus Invoice (Soft Delete)", description = "Menonaktifkan invoice dan mencatat aksi ke log audit")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserSessionDto session
    ) {
        invoiceService.deleteInvoice(id, session);
        return ResponseEntity.ok(ApiResponse.success("Invoice berhasil dihapus", null));
    }
}
