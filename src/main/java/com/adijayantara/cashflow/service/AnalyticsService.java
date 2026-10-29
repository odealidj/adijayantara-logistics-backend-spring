package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcClient jdbcClient;

    // ---------------------------------------------------------------------
    // 1. Proyeksi Arus Kas Runway (P1) & CCC Gap (P6)
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public CashflowRunwayResponse getCashflowRunway(Integer daysParam) {
        int days = (daysParam == null || daysParam <= 0 || daysParam > 90) ? 60 : daysParam;
        double minBuffer = 25000000.0; // Rp 25 Juta ambang kas aman

        // 1. Dapatkan saldo kas riil terkini
        Double currentBalance = jdbcClient.sql("SELECT COALESCE((SELECT saldo FROM cashflow_entries ORDER BY sequence_no DESC LIMIT 1), 0)")
                .query(Double.class)
                .single();
        if (currentBalance == null) currentBalance = 0.0;

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        // 2. Query Piutang Invoice belum dibayar
        List<Map<String, Object>> inflows = jdbcClient.sql("""
            SELECT due_date, SUM(amount) as amount
            FROM invoices
            WHERE deleted_at IS NULL AND status != 'PAID'
            GROUP BY due_date
            ORDER BY due_date ASC
            """).query().listOfRows();

        // 3. Query Hutang Vendor belum dibayar
        List<Map<String, Object>> outflows = jdbcClient.sql("""
            SELECT COALESCE(due_date, date_of_entry) as due_date, SUM(grand_cost) as amount
            FROM cashflow_entries
            WHERE remarks = 'UNPAID' AND entry_type = 'SHIPMENT'
            GROUP BY COALESCE(due_date, date_of_entry)
            ORDER BY due_date ASC
            """).query().listOfRows();

        // 4. CCC Gap (Customer DSO & Vendor DPO)
        Double avgCustDSO = jdbcClient.sql("""
            SELECT COALESCE(AVG(CASE 
                WHEN status = 'PAID' AND paid_at IS NOT NULL THEN (paid_at::date - shipment_date)
                ELSE top_days 
            END), 30)
            FROM invoices
            WHERE deleted_at IS NULL
            """).query(Double.class).single();
        if (avgCustDSO == null) avgCustDSO = 30.0;

        Double avgVendDPO = jdbcClient.sql("""
            SELECT COALESCE(AVG(top_days), 14)
            FROM cashflow_entries
            WHERE entry_type = 'SHIPMENT' AND top_days > 0
            """).query(Double.class).single();
        if (avgVendDPO == null) avgVendDPO = 14.0;

        double dso = Math.round(avgCustDSO * 10.0) / 10.0;
        double dpo = Math.round(avgVendDPO * 10.0) / 10.0;
        double financingGap = Math.round((dso - dpo) * 10.0) / 10.0;

        // Petakan ke map per tanggal
        Map<String, Double> inflowMap = new HashMap<>();
        double totalInflow = 0.0;
        for (Map<String, Object> inf : inflows) {
            LocalDate d = null;
            Object dObj = inf.get("due_date");
            if (dObj instanceof java.sql.Date sd) {
                d = sd.toLocalDate();
            } else if (dObj instanceof LocalDate ld) {
                d = ld;
            }
            double amt = ((Number) inf.get("amount")).doubleValue();
            totalInflow += amt;
            String dStr = (d != null && !d.isBefore(today)) ? d.toString() : today.toString();
            inflowMap.merge(dStr, amt, Double::sum);
        }

        Map<String, Double> outflowMap = new HashMap<>();
        double totalOutflow = 0.0;
        for (Map<String, Object> out : outflows) {
            LocalDate d = null;
            Object dObj = out.get("due_date");
            if (dObj instanceof java.sql.Date sd) {
                d = sd.toLocalDate();
            } else if (dObj instanceof LocalDate ld) {
                d = ld;
            }
            double amt = ((Number) out.get("amount")).doubleValue();
            totalOutflow += amt;
            String dStr = (d != null && !d.isBefore(today)) ? d.toString() : today.toString();
            outflowMap.merge(dStr, amt, Double::sum);
        }

        // 5. Bangun proyeksi bergulir 8-9 minggu
        double runningCash = currentBalance;
        double lowestPoint = runningCash;
        String lowestDate = today.toString();

        List<CashflowRunwayResponse.RunwayBucket> buckets = new ArrayList<>();
        LocalDate currentBucketDate = today;

        for (int i = 0; i <= days; i += 7) {
            LocalDate nextBucketDate = currentBucketDate.plusDays(7);
            if (nextBucketDate.isAfter(endDate)) {
                nextBucketDate = endDate;
            }

            double weekInflow = 0.0;
            double weekOutflow = 0.0;

            for (LocalDate d = currentBucketDate; !d.isAfter(nextBucketDate); d = d.plusDays(1)) {
                String dKey = d.toString();
                weekInflow += inflowMap.getOrDefault(dKey, 0.0);
                weekOutflow += outflowMap.getOrDefault(dKey, 0.0);
            }

            double net = weekInflow - weekOutflow;
            runningCash += net;

            if (runningCash < lowestPoint) {
                lowestPoint = runningCash;
                lowestDate = nextBucketDate.toString();
            }

            int weekIdx = (i / 7) + 1;
            String weekLabel = (i == 0) ? "Hari Ini" : "Mgg " + weekIdx;

            buckets.add(CashflowRunwayResponse.RunwayBucket.builder()
                    .date(nextBucketDate.toString())
                    .weekLabel(weekLabel)
                    .dayOffset(i + 7)
                    .inflowAmount(weekInflow)
                    .outflowAmount(weekOutflow)
                    .netChange(net)
                    .projectedCash(runningCash)
                    .isInDanger(runningCash < minBuffer)
                    .build());

            currentBucketDate = nextBucketDate.plusDays(1);
            if (currentBucketDate.isAfter(endDate)) {
                break;
            }
        }

        return CashflowRunwayResponse.builder()
                .currentCashBalance(currentBalance)
                .minimumCashBuffer(minBuffer)
                .totalPendingInflow(totalInflow)
                .totalPendingOutflow(totalOutflow)
                .netProjected60Days(runningCash - currentBalance)
                .lowestPointDate(lowestDate)
                .lowestPointBalance(lowestPoint)
                .runwayDays(days)
                .buckets(buckets)
                .avgCustomerDso(dso)
                .avgVendorDpo(dpo)
                .financingGapDays(financingGap)
                .build();
    }

    // ---------------------------------------------------------------------
    // 2. Matriks Kuadran Profitabilitas Rute BCG (P2)
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public RouteMatrixResponse getRouteMatrix(LocalDate dateFrom, LocalDate dateTo) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        conditions.add("entry_type = 'SHIPMENT'");

        if (dateFrom != null) {
            conditions.add("date_of_entry >= :dateFrom");
            params.put("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            conditions.add("date_of_entry <= :dateTo");
            params.put("dateTo", dateTo);
        }

        String sql = "SELECT " +
            "COALESCE(NULLIF(TRIM(act_explaination), ''), NULLIF(TRIM(act_information), ''), 'Rute Operasional') as route_name, " +
            "COUNT(*) as trip_count, " +
            "COALESCE(SUM(grand_selling), 0) as total_revenue, " +
            "COALESCE(SUM(grand_cost), 0) as total_cost, " +
            "COALESCE(SUM(profit), 0) as total_profit, " +
            "COALESCE(AVG(CASE WHEN grand_selling > 0 THEN margin_pct * 100 ELSE 0 END), 0) as avg_margin_pct " +
            "FROM cashflow_entries WHERE " + String.join(" AND ", conditions) + " " +
            "GROUP BY route_name ORDER BY total_profit DESC";

        var client = jdbcClient.sql(sql);
        params.forEach(client::param);

        List<Map<String, Object>> rows = client.query().listOfRows();
        if (rows.isEmpty()) {
            return RouteMatrixResponse.builder()
                    .totalRoutes(0)
                    .avgTripThreshold(0.0)
                    .avgMarginThreshold(12.0)
                    .routes(Collections.emptyList())
                    .build();
        }

        int sumTrips = 0;
        double sumMargin = 0.0;
        for (Map<String, Object> r : rows) {
            sumTrips += ((Number) r.get("trip_count")).intValue();
            sumMargin += ((Number) r.get("avg_margin_pct")).doubleValue();
        }

        double avgTrip = (double) sumTrips / rows.size();
        double avgMargin = sumMargin / rows.size();
        if (avgMargin < 12.0) avgMargin = 12.0;

        List<RouteMatrixResponse.RouteMatrixItem> routes = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String routeName = (String) r.get("route_name");
            int tripCount = ((Number) r.get("trip_count")).intValue();
            double totalRevenue = ((Number) r.get("total_revenue")).doubleValue();
            double totalCost = ((Number) r.get("total_cost")).doubleValue();
            double totalProfit = ((Number) r.get("total_profit")).doubleValue();
            double marginPct = ((Number) r.get("avg_margin_pct")).doubleValue();

            boolean isHighTrip = (double) tripCount >= avgTrip;
            boolean isHighMargin = marginPct >= avgMargin;

            String quad, quadName;
            if (isHighTrip && isHighMargin) {
                quad = "STAR";
                quadName = "Kuadran I: Bintang (High Volume, High Margin)";
            } else if (!isHighTrip && isHighMargin) {
                quad = "OPPORTUNITY";
                quadName = "Kuadran II: Potensial (Low Volume, High Margin)";
            } else if (isHighTrip && !isHighMargin) {
                quad = "CASH_COW";
                quadName = "Kuadran III: Sapi Perah (High Volume, Low Margin)";
            } else {
                quad = "EVALUATE";
                quadName = "Kuadran IV: Evaluasi Kritis (Low Volume, Low Margin)";
            }

            routes.add(RouteMatrixResponse.RouteMatrixItem.builder()
                    .routeName(routeName)
                    .tripCount(tripCount)
                    .totalRevenue(totalRevenue)
                    .totalCost(totalCost)
                    .totalProfit(totalProfit)
                    .avgMarginPct(Math.round(marginPct * 10.0) / 10.0)
                    .quadrant(quad)
                    .quadrantName(quadName)
                    .build());
        }

        return RouteMatrixResponse.builder()
                .totalRoutes(rows.size())
                .avgTripThreshold(Math.round(avgTrip * 10.0) / 10.0)
                .avgMarginThreshold(Math.round(avgMargin * 10.0) / 10.0)
                .routes(routes)
                .build();
    }

    // ---------------------------------------------------------------------
    // 3. Kepatuhan DSO Customer (P3) & Konsentrasi Pareto 80/20 (P5)
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public CustomerDisciplineAndParetoResponse getCustomerDisciplineAndPareto(LocalDate dateFrom, LocalDate dateTo) {
        String sql = """
            SELECT 
                i.client_name,
                COUNT(DISTINCT i.id) as total_invoices,
                COALESCE(SUM(i.amount), 0) as total_billed,
                COALESCE(AVG(i.top_days), 30)::int as avg_top_days,
                COALESCE(AVG(CASE 
                    WHEN i.status = 'PAID' AND i.paid_at IS NOT NULL THEN (i.paid_at::date - i.shipment_date)
                    WHEN i.status = 'OVERDUE' THEN i.top_days + (CURRENT_DATE - i.due_date)
                    ELSE i.top_days 
                END), 30) as avg_actual_dso,
                COUNT(DISTINCT CASE WHEN i.status = 'OVERDUE' THEN i.id END) as overdue_count,
                COALESCE(COUNT(DISTINCT h.id), 0) as reschedule_count
            FROM invoices i
            LEFT JOIN invoice_due_date_history h ON i.id = h.invoice_id
            WHERE i.deleted_at IS NULL
            GROUP BY i.client_name
            ORDER BY total_billed DESC
            """;

        List<Map<String, Object>> rows = jdbcClient.sql(sql).query().listOfRows();

        // Churn query
        String tripSql = """
            SELECT 
                act_information as client_name,
                COUNT(CASE WHEN date_of_entry >= CURRENT_DATE - INTERVAL '30 days' THEN 1 END) as recent_trips,
                COUNT(CASE WHEN date_of_entry >= CURRENT_DATE - INTERVAL '60 days' AND date_of_entry < CURRENT_DATE - INTERVAL '30 days' THEN 1 END) as prev_trips
            FROM cashflow_entries
            WHERE entry_type = 'SHIPMENT' AND act_information IS NOT NULL AND TRIM(act_information) != ''
            GROUP BY act_information
            """;

        List<Map<String, Object>> tripRows = jdbcClient.sql(tripSql).query().listOfRows();
        Map<String, int[]> tripMap = new HashMap<>();
        for (Map<String, Object> tr : tripRows) {
            String client = (String) tr.get("client_name");
            int rec = ((Number) tr.get("recent_trips")).intValue();
            int prev = ((Number) tr.get("prev_trips")).intValue();
            tripMap.put(client, new int[]{rec, prev});
        }

        double totalRevenue = 0.0;
        for (Map<String, Object> r : rows) {
            totalRevenue += ((Number) r.get("total_billed")).doubleValue();
        }

        double cumulative = 0.0;
        double totalDso = 0.0;
        int top80Count = 0;
        int highRiskCount = 0;
        int churnCount = 0;

        List<CustomerDisciplineAndParetoResponse.CustomerDisciplineItem> customers = new ArrayList<>();

        for (Map<String, Object> r : rows) {
            String clientName = (String) r.get("client_name");
            int totalInvoices = ((Number) r.get("total_invoices")).intValue();
            double billed = ((Number) r.get("total_billed")).doubleValue();
            int avgTop = ((Number) r.get("avg_top_days")).intValue();
            double actualDso = ((Number) r.get("avg_actual_dso")).doubleValue();
            int overdue = ((Number) r.get("overdue_count")).intValue();
            int reschedule = ((Number) r.get("reschedule_count")).intValue();

            double pct = (totalRevenue > 0) ? (billed / totalRevenue) * 100 : 0.0;
            cumulative += pct;
            totalDso += actualDso;

            if (cumulative <= 80.0) {
                top80Count++;
            }

            String status;
            if (overdue > 0 || reschedule >= 2 || actualDso > (avgTop + 10)) {
                status = "HIGH_RISK";
                highRiskCount++;
            } else if (actualDso > avgTop) {
                status = "MODERATE";
            } else {
                status = "PRIME";
            }

            int[] trips = tripMap.getOrDefault(clientName, new int[]{0, 0});
            int recentTrips = trips[0];
            int prevTrips = trips[1];
            double changePct = 0.0;
            boolean isChurn = false;
            if (prevTrips > 0) {
                changePct = (((double) (recentTrips - prevTrips)) / prevTrips) * 100.0;
                if (changePct <= -40.0) {
                    isChurn = true;
                    churnCount++;
                }
            }

            customers.add(CustomerDisciplineAndParetoResponse.CustomerDisciplineItem.builder()
                    .clientName(clientName)
                    .totalInvoices(totalInvoices)
                    .totalBilled(billed)
                    .avgTopDays(avgTop)
                    .avgActualDso(Math.round(actualDso * 10.0) / 10.0)
                    .overdueCount(overdue)
                    .rescheduleCount(reschedule)
                    .disciplineStatus(status)
                    .paretoPercent(Math.round(pct * 10.0) / 10.0)
                    .cumulativePercent(Math.round(cumulative * 10.0) / 10.0)
                    .recentTripsCount(recentTrips)
                    .previousTripsCount(prevTrips)
                    .tripChangePct(Math.round(changePct * 10.0) / 10.0)
                    .isChurnWarning(isChurn)
                    .build());
        }

        if (top80Count == 0 && !rows.isEmpty()) {
            top80Count = 1;
        }

        double overallDso = rows.isEmpty() ? 0.0 : Math.round((totalDso / rows.size()) * 10.0) / 10.0;

        return CustomerDisciplineAndParetoResponse.builder()
                .totalClients(rows.size())
                .totalRevenue(totalRevenue)
                .top80PercentCount(top80Count)
                .customers(customers)
                .overallAvgDso(overallDso)
                .highRiskClientCount(highRiskCount)
                .churnAlertCount(churnCount)
                .build();
    }

    // ---------------------------------------------------------------------
    // 4. Analisis Efisiensi & Ketergantungan Rekanan Vendor (P4)
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public VendorEfficiencyResponse getVendorEfficiency(LocalDate dateFrom, LocalDate dateTo) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        conditions.add("entry_type = 'SHIPMENT'");

        if (dateFrom != null) {
            conditions.add("date_of_entry >= :dateFrom");
            params.put("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            conditions.add("date_of_entry <= :dateTo");
            params.put("dateTo", dateTo);
        }

        String sql = "SELECT " +
            "COALESCE(NULLIF(TRIM(vendor_name_raw), ''), 'Vendor Rekanan Operasional') as vendor_name, " +
            "COUNT(*) as trip_count, " +
            "COALESCE(SUM(grand_cost), 0) as total_cost, " +
            "COALESCE(SUM(grand_selling), 0) as total_selling, " +
            "COALESCE(SUM(profit), 0) as total_profit, " +
            "COALESCE(AVG(CASE WHEN grand_selling > 0 THEN margin_pct * 100 ELSE 0 END), 0) as avg_margin_pct " +
            "FROM cashflow_entries WHERE " + String.join(" AND ", conditions) + " " +
            "GROUP BY vendor_name ORDER BY total_cost DESC";

        var client = jdbcClient.sql(sql);
        params.forEach(client::param);

        List<Map<String, Object>> rows = client.query().listOfRows();

        int totalTrips = 0;
        double totalCost = 0.0;
        double maxMargin = 0.0;
        String bestMarginVendor = "";

        for (Map<String, Object> r : rows) {
            int tc = ((Number) r.get("trip_count")).intValue();
            double cost = ((Number) r.get("total_cost")).doubleValue();
            double margin = ((Number) r.get("avg_margin_pct")).doubleValue();
            String name = (String) r.get("vendor_name");

            totalTrips += tc;
            totalCost += cost;

            if (margin > maxMargin && tc >= 2) {
                maxMargin = margin;
                bestMarginVendor = name;
            }
        }

        String topVendor = "";
        double topConcentration = 0.0;
        List<VendorEfficiencyResponse.VendorEfficiencyItem> list = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> r = rows.get(i);
            String name = (String) r.get("vendor_name");
            int tc = ((Number) r.get("trip_count")).intValue();
            double cost = ((Number) r.get("total_cost")).doubleValue();
            double sell = ((Number) r.get("total_selling")).doubleValue();
            double profit = ((Number) r.get("total_profit")).doubleValue();
            double margin = ((Number) r.get("avg_margin_pct")).doubleValue();

            double ratio = (totalTrips > 0) ? (((double) tc) / totalTrips) * 100.0 : 0.0;

            if (i == 0) {
                topVendor = name;
                topConcentration = Math.round(ratio * 10.0) / 10.0;
            }

            list.add(VendorEfficiencyResponse.VendorEfficiencyItem.builder()
                    .vendorName(name)
                    .tripCount(tc)
                    .totalCost(cost)
                    .totalSelling(sell)
                    .totalProfit(profit)
                    .avgMarginPct(Math.round(margin * 10.0) / 10.0)
                    .concentrationRatio(Math.round(ratio * 10.0) / 10.0)
                    .isDominant(ratio >= 35.0)
                    .build());
        }

        return VendorEfficiencyResponse.builder()
                .totalVendors(rows.size())
                .totalTripsAll(totalTrips)
                .totalCostAll(totalCost)
                .topVendorName(topVendor)
                .topConcentrationPct(topConcentration)
                .highestMarginVendor(bestMarginVendor)
                .highestMarginPct(Math.round(maxMargin * 10.0) / 10.0)
                .vendors(list)
                .build();
    }
}
