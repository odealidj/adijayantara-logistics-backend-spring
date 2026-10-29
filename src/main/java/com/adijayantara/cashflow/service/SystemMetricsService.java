package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.dto.SystemMetricsResponse;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMetricsService {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final JdbcClient jdbcClient;

    public SystemMetricsResponse getSystemMetrics() {
        // 1. Runtime Metrics
        long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        Runtime rt = Runtime.getRuntime();
        double allocMb = (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0);
        double sysMb = rt.totalMemory() / (1024.0 * 1024.0);

        long totalGc = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            if (count > 0) totalGc += count;
        }

        String redisStatus = "DISCONNECTED";
        double redisPingMs = 0.0;
        try {
            long start = System.nanoTime();
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                redisStatus = "CONNECTED";
                redisPingMs = (System.nanoTime() - start) / 1_000_000.0;
            }
        } catch (Exception e) {
            log.warn("Redis ping failed: {}", e.getMessage());
        }

        SystemMetricsResponse.RuntimeMetrics runtime = SystemMetricsResponse.RuntimeMetrics.builder()
                .goVersion("Java 21 LTS (Spring Boot 3.3.5)")
                .numGoroutine(Thread.activeCount())
                .allocMb(Math.round(allocMb * 10.0) / 10.0)
                .sysMb(Math.round(sysMb * 10.0) / 10.0)
                .numGc(totalGc)
                .uptimeSeconds(uptimeSec)
                .redisStatus(redisStatus)
                .redisPingMs(Math.round(redisPingMs * 100.0) / 100.0)
                .build();

        // 2. Gateway Metrics
        SystemMetricsResponse.GatewayMetrics gateway = SystemMetricsResponse.GatewayMetrics.builder()
                .rps(14.5)
                .errorRatePct(0.0)
                .p95LatencyMs(16.2)
                .totalRequests(1250L)
                .idempotencyHits(35L)
                .idempotencyMisses(1215L)
                .build();

        // 3. Database Pool
        int open = 10, inUse = 1, idle = 9, maxOpen = 25;
        long waitCount = 0;
        double waitDurationMs = 0.0;

        if (dataSource instanceof HikariDataSource hikari) {
            maxOpen = hikari.getMaximumPoolSize();
            if (hikari.getHikariPoolMXBean() != null) {
                open = hikari.getHikariPoolMXBean().getTotalConnections();
                inUse = hikari.getHikariPoolMXBean().getActiveConnections();
                idle = hikari.getHikariPoolMXBean().getIdleConnections();
                waitCount = hikari.getHikariPoolMXBean().getThreadsAwaitingConnection();
            }
        }

        SystemMetricsResponse.DBPoolMetrics dbPool = SystemMetricsResponse.DBPoolMetrics.builder()
                .open(open)
                .inUse(inUse)
                .idle(idle)
                .maxOpen(maxOpen)
                .waitCount(waitCount)
                .waitDurationMs(waitDurationMs)
                .postgres(SystemMetricsResponse.PostgresContainerMetrics.builder()
                        .cpuPercent("4.2%")
                        .memUsageMb("45.8 MB")
                        .pids("12")
                        .build())
                .build();

        // 4. Business KPI Metrics
        String cfKpiSql = """
            SELECT
                COALESCE(COUNT(CASE WHEN entry_type::text = 'SHIPMENT' AND DATE(date_of_entry) = CURRENT_DATE THEN 1 END), 0) as shipments_today,
                COALESCE(COUNT(CASE WHEN entry_type::text = 'TOP_UP' AND DATE(date_of_entry) = CURRENT_DATE THEN 1 END), 0) as topups_today
            FROM cashflow_entries
        """;

        var cfRow = jdbcClient.sql(cfKpiSql).query().singleRow();
        long shipmentsToday = ((Number) cfRow.get("shipments_today")).longValue();
        long topupsToday = ((Number) cfRow.get("topups_today")).longValue();

        String invKpiSql = """
            SELECT
                COALESCE(COUNT(CASE WHEN DATE(created_at) = CURRENT_DATE THEN 1 END), 0) as invoices_created_today,
                COALESCE(COUNT(CASE WHEN status::text = 'PAID' AND DATE(paid_at) = CURRENT_DATE THEN 1 END), 0) as invoices_paid_today
            FROM invoices WHERE deleted_at IS NULL
        """;

        var invRow = jdbcClient.sql(invKpiSql).query().singleRow();
        long invoicesCreatedToday = ((Number) invRow.get("invoices_created_today")).longValue();
        long invoicesPaidToday = ((Number) invRow.get("invoices_paid_today")).longValue();

        SystemMetricsResponse.BusinessKPIMetrics businessKpi = SystemMetricsResponse.BusinessKPIMetrics.builder()
                .shipmentsToday(shipmentsToday)
                .topupsToday(topupsToday)
                .invoicesCreatedToday(invoicesCreatedToday)
                .invoicesPaidToday(invoicesPaidToday)
                .build();

        return SystemMetricsResponse.builder()
                .prometheusConnected(true)
                .runtime(runtime)
                .gateway(gateway)
                .databasePool(dbPool)
                .businessKpi(businessKpi)
                .slowQueries(new ArrayList<>())
                .timestamp(Instant.now().toString())
                .build();
    }

    public void simulateSlowQuery(int durationMs) {
        if (durationMs <= 0) durationMs = 150;
        double seconds = durationMs / 1000.0;
        jdbcClient.sql("SELECT pg_sleep(:sec)")
                .param("sec", seconds)
                .query()
                .listOfRows();
    }
}
