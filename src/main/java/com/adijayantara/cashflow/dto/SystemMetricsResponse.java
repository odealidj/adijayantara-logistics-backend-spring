package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMetricsResponse {

    @JsonProperty("prometheus_connected")
    private Boolean prometheusConnected;

    @JsonProperty("runtime")
    private RuntimeMetrics runtime;

    @JsonProperty("gateway")
    private GatewayMetrics gateway;

    @JsonProperty("database_pool")
    private DBPoolMetrics databasePool;

    @JsonProperty("business_kpi")
    private BusinessKPIMetrics businessKpi;

    @JsonProperty("slow_queries")
    @Builder.Default
    private List<SlowQueryRecord> slowQueries = new ArrayList<>();

    @JsonProperty("timestamp")
    private String timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuntimeMetrics {
        @JsonProperty("go_version")
        private String goVersion;

        @JsonProperty("num_goroutine")
        private Integer numGoroutine;

        @JsonProperty("alloc_mb")
        private Double allocMb;

        @JsonProperty("sys_mb")
        private Double sysMb;

        @JsonProperty("num_gc")
        private Long numGc;

        @JsonProperty("uptime_seconds")
        private Long uptimeSeconds;

        @JsonProperty("redis_status")
        private String redisStatus;

        @JsonProperty("redis_ping_ms")
        private Double redisPingMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewayMetrics {
        @JsonProperty("rps")
        private Double rps;

        @JsonProperty("error_rate_pct")
        private Double errorRatePct;

        @JsonProperty("p95_latency_ms")
        private Double p95LatencyMs;

        @JsonProperty("total_requests")
        private Long totalRequests;

        @JsonProperty("idempotency_hits")
        private Long idempotencyHits;

        @JsonProperty("idempotency_misses")
        private Long idempotencyMisses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DBPoolMetrics {
        @JsonProperty("open")
        private Integer open;

        @JsonProperty("in_use")
        private Integer inUse;

        @JsonProperty("idle")
        private Integer idle;

        @JsonProperty("max_open")
        private Integer maxOpen;

        @JsonProperty("wait_count")
        private Long waitCount;

        @JsonProperty("wait_duration_ms")
        private Double waitDurationMs;

        @JsonProperty("postgres")
        private PostgresContainerMetrics postgres;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostgresContainerMetrics {
        @JsonProperty("cpu_percent")
        private String cpuPercent;

        @JsonProperty("mem_usage_mb")
        private String memUsageMb;

        @JsonProperty("pids")
        private String pids;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessKPIMetrics {
        @JsonProperty("shipments_today")
        private Long shipmentsToday;

        @JsonProperty("topups_today")
        private Long topupsToday;

        @JsonProperty("invoices_created_today")
        private Long invoicesCreatedToday;

        @JsonProperty("invoices_paid_today")
        private Long invoicesPaidToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlowQueryRecord {
        @JsonProperty("repository")
        private String repository;

        @JsonProperty("operation")
        private String operation;

        @JsonProperty("query_preview")
        private String queryPreview;

        @JsonProperty("duration_ms")
        private Double durationMs;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("has_error")
        private Boolean hasError;
    }
}

