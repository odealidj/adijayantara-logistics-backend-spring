package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Strategic Analytics & Business Intelligence", description = "Endpoint analisis eksekutif, runway likuiditas, matriks rute BCG, kurva Pareto klien, dan efisiensi vendor")
@SecurityRequirement(name = "BearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/cashflow-runway")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.view')")
    @Operation(summary = "Proyeksi Arus Kas Runway & Siklus Konversi Kas (CCC)", description = "Menghitung proyeksi saldo kas bergulir 60 hari ke depan berdasarkan piutang jatuh tempo vs hutang vendor")
    public ResponseEntity<ApiResponse<CashflowRunwayResponse>> getCashflowRunway(
            @RequestParam(name = "days", required = false, defaultValue = "60") Integer days
    ) {
        CashflowRunwayResponse response = analyticsService.getCashflowRunway(days);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil data proyeksi arus kas runway", response));
    }

    @GetMapping("/route-matrix")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.view')")
    @Operation(summary = "Matriks Kuadran Profitabilitas Rute BCG", description = "Memetakan performa trayek/rute logistik ke dalam 4 kuadran: Bintang, Potensial, Sapi Perah, dan Evaluasi Kritis")
    public ResponseEntity<ApiResponse<RouteMatrixResponse>> getRouteMatrix(
            @RequestParam(name = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        RouteMatrixResponse response = analyticsService.getRouteMatrix(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil matriks kuadran profitabilitas rute BCG", response));
    }

    @GetMapping("/customer-discipline-pareto")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.view')")
    @Operation(summary = "Skor Kepatuhan DSO Customer & Analisis Pareto Omset 80/20", description = "Menghitung realisasi pembayaran klien vs janji TOP, reschedule count, dan kurva Pareto kontribusi omset")
    public ResponseEntity<ApiResponse<CustomerDisciplineAndParetoResponse>> getCustomerDisciplineAndPareto(
            @RequestParam(name = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        CustomerDisciplineAndParetoResponse response = analyticsService.getCustomerDisciplineAndPareto(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil data kepatuhan pelanggan dan analisis pareto", response));
    }

    @GetMapping("/vendor-efficiency")
    @PreAuthorize("hasAnyAuthority('*', 'cashflow.view')")
    @Operation(summary = "Analisis Efisiensi & Ketergantungan Rekanan Vendor", description = "Menghitung rasio konsentrasi armada rekanan, biaya pokok, dan rata-rata margin keuntungan per vendor")
    public ResponseEntity<ApiResponse<VendorEfficiencyResponse>> getVendorEfficiency(
            @RequestParam(name = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        VendorEfficiencyResponse response = analyticsService.getVendorEfficiency(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success("Berhasil mengambil analisis efisiensi dan konsentrasi vendor", response));
    }
}
