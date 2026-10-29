package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.entity.CashflowEntryHistory;
import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.repository.CashflowEntryHistoryRepository;
import com.adijayantara.cashflow.repository.CashflowEntryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashflowService {

    private final CashflowEntryRepository cashflowEntryRepository;
    private final CashflowEntryHistoryRepository cashflowEntryHistoryRepository;
    private final VendorService vendorService;
    private final CustomerService customerService;
    private final ActivityPresetService activityPresetService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;

    private static final String CACHE_PREFIX = "cache:cashflow:";

    public void invalidateCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} cache keys for prefix '{}'", keys.size(), CACHE_PREFIX);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate cache in Redis: {}", e.getMessage());
        }
    }

    @Transactional
    public CashflowEntryResponseDto recordTopUp(CreateTopUpRequest req, UserSessionDto user) {
        if (req.getKredit() == null || req.getKredit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-Up harus memiliki nilai kredit (kas masuk) yang valid lebih dari 0");
        }

        UUID actorId = user != null ? user.getUserId() : null;
        BigDecimal lastSaldo = cashflowEntryRepository.getLastSaldoForUpdate();
        BigDecimal newSaldo = lastSaldo.add(req.getKredit());

        String actInfo = (req.getActInformation() != null && !req.getActInformation().trim().isEmpty())
                ? req.getActInformation().trim()
                : "TOP UP";

        CashflowEntry entry = CashflowEntry.builder()
                .entryType(EntryType.TOP_UP)
                .kredit(req.getKredit())
                .debit(BigDecimal.ZERO)
                .saldo(newSaldo)
                .dateOfEntry(req.getDateOfEntry() != null ? req.getDateOfEntry() : LocalDate.now())
                .actInformation(actInfo)
                .actExplaination(req.getActExplaination() != null ? req.getActExplaination().trim() : "")
                .remarks(PaymentStatus.PAID)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        CashflowEntry saved = cashflowEntryRepository.save(entry);

        invalidateCache();
        notificationService.notifyLowCashBalance(saved.getSaldo(), BigDecimal.valueOf(15_000_000));
        auditService.logCashflowCreate(saved, user);

        return CashflowEntryResponseDto.fromEntity(saved);
    }

    @Transactional
    public CashflowEntryResponseDto recordInvoicePayment(Integer invoiceId, BigDecimal amount, LocalDate paymentDate, String actInfo, String actExplain, UserSessionDto user) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Pelunasan invoice harus memiliki nilai penerimaan kas (kredit) yang valid");
        }

        UUID actorId = user != null ? user.getUserId() : null;
        BigDecimal lastSaldo = cashflowEntryRepository.getLastSaldoForUpdate();
        BigDecimal newSaldo = lastSaldo.add(amount);

        CashflowEntry entry = CashflowEntry.builder()
                .entryType(EntryType.INVOICE_PAYMENT)
                .kredit(amount)
                .debit(BigDecimal.ZERO)
                .saldo(newSaldo)
                .dateOfEntry(paymentDate != null ? paymentDate : LocalDate.now())
                .actInformation(actInfo != null ? actInfo.trim() : "Pelunasan Invoice")
                .actExplaination(actExplain != null ? actExplain.trim() : "")
                .grandCost(BigDecimal.ZERO)
                .grandSelling(amount)
                .profit(BigDecimal.ZERO)
                .marginPct(BigDecimal.ZERO)
                .remarks(PaymentStatus.PAID)
                .invoiceId(invoiceId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        CashflowEntry saved = cashflowEntryRepository.save(entry);

        invalidateCache();
        auditService.logCashflowCreate(saved, user);

        return CashflowEntryResponseDto.fromEntity(saved);
    }

    @Transactional
    public CashflowEntryResponseDto recordShipment(CreateShipmentRequest req, UserSessionDto user) {
        UUID actorId = user != null ? user.getUserId() : null;

        BigDecimal grandCost = req.getGrandCost() != null ? req.getGrandCost() : BigDecimal.ZERO;
        BigDecimal debit = req.getDebit() != null ? req.getDebit() : BigDecimal.ZERO;
        BigDecimal grandSelling = req.getGrandSelling() != null ? req.getGrandSelling() : BigDecimal.ZERO;

        // Aturan Logika Bisnis: DEBIT harus sama dengan GRAND COST (HPP)
        if (grandCost.compareTo(BigDecimal.ZERO) > 0 && debit.compareTo(BigDecimal.ZERO) == 0) {
            debit = grandCost;
        } else if (debit.compareTo(BigDecimal.ZERO) > 0 && grandCost.compareTo(BigDecimal.ZERO) == 0) {
            grandCost = debit;
        } else if (debit.compareTo(grandCost) != 0) {
            throw new IllegalArgumentException("Untuk transaksi shipment, nilai debit harus sama dengan grand cost (HPP)");
        }

        // Kalkulasi Profit & Margin Pct
        BigDecimal profit = grandSelling.subtract(grandCost);
        BigDecimal marginPct = BigDecimal.ZERO;
        if (grandSelling.compareTo(BigDecimal.ZERO) > 0) {
            marginPct = profit.divide(grandSelling, 4, RoundingMode.HALF_UP);
        }

        // Auto-register vendor jika belum terdaftar
        Integer vendorId = req.getVendorId();
        if (vendorId == null && req.getVendorNameRaw() != null && !req.getVendorNameRaw().trim().isEmpty()) {
            var v = vendorService.findOrCreateVendor(req.getVendorNameRaw(), "", actorId);
            if (v != null) vendorId = v.getId();
        }

        // Auto-register customer jika belum terdaftar
        Integer customerId = req.getCustomerId();
        if (customerId == null && req.getCustomerNameRaw() != null && !req.getCustomerNameRaw().trim().isEmpty()) {
            var c = customerService.findOrCreateCustomer(req.getCustomerNameRaw(), "", "", "", actorId);
            if (c != null) customerId = c.getId();
        }

        // Auto-register activity presets
        if (req.getActInformation() != null && !req.getActInformation().trim().isEmpty()) {
            activityPresetService.findOrCreatePreset("ACT_INFO", req.getActInformation(), "", actorId);
        }
        if (req.getActExplaination() != null && !req.getActExplaination().trim().isEmpty()) {
            activityPresetService.findOrCreatePreset("ACT_EXPLAIN", req.getActExplaination(), "", actorId);
        }

        // Kalkulasi Due Date
        LocalDate entryDate = req.getDateOfEntry() != null ? req.getDateOfEntry() : LocalDate.now();
        int topDays = req.getTopDays() != null ? req.getTopDays() : 0;
        LocalDate dueDate = req.getDueDate();
        if (dueDate == null && topDays > 0) {
            dueDate = entryDate.plusDays(topDays);
        }

        PaymentStatus remarks = req.getRemarks() != null ? req.getRemarks() : PaymentStatus.UNPAID;

        // Atomically lock last row and calculate new rolling saldo
        BigDecimal lastSaldo = cashflowEntryRepository.getLastSaldoForUpdate();
        BigDecimal newSaldo = lastSaldo.subtract(debit);

        CashflowEntry entry = CashflowEntry.builder()
                .entryType(EntryType.SHIPMENT)
                .kredit(BigDecimal.ZERO)
                .debit(debit)
                .saldo(newSaldo)
                .dateOfEntry(entryDate)
                .actInformation(req.getActInformation() != null ? req.getActInformation().trim() : "")
                .actExplaination(req.getActExplaination() != null ? req.getActExplaination().trim() : "")
                .vendorId(vendorId)
                .vendorNameRaw(req.getVendorNameRaw() != null ? req.getVendorNameRaw().trim() : "")
                .customerId(customerId)
                .customerNameRaw(req.getCustomerNameRaw() != null ? req.getCustomerNameRaw().trim() : "")
                .topDays(topDays)
                .dueDate(dueDate)
                .grandCost(grandCost)
                .grandSelling(grandSelling)
                .profit(profit)
                .marginPct(marginPct)
                .remarks(remarks)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        CashflowEntry saved = cashflowEntryRepository.save(entry);

        invalidateCache();
        notificationService.notifyLowCashBalance(saved.getSaldo(), BigDecimal.valueOf(15_000_000));
        if (saved.getProfit().compareTo(BigDecimal.ZERO) < 0 || saved.getMarginPct().compareTo(BigDecimal.ZERO) < 0) {
            notificationService.notifyNegativeMargin(saved.getActInformation(), saved.getActExplaination(),
                    saved.getGrandCost(), saved.getGrandSelling(), saved.getProfit(), saved.getMarginPct());
        }
        auditService.logCashflowCreate(saved, user);

        return CashflowEntryResponseDto.fromEntity(saved);
    }

    @Transactional
    public CashflowEntryResponseDto updateEntry(Integer id, UpdateCashflowRequest req, UserSessionDto user) {
        CashflowEntry oldEntry = cashflowEntryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transaksi kas dengan ID " + id + " tidak ditemukan"));

        UUID actorId = user != null ? user.getUserId() : null;

        // 1. Simpan salinan ke tabel riwayat (history) sebelum dimodifikasi
        CashflowEntryHistory history = CashflowEntryHistory.builder()
                .entryId(oldEntry.getId())
                .sequenceNo(oldEntry.getSequenceNo())
                .entryType(oldEntry.getEntryType())
                .kredit(oldEntry.getKredit())
                .debit(oldEntry.getDebit())
                .saldo(oldEntry.getSaldo())
                .dateOfEntry(oldEntry.getDateOfEntry())
                .actInformation(oldEntry.getActInformation())
                .actExplaination(oldEntry.getActExplaination())
                .vendorId(oldEntry.getVendorId())
                .vendorNameRaw(oldEntry.getVendorNameRaw())
                .customerId(oldEntry.getCustomerId())
                .customerNameRaw(oldEntry.getCustomerNameRaw())
                .topDays(oldEntry.getTopDays())
                .dueDate(oldEntry.getDueDate())
                .grandCost(oldEntry.getGrandCost())
                .grandSelling(oldEntry.getGrandSelling())
                .profit(oldEntry.getProfit())
                .marginPct(oldEntry.getMarginPct())
                .remarks(oldEntry.getRemarks())
                .originalCreatedAt(oldEntry.getCreatedAt())
                .originalUpdatedAt(oldEntry.getUpdatedAt())
                .createdBy(oldEntry.getCreatedBy())
                .updatedBy(oldEntry.getUpdatedBy())
                .archivedBy(actorId)
                .archiveReason("manual_edit")
                .build();
        cashflowEntryHistoryRepository.save(history);

        // Tentukan nilai baru
        EntryType entryType = req.getEntryType() != null ? req.getEntryType() : oldEntry.getEntryType();
        BigDecimal newKredit = req.getKredit() != null ? req.getKredit() : oldEntry.getKredit();
        BigDecimal newDebit = req.getDebit() != null ? req.getDebit() : oldEntry.getDebit();
        BigDecimal newGrandCost = req.getGrandCost() != null ? req.getGrandCost() : oldEntry.getGrandCost();
        BigDecimal newGrandSelling = req.getGrandSelling() != null ? req.getGrandSelling() : oldEntry.getGrandSelling();

        BigDecimal profit = oldEntry.getProfit();
        BigDecimal marginPct = oldEntry.getMarginPct();

        if (entryType == EntryType.SHIPMENT) {
            if (newGrandCost.compareTo(BigDecimal.ZERO) > 0 && newDebit.compareTo(BigDecimal.ZERO) == 0) {
                newDebit = newGrandCost;
            } else if (newDebit.compareTo(BigDecimal.ZERO) > 0 && newGrandCost.compareTo(BigDecimal.ZERO) == 0) {
                newGrandCost = newDebit;
            } else if (newDebit.compareTo(newGrandCost) != 0) {
                throw new IllegalArgumentException("Untuk transaksi shipment, nilai debit harus sama dengan grand cost (HPP)");
            }

            profit = newGrandSelling.subtract(newGrandCost);
            marginPct = newGrandSelling.compareTo(BigDecimal.ZERO) > 0
                    ? profit.divide(newGrandSelling, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            newKredit = BigDecimal.ZERO;
        } else if (entryType == EntryType.TOP_UP) {
            newDebit = BigDecimal.ZERO;
            newGrandCost = BigDecimal.ZERO;
            newGrandSelling = BigDecimal.ZERO;
            profit = BigDecimal.ZERO;
            marginPct = BigDecimal.ZERO;
        }

        // Auto-register vendor/customer/preset jika diperbarui
        Integer vendorId = req.getVendorId() != null ? req.getVendorId() : oldEntry.getVendorId();
        if (req.getVendorNameRaw() != null && !req.getVendorNameRaw().trim().isEmpty()) {
            var v = vendorService.findOrCreateVendor(req.getVendorNameRaw(), "", actorId);
            if (v != null) vendorId = v.getId();
        }

        Integer customerId = req.getCustomerId() != null ? req.getCustomerId() : oldEntry.getCustomerId();
        if (req.getCustomerNameRaw() != null && !req.getCustomerNameRaw().trim().isEmpty()) {
            var c = customerService.findOrCreateCustomer(req.getCustomerNameRaw(), "", "", "", actorId);
            if (c != null) customerId = c.getId();
        }

        if (req.getActInformation() != null && !req.getActInformation().trim().isEmpty()) {
            activityPresetService.findOrCreatePreset("ACT_INFO", req.getActInformation(), "", actorId);
        }
        if (req.getActExplaination() != null && !req.getActExplaination().trim().isEmpty()) {
            activityPresetService.findOrCreatePreset("ACT_EXPLAIN", req.getActExplaination(), "", actorId);
        }

        // Hitung selisih pergeseran saldo berjalan (cascading balance)
        BigDecimal oldNet = oldEntry.getKredit().subtract(oldEntry.getDebit());
        BigDecimal newNet = newKredit.subtract(newDebit);
        BigDecimal diff = newNet.subtract(oldNet);

        oldEntry.setEntryType(entryType);
        oldEntry.setKredit(newKredit);
        oldEntry.setDebit(newDebit);
        oldEntry.setGrandCost(newGrandCost);
        oldEntry.setGrandSelling(newGrandSelling);
        oldEntry.setProfit(profit);
        oldEntry.setMarginPct(marginPct);
        oldEntry.setSaldo(oldEntry.getSaldo().add(diff));

        if (req.getDateOfEntry() != null) oldEntry.setDateOfEntry(req.getDateOfEntry());
        if (req.getActInformation() != null) oldEntry.setActInformation(req.getActInformation().trim());
        if (req.getActExplaination() != null) oldEntry.setActExplaination(req.getActExplaination().trim());
        if (req.getRemarks() != null) oldEntry.setRemarks(req.getRemarks());
        if (vendorId != null) oldEntry.setVendorId(vendorId);
        if (req.getVendorNameRaw() != null) oldEntry.setVendorNameRaw(req.getVendorNameRaw().trim());
        if (customerId != null) oldEntry.setCustomerId(customerId);
        if (req.getCustomerNameRaw() != null) oldEntry.setCustomerNameRaw(req.getCustomerNameRaw().trim());
        if (req.getTopDays() != null) oldEntry.setTopDays(req.getTopDays());
        if (req.getDueDate() != null) oldEntry.setDueDate(req.getDueDate());
        oldEntry.setUpdatedBy(actorId);

        CashflowEntry updated = cashflowEntryRepository.save(oldEntry);

        // Jika ada perubahan neto, perbarui baris-baris setelahnya
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            cashflowEntryRepository.updateBalancesAfter(oldEntry.getSequenceNo(), diff);
        }

        invalidateCache();
        auditService.logCashflowUpdate(oldEntry, updated, user);

        return CashflowEntryResponseDto.fromEntity(updated);
    }

    @Transactional
    public void deleteEntry(Integer id, UserSessionDto user) {
        CashflowEntry oldEntry = cashflowEntryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transaksi kas dengan ID " + id + " tidak ditemukan"));

        UUID actorId = user != null ? user.getUserId() : null;

        // Arsipkan ke tabel history sebelum dihapus
        CashflowEntryHistory history = CashflowEntryHistory.builder()
                .entryId(oldEntry.getId())
                .sequenceNo(oldEntry.getSequenceNo())
                .entryType(oldEntry.getEntryType())
                .kredit(oldEntry.getKredit())
                .debit(oldEntry.getDebit())
                .saldo(oldEntry.getSaldo())
                .dateOfEntry(oldEntry.getDateOfEntry())
                .actInformation(oldEntry.getActInformation())
                .actExplaination(oldEntry.getActExplaination())
                .vendorId(oldEntry.getVendorId())
                .vendorNameRaw(oldEntry.getVendorNameRaw())
                .customerId(oldEntry.getCustomerId())
                .customerNameRaw(oldEntry.getCustomerNameRaw())
                .topDays(oldEntry.getTopDays())
                .dueDate(oldEntry.getDueDate())
                .grandCost(oldEntry.getGrandCost())
                .grandSelling(oldEntry.getGrandSelling())
                .profit(oldEntry.getProfit())
                .marginPct(oldEntry.getMarginPct())
                .remarks(oldEntry.getRemarks())
                .originalCreatedAt(oldEntry.getCreatedAt())
                .originalUpdatedAt(oldEntry.getUpdatedAt())
                .createdBy(oldEntry.getCreatedBy())
                .updatedBy(oldEntry.getUpdatedBy())
                .archivedBy(actorId)
                .archiveReason("manual_delete")
                .build();
        cashflowEntryHistoryRepository.save(history);

        // Selisih untuk memundurkan saldo baris sesudahnya
        BigDecimal diff = oldEntry.getDebit().subtract(oldEntry.getKredit()); // -(kredit - debit)

        cashflowEntryRepository.delete(oldEntry);

        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            cashflowEntryRepository.updateBalancesAfter(oldEntry.getSequenceNo(), diff);
        }

        invalidateCache();
        auditService.logCashflowDelete(oldEntry, user);
    }

    @Transactional
    public void updatePaymentStatus(Integer id, PaymentStatus remarks, UserSessionDto user) {
        UUID actorId = user != null ? user.getUserId() : null;
        int updated = cashflowEntryRepository.updateRemarks(id, remarks, actorId);
        if (updated == 0) {
            throw new NoSuchElementException("Transaksi kas dengan ID " + id + " tidak ditemukan");
        }
        invalidateCache();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(CashflowFilterRequest filter) {
        int page = filter.getPage();
        int limit = filter.getLimit();
        String sortDir = filter.getSort();

        String cacheKey = String.format("%sp:%d:l:%d:df:%s:dt:%s:et:%s:rem:%s:vn:%s:sort:%s",
                CACHE_PREFIX, page, limit,
                filter.getDate_from(), filter.getDate_to(),
                filter.getEntry_type(), filter.getRemarks(),
                filter.getVendor_name(), sortDir);

        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", cacheKey, e.getMessage());
        }

        Specification<CashflowEntry> spec = buildSpecification(filter);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(Sort.Direction.ASC, "sequenceNo")
                : Sort.by(Sort.Direction.DESC, "sequenceNo");

        Page<CashflowEntry> entryPage = cashflowEntryRepository.findAll(spec, PageRequest.of(page - 1, limit, sort));

        List<CashflowEntryResponseDto> dtos = entryPage.getContent().stream()
                .map(CashflowEntryResponseDto::fromEntity)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", dtos);
        result.put("total", entryPage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        result.put("total_pages", entryPage.getTotalPages());

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for {}: {}", cacheKey, e.getMessage());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public CashflowSummaryDto getSummary(CashflowFilterRequest filter) {
        String cacheKey = String.format("%ssum:df:%s:dt:%s:et:%s:rem:%s:vn:%s",
                CACHE_PREFIX, filter.getDate_from(), filter.getDate_to(),
                filter.getEntry_type(), filter.getRemarks(), filter.getVendor_name());

        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, CashflowSummaryDto.class);
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", cacheKey, e.getMessage());
        }

        BigDecimal currentSaldo = cashflowEntryRepository.getCurrentSaldo();

        // Build dynamic WHERE clause
        StringBuilder wherePeriodic = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (filter.getDate_from() != null) {
            wherePeriodic.append(" AND date_of_entry >= :dateFrom ");
            params.put("dateFrom", filter.getDate_from());
        }
        if (filter.getDate_to() != null) {
            wherePeriodic.append(" AND date_of_entry <= :dateTo ");
            params.put("dateTo", filter.getDate_to());
        }

        String sqlMain = """
            SELECT 
                COALESCE(SUM(kredit), 0) AS total_kredit,
                COALESCE(SUM(debit), 0) AS total_debit,
                COALESCE(SUM(CASE WHEN entry_type = 'SHIPMENT' THEN profit ELSE 0 END), 0) AS total_profit,
                COALESCE(AVG(CASE WHEN entry_type = 'SHIPMENT' AND grand_selling > 0 THEN margin_pct ELSE NULL END), 0) AS avg_margin
            FROM cashflow_entries
        """ + wherePeriodic;

        var mainQuery = jdbcClient.sql(sqlMain);
        params.forEach(mainQuery::param);

        Map<String, Object> mainRow = mainQuery.query().singleRow();
        BigDecimal totalKredit = (BigDecimal) mainRow.get("total_kredit");
        BigDecimal totalDebit = (BigDecimal) mainRow.get("total_debit");
        BigDecimal totalProfit = (BigDecimal) mainRow.get("total_profit");
        BigDecimal avgMargin = (BigDecimal) mainRow.get("avg_margin");

        // Unpaid metrics (remarks = 'UNPAID' AND entry_type = 'SHIPMENT')
        String sqlUnpaid = """
            SELECT 
                COUNT(*) AS unpaid_count,
                COALESCE(SUM(debit), 0) AS unpaid_amount
            FROM cashflow_entries
        """ + wherePeriodic + " AND remarks = 'UNPAID' AND entry_type = 'SHIPMENT'";

        var unpaidQuery = jdbcClient.sql(sqlUnpaid);
        params.forEach(unpaidQuery::param);

        Map<String, Object> unpaidRow = unpaidQuery.query().singleRow();
        Long unpaidCount = ((Number) unpaidRow.get("unpaid_count")).longValue();
        BigDecimal unpaidAmount = (BigDecimal) unpaidRow.get("unpaid_amount");

        CashflowSummaryDto summary = CashflowSummaryDto.builder()
                .currentSaldo(currentSaldo)
                .totalKredit(totalKredit != null ? totalKredit : BigDecimal.ZERO)
                .totalDebit(totalDebit != null ? totalDebit : BigDecimal.ZERO)
                .totalProfit(totalProfit != null ? totalProfit : BigDecimal.ZERO)
                .avgMarginPct(avgMargin != null ? avgMargin : BigDecimal.ZERO)
                .unpaidCount(unpaidCount != null ? unpaidCount : 0L)
                .unpaidAmount(unpaidAmount != null ? unpaidAmount : BigDecimal.ZERO)
                .build();

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(summary), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for {}: {}", cacheKey, e.getMessage());
        }

        return summary;
    }

    @Transactional(readOnly = true)
    public List<AvailablePeriodDto> getAvailablePeriods() {
        List<Object[]> rows = cashflowEntryRepository.findAvailablePeriodsRaw();
        List<AvailablePeriodDto> list = new ArrayList<>();
        for (Object[] r : rows) {
            String monthKey = (String) r[0];
            Integer year = ((Number) r[1]).intValue();
            Integer month = ((Number) r[2]).intValue();
            Long totalCount = ((Number) r[3]).longValue();

            list.add(AvailablePeriodDto.builder()
                    .monthKey(monthKey)
                    .year(year)
                    .month(month)
                    .totalCount(totalCount)
                    .build());
        }
        return list;
    }

    public Specification<CashflowEntry> buildSpecification(CashflowFilterRequest filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return cb.conjunction();
            }

            if (filter.getDate_from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfEntry"), filter.getDate_from()));
            }
            if (filter.getDate_to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfEntry"), filter.getDate_to()));
            }
            if (filter.getEntry_type() != null) {
                predicates.add(cb.equal(root.get("entryType"), filter.getEntry_type()));
            }
            if (filter.getRemarks() != null) {
                predicates.add(cb.equal(root.get("remarks"), filter.getRemarks()));
            }
            if (filter.getVendor_name() != null && !filter.getVendor_name().trim().isEmpty()) {
                String likePattern = "%" + filter.getVendor_name().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("vendorNameRaw")), likePattern));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

