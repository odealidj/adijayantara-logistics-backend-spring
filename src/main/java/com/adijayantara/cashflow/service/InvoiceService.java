package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Invoice;
import com.adijayantara.cashflow.domain.entity.InvoiceDueDateHistory;
import com.adijayantara.cashflow.domain.entity.InvoicePaymentHistory;
import com.adijayantara.cashflow.domain.entity.User;
import com.adijayantara.cashflow.domain.enums.InvoiceStatus;
import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.repository.InvoiceDueDateHistoryRepository;
import com.adijayantara.cashflow.repository.InvoicePaymentHistoryRepository;
import com.adijayantara.cashflow.repository.InvoiceRepository;
import com.adijayantara.cashflow.repository.UserRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDueDateHistoryRepository dueDateHistoryRepository;
    private final InvoicePaymentHistoryRepository paymentHistoryRepository;
    private final UserRepository userRepository;
    private final CashflowService cashflowService;
    private final AuditService auditService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcClient jdbcClient;

    private static final String CACHE_PREFIX = "cache:invoices:";

    public void invalidateCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Invalidated {} cache keys for prefix '{}'", keys.size(), CACHE_PREFIX);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate invoice cache in Redis: {}", e.getMessage());
        }
    }

    @Transactional
    public InvoiceResponseDto createInvoice(CreateInvoiceRequest req, UserSessionDto user) {
        String invNo = req.getInvoiceNo().trim();
        if (invoiceRepository.existsByInvoiceNoAndDeletedAtIsNull(invNo)) {
            throw new IllegalArgumentException("Invoice dengan nomor '" + invNo + "' sudah terdaftar");
        }

        UUID creatorId = user != null ? user.getUserId() : null;
        String creatorName = user != null && user.getFullName() != null ? user.getFullName() : "Sistem";

        int topDays = req.getTopDays() != null ? req.getTopDays() : 0;
        LocalDate shipmentDate = req.getShipmentDate() != null ? req.getShipmentDate() : LocalDate.now();
        LocalDate dueDate = shipmentDate.plusDays(topDays);

        String terms = req.getTopTerms();
        if (terms == null || terms.trim().isEmpty()) {
            terms = (topDays == 0) ? "COD (Cash on Delivery)" : String.format("Net %d Hari", topDays);
        }

        InvoiceStatus status = InvoiceStatus.UNPAID;
        LocalDateTime paidAt = null;

        if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
            try {
                status = InvoiceStatus.valueOf(req.getStatus().trim().toUpperCase());
                if (status == InvoiceStatus.PAID) {
                    paidAt = LocalDateTime.now();
                }
            } catch (IllegalArgumentException ignored) {
            }
        } else if (LocalDate.now().isAfter(dueDate)) {
            status = InvoiceStatus.OVERDUE;
        }

        Invoice invoice = Invoice.builder()
                .invoiceNo(invNo)
                .clientName(req.getClientName().trim())
                .shipmentDate(shipmentDate)
                .topTerms(terms)
                .topDays(topDays)
                .dueDate(dueDate)
                .originalDueDate(dueDate)
                .amount(req.getAmount())
                .status(status)
                .paidAt(paidAt)
                .notes(req.getNotes() != null ? req.getNotes().trim() : "")
                .cashflowEntryId(req.getCashflowEntryId())
                .createdBy(creatorId)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        invalidateCache();
        auditService.logInvoiceCreate(saved, user);

        return InvoiceResponseDto.fromEntity(saved, 0, creatorName, "");
    }

    @Transactional
    public InvoiceResponseDto updateInvoice(Integer id, CreateInvoiceRequest req, UserSessionDto user) {
        Invoice invoice = invoiceRepository.findById(id)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Invoice dengan ID " + id + " tidak ditemukan"));

        if (req.getClientName() != null && !req.getClientName().trim().isEmpty()) {
            invoice.setClientName(req.getClientName().trim());
        }
        if (req.getAmount() != null && req.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setAmount(req.getAmount());
        }
        if (req.getNotes() != null) {
            invoice.setNotes(req.getNotes().trim());
        }
        if (req.getShipmentDate() != null) {
            invoice.setShipmentDate(req.getShipmentDate());
        }
        if (req.getTopDays() != null) {
            invoice.setTopDays(req.getTopDays());
            invoice.setDueDate(invoice.getShipmentDate().plusDays(req.getTopDays()));
        }
        if (req.getTopTerms() != null && !req.getTopTerms().trim().isEmpty()) {
            invoice.setTopTerms(req.getTopTerms().trim());
        } else if (req.getTopDays() != null) {
            invoice.setTopTerms(req.getTopDays() == 0 ? "COD (Cash on Delivery)" : String.format("Net %d Hari", req.getTopDays()));
        }

        if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
            try {
                InvoiceStatus newStatus = InvoiceStatus.valueOf(req.getStatus().trim().toUpperCase());
                invoice.setStatus(newStatus);
                if (newStatus == InvoiceStatus.PAID && invoice.getPaidAt() == null) {
                    invoice.setPaidAt(LocalDateTime.now());
                } else if (newStatus != InvoiceStatus.PAID) {
                    invoice.setPaidAt(null);
                }
            } catch (IllegalArgumentException ignored) {
            }
        } else if (invoice.getStatus() != InvoiceStatus.PAID) {
            if (LocalDate.now().isAfter(invoice.getDueDate())) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
            } else {
                invoice.setStatus(InvoiceStatus.UNPAID);
            }
        }

        Invoice updated = invoiceRepository.save(invoice);
        invalidateCache();

        int rescheduleCount = dueDateHistoryRepository.countByInvoiceId(id);
        String creatorName = resolveUserName(updated.getCreatedBy());
        String paidByName = resolveUserName(updated.getPaidBy());

        return InvoiceResponseDto.fromEntity(updated, rescheduleCount, creatorName, paidByName);
    }

    @Transactional
    public InvoiceResponseDto settleInvoice(Integer id, SettleInvoiceRequest req, UserSessionDto user) {
        Invoice invoice = invoiceRepository.findById(id)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Invoice dengan ID " + id + " tidak ditemukan"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice ini sudah berstatus lunas");
        }

        UUID actorId = user != null ? user.getUserId() : null;
        String actorName = user != null && user.getFullName() != null ? user.getFullName() : "Staf Finance";

        LocalDate payDate = req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now();

        // 1. Update Invoice status
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(payDate.atStartOfDay());
        invoice.setPaidBy(actorId);
        invoice.setPaymentReference(req.getReferenceNo() != null ? req.getReferenceNo().trim() : "");
        invoice.setPaymentProofUrl(req.getProofUrl() != null ? req.getProofUrl().trim() : "");
        invoice.setPaymentNotes(req.getNotes() != null ? req.getNotes().trim() : "");

        Invoice saved = invoiceRepository.save(invoice);

        // 2. Insert into invoice_payment_history
        InvoicePaymentHistory payment = InvoicePaymentHistory.builder()
                .invoiceId(id)
                .action("SETTLED")
                .amount(saved.getAmount())
                .paymentDate(payDate.atStartOfDay())
                .referenceNo(req.getReferenceNo() != null ? req.getReferenceNo().trim() : "")
                .proofUrl(req.getProofUrl() != null ? req.getProofUrl().trim() : "")
                .notes(req.getNotes() != null ? req.getNotes().trim() : "")
                .createdBy(actorId)
                .createdByName(actorName)
                .build();
        paymentHistoryRepository.save(payment);

        // 3. Catat otomatis penerimaan kas masuk ke buku kas Cashflow (EntryType.INVOICE_PAYMENT)
        String actInfo = String.format("Pelunasan Invoice: %s - %s", saved.getInvoiceNo(), saved.getClientName());
        String actExplain = "";
        if (req.getReferenceNo() != null && !req.getReferenceNo().trim().isEmpty()) {
            actExplain = "Ref: " + req.getReferenceNo().trim();
        }
        if (req.getNotes() != null && !req.getNotes().trim().isEmpty()) {
            actExplain = actExplain.isEmpty() ? req.getNotes().trim() : actExplain + " | " + req.getNotes().trim();
        }

        cashflowService.recordInvoicePayment(saved.getId(), saved.getAmount(), payDate, actInfo, actExplain, user);

        invalidateCache();
        auditService.logInvoiceSettle(saved, saved.getAmount(), req.getReferenceNo(), user);

        int rescheduleCount = dueDateHistoryRepository.countByInvoiceId(id);
        String creatorName = resolveUserName(saved.getCreatedBy());

        return InvoiceResponseDto.fromEntity(saved, rescheduleCount, creatorName, actorName);
    }

    @Transactional
    public InvoiceResponseDto rescheduleDueDate(Integer id, RescheduleDueDateRequest req, UserSessionDto user) {
        Invoice invoice = invoiceRepository.findById(id)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Invoice dengan ID " + id + " tidak ditemukan"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Tidak dapat mengubah tanggal jatuh tempo invoice yang sudah lunas");
        }

        UUID actorId = user != null ? user.getUserId() : null;
        String actorName = user != null && user.getFullName() != null ? user.getFullName() : "Sistem";

        LocalDate prevDue = invoice.getDueDate();
        LocalDate newDue = req.getNewDueDate();
        int daysAdded = (int) ChronoUnit.DAYS.between(prevDue, newDue);

        if (invoice.getOriginalDueDate() == null) {
            invoice.setOriginalDueDate(prevDue);
        }
        invoice.setDueDate(newDue);

        // Jika jatuh tempo baru berada di masa depan, kembalikan status dari OVERDUE ke UNPAID
        if (!newDue.isBefore(LocalDate.now())) {
            invoice.setStatus(InvoiceStatus.UNPAID);
        } else {
            invoice.setStatus(InvoiceStatus.OVERDUE);
        }

        Invoice saved = invoiceRepository.save(invoice);

        InvoiceDueDateHistory history = InvoiceDueDateHistory.builder()
                .invoiceId(id)
                .previousDueDate(prevDue)
                .newDueDate(newDue)
                .daysAdded(daysAdded)
                .reason(req.getReason().trim())
                .changedBy(actorId)
                .changedByName(actorName)
                .build();
        dueDateHistoryRepository.save(history);

        invalidateCache();
        auditService.logInvoiceReschedule(saved, prevDue, newDue, req.getReason().trim(), user);

        int rescheduleCount = dueDateHistoryRepository.countByInvoiceId(id);
        String creatorName = resolveUserName(saved.getCreatedBy());
        String paidByName = resolveUserName(saved.getPaidBy());

        return InvoiceResponseDto.fromEntity(saved, rescheduleCount, creatorName, paidByName);
    }

    @Transactional
    public void deleteInvoice(Integer id, UserSessionDto user) {
        Invoice invoice = invoiceRepository.findById(id)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Invoice dengan ID " + id + " tidak ditemukan"));

        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        invalidateCache();
        auditService.logInvoiceDelete(invoice, user);
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoice(Integer id) {
        Invoice invoice = invoiceRepository.findById(id)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Invoice dengan ID " + id + " tidak ditemukan"));

        int rescheduleCount = dueDateHistoryRepository.countByInvoiceId(id);
        String creatorName = resolveUserName(invoice.getCreatedBy());
        String paidByName = resolveUserName(invoice.getPaidBy());

        return InvoiceResponseDto.fromEntity(invoice, rescheduleCount, creatorName, paidByName);
    }

    @Transactional(readOnly = true)
    public InvoiceHistorySummaryDto getInvoiceHistory(Integer id) {
        InvoiceResponseDto invoiceDto = getInvoice(id);

        List<InvoiceDueDateHistoryDto> rescheduleLogs = dueDateHistoryRepository.findByInvoiceIdOrderByCreatedAtDesc(id)
                .stream()
                .map(InvoiceDueDateHistoryDto::fromEntity)
                .toList();

        List<InvoicePaymentHistoryDto> paymentLogs = paymentHistoryRepository.findByInvoiceIdOrderByPaymentDateDesc(id)
                .stream()
                .map(InvoicePaymentHistoryDto::fromEntity)
                .toList();

        return InvoiceHistorySummaryDto.builder()
                .invoice(invoiceDto)
                .rescheduleLogs(rescheduleLogs)
                .paymentLogs(paymentLogs)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listInvoices(InvoiceFilterRequest filter) {
        int page = filter.getPage();
        int limit = filter.getLimit();
        String sortBy = filter.getSortBy();
        String sortDir = filter.getSortDir();

        String cacheKey = String.format("%sp:%d:l:%d:df:%s:dt:%s:st:%s:cn:%s:inv:%s:sb:%s:sd:%s",
                CACHE_PREFIX, page, limit,
                filter.getDate_from(), filter.getDate_to(),
                filter.getStatus(), filter.getClient_name(),
                filter.getInvoice_no(), sortBy, sortDir);

        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", cacheKey, e.getMessage());
        }

        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (filter.getDate_from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("shipmentDate"), filter.getDate_from()));
            }
            if (filter.getDate_to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("shipmentDate"), filter.getDate_to()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getClient_name() != null && !filter.getClient_name().trim().isEmpty()) {
                String like = "%" + filter.getClient_name().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("clientName")), like));
            }
            if (filter.getInvoice_no() != null && !filter.getInvoice_no().trim().isEmpty()) {
                String like = "%" + filter.getInvoice_no().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("invoiceNo")), like));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String property = switch (sortBy) {
            case "name", "client_name" -> "clientName";
            case "due_date" -> "dueDate";
            case "shipment_date" -> "shipmentDate";
            case "invoice_no" -> "invoiceNo";
            case "amount" -> "amount";
            default -> "createdAt";
        };

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(Sort.Direction.ASC, property)
                : Sort.by(Sort.Direction.DESC, property);

        Page<Invoice> invoicePage = invoiceRepository.findAll(spec, PageRequest.of(page - 1, limit, sort));

        List<InvoiceResponseDto> dtos = invoicePage.getContent().stream()
                .map(inv -> {
                    int rCount = dueDateHistoryRepository.countByInvoiceId(inv.getId());
                    String creatorName = resolveUserName(inv.getCreatedBy());
                    String paidByName = resolveUserName(inv.getPaidBy());
                    return InvoiceResponseDto.fromEntity(inv, rCount, creatorName, paidByName);
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", dtos);
        result.put("total", invoicePage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        result.put("total_pages", invoicePage.getTotalPages());

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for {}: {}", cacheKey, e.getMessage());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public InvoiceSummaryDto getSummary(InvoiceFilterRequest filter) {
        String cacheKey = String.format("%ssum:df:%s:dt:%s:st:%s:cn:%s:inv:%s",
                CACHE_PREFIX, filter.getDate_from(), filter.getDate_to(),
                filter.getStatus(), filter.getClient_name(), filter.getInvoice_no());

        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, InvoiceSummaryDto.class);
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", cacheKey, e.getMessage());
        }

        StringBuilder where = new StringBuilder(" WHERE deleted_at IS NULL ");
        Map<String, Object> params = new HashMap<>();

        if (filter.getDate_from() != null) {
            where.append(" AND shipment_date >= :dateFrom ");
            params.put("dateFrom", filter.getDate_from());
        }
        if (filter.getDate_to() != null) {
            where.append(" AND shipment_date <= :dateTo ");
            params.put("dateTo", filter.getDate_to());
        }
        if (filter.getClient_name() != null && !filter.getClient_name().trim().isEmpty()) {
            where.append(" AND client_name ILIKE :clientName ");
            params.put("clientName", "%" + filter.getClient_name().trim() + "%");
        }

        String sql = """
            SELECT
                COALESCE(SUM(amount), 0) AS total_amount,
                COUNT(*) AS total_count,
                COALESCE(SUM(CASE WHEN status = 'PAID' THEN amount ELSE 0 END), 0) AS paid_amount,
                COUNT(CASE WHEN status = 'PAID' THEN 1 END) AS paid_count,
                COALESCE(SUM(CASE WHEN status = 'UNPAID' THEN amount ELSE 0 END), 0) AS unpaid_amount,
                COUNT(CASE WHEN status = 'UNPAID' THEN 1 END) AS unpaid_count,
                COALESCE(SUM(CASE WHEN status = 'OVERDUE' THEN amount ELSE 0 END), 0) AS overdue_amount,
                COUNT(CASE WHEN status = 'OVERDUE' THEN 1 END) AS overdue_count
            FROM invoices
        """ + where;

        var query = jdbcClient.sql(sql);
        params.forEach(query::param);

        Map<String, Object> row = query.query().singleRow();

        InvoiceSummaryDto summary = InvoiceSummaryDto.builder()
                .totalAmount((BigDecimal) row.get("total_amount"))
                .totalCount(((Number) row.get("total_count")).longValue())
                .paidAmount((BigDecimal) row.get("paid_amount"))
                .paidCount(((Number) row.get("paid_count")).longValue())
                .unpaidAmount((BigDecimal) row.get("unpaid_amount"))
                .unpaidCount(((Number) row.get("unpaid_count")).longValue())
                .overdueAmount((BigDecimal) row.get("overdue_amount"))
                .overdueCount(((Number) row.get("overdue_count")).longValue())
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
        List<Object[]> rows = invoiceRepository.findAvailablePeriodsRaw();
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

    private String resolveUserName(UUID userId) {
        if (userId == null) return "";
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("");
    }
}
