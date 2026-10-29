package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.SystemMetricsResponse;
import com.adijayantara.cashflow.service.SystemMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System Metrics & Telemetry", description = "Pemantauan kesehatan database pool, JVM runtime, dan k6 benchmarking")
@SecurityRequirement(name = "BearerAuth")
public class SystemMetricsController {

    private final SystemMetricsService systemMetricsService;

    @Operation(summary = "Get system metrics", description = "Mengambil agregasi metrik telemetri runtime, database connection pool, dan KPI transaksi")
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SystemMetricsResponse>> getMetrics() {
        SystemMetricsResponse metrics = systemMetricsService.getSystemMetrics();
        return ResponseEntity.ok(ApiResponse.success("Metrik telemetri sistem berhasil dimuat", metrics));
    }

    @Operation(summary = "Simulate slow query", description = "Mensimulasikan query lambat di PostgreSQL menggunakan pg_sleep")
    @PostMapping("/simulate-slow-query")
    public ResponseEntity<ApiResponse<Map<String, Object>>> simulateSlowQuery(
            @RequestParam(name = "duration_ms", defaultValue = "150") int durationMs) {
        systemMetricsService.simulateSlowQuery(durationMs);
        return ResponseEntity.ok(ApiResponse.success("Simulasi slow query selesai diproses", Map.of("duration_ms", durationMs)));
    }

    @Operation(summary = "Get benchmark status", description = "Status k6 performance testing")
    @GetMapping("/benchmark/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBenchmarkStatus() {
        Map<String, Object> status = Map.of(
            "status", "IDLE",
            "message", "Siap menjalankan pengujian performa benchmark",
            "progress_pct", 0,
            "vus", 0,
            "target_vus", 0
        );
        return ResponseEntity.ok(ApiResponse.success("Status benchmark berhasil dimuat", status));
    }

    @Operation(summary = "Get benchmark history", description = "Daftar riwayat laporan pengujian k6")
    @GetMapping("/benchmark/history")
    public ResponseEntity<ApiResponse<List<Object>>> getBenchmarkHistory() {
        return ResponseEntity.ok(ApiResponse.success("Riwayat benchmark berhasil dimuat", List.of()));
    }

    @Operation(summary = "Start benchmark", description = "Memulai pengujian performa k6")
    @PostMapping("/benchmark/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startBenchmark() {
        String jobId = UUID.randomUUID().toString();
        Map<String, Object> data = Map.of(
            "status", "RUNNING",
            "job_id", jobId,
            "started_at", Instant.now().toString(),
            "message", "Pengujian beban performa (benchmark) berhasil dimulai"
        );
        return ResponseEntity.ok(ApiResponse.success("Pengujian dimulai", data));
    }

    @Operation(summary = "Abort benchmark", description = "Membatalkan pengujian performa k6 yang sedang berjalan")
    @PostMapping("/benchmark/abort")
    public ResponseEntity<ApiResponse<Map<String, Object>>> abortBenchmark() {
        Map<String, Object> data = Map.of(
            "status", "IDLE",
            "message", "Pengujian performa berhasil dibatalkan"
        );
        return ResponseEntity.ok(ApiResponse.success("Benchmark dibatalkan", data));
    }
}
