package com.adijayantara.cashflow;

import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.entity.Invoice;
import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.InvoiceStatus;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.adijayantara.cashflow.dto.CreateInvoiceRequest;
import com.adijayantara.cashflow.dto.LoginRequest;
import com.adijayantara.cashflow.dto.RescheduleDueDateRequest;
import com.adijayantara.cashflow.dto.SettleInvoiceRequest;
import com.adijayantara.cashflow.repository.CashflowEntryRepository;
import com.adijayantara.cashflow.repository.InvoiceDueDateHistoryRepository;
import com.adijayantara.cashflow.repository.InvoicePaymentHistoryRepository;
import com.adijayantara.cashflow.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDueDateHistoryRepository dueDateHistoryRepository;

    @Autowired
    private InvoicePaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private CashflowEntryRepository cashflowEntryRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .identifier("finance.ayu@adijayantara.co.id")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        authToken = jsonNode.path("data").path("token").asText();
        assertThat(authToken).isNotBlank();
    }

    @Test
    @DisplayName("End-to-End Invoice Lifecycle: Create -> Reschedule -> Settle (Auto-Cashflow) -> History -> Soft Delete")
    void testInvoiceLifecycle() throws Exception {
        // 1. Get initial invoice summary
        mockMvc.perform(get("/api/v1/invoices/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.total_amount").exists());

        // 2. Create Invoice
        String uniqueInvoiceNo = "INV/TEST/" + System.currentTimeMillis();
        CreateInvoiceRequest createReq = CreateInvoiceRequest.builder()
                .invoiceNo(uniqueInvoiceNo)
                .clientName("PT Logistik Nusantara Test")
                .shipmentDate(LocalDate.now())
                .topDays(30)
                .amount(BigDecimal.valueOf(25_000_000))
                .notes("Tagihan rute antar pulau muatan retail")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/invoices")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.invoice_no").value(uniqueInvoiceNo))
                .andExpect(jsonPath("$.data.amount").value(25000000.0))
                .andExpect(jsonPath("$.data.status").value("UNPAID"))
                .andExpect(jsonPath("$.data.top_terms").value("Net 30 Hari"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        int invoiceId = createdJson.path("data").path("id").asInt();

        // 3. Reschedule Due Date (+15 days)
        LocalDate newDueDate = LocalDate.now().plusDays(45);
        RescheduleDueDateRequest rescheduleReq = RescheduleDueDateRequest.builder()
                .newDueDate(newDueDate)
                .reason("Permintaan kelonggaran tempo giro dari finance klien")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/reschedule")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rescheduleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.reschedule_count").value(1));

        // Verify history endpoint
        mockMvc.perform(get("/api/v1/invoices/" + invoiceId + "/history")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.reschedule_logs").isArray())
                .andExpect(jsonPath("$.data.reschedule_logs[0].reason").value("Permintaan kelonggaran tempo giro dari finance klien"));

        // Record initial cashflow balance before settlement
        BigDecimal initialCashSaldo = cashflowEntryRepository.getCurrentSaldo();

        // 4. Settle Invoice (Mark Paid)
        SettleInvoiceRequest settleReq = SettleInvoiceRequest.builder()
                .paymentDate(LocalDate.now())
                .referenceNo("TRF-BCA-889911")
                .proofUrl("https://storage.adijayantara.co.id/proofs/bca-889911.jpg")
                .notes("Pelunasan transfer BCA telah divalidasi mutasi bank")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/pay")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.payment_reference").value("TRF-BCA-889911"));

        // Verify invoice in DB is PAID
        Invoice settledInv = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(settledInv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(settledInv.getPaidAt()).isNotNull();

        // Verify payment history log in DB
        boolean hasPaymentLog = !paymentHistoryRepository.findByInvoiceIdOrderByPaymentDateDesc(invoiceId).isEmpty();
        assertThat(hasPaymentLog).isTrue();

        // Verify auto-generated Cashflow entry INVOICE_PAYMENT in DB
        CashflowEntry autoCashflow = cashflowEntryRepository.findByInvoiceId(invoiceId).orElse(null);
        assertThat(autoCashflow).isNotNull();
        assertThat(autoCashflow.getEntryType()).isEqualTo(EntryType.INVOICE_PAYMENT);
        assertThat(autoCashflow.getKredit().doubleValue()).isEqualTo(25_000_000.0);
        assertThat(autoCashflow.getRemarks()).isEqualTo(PaymentStatus.PAID);

        // Verify cash wallet balance increased by 25.000.000
        BigDecimal newCashSaldo = cashflowEntryRepository.getCurrentSaldo();
        assertThat(newCashSaldo.doubleValue()).isEqualTo(initialCashSaldo.doubleValue() + 25_000_000.0);

        // 5. Test Available Periods
        mockMvc.perform(get("/api/v1/invoices/periods")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 6. Delete Invoice (Soft Delete)
        mockMvc.perform(delete("/api/v1/invoices/" + invoiceId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));

        Invoice deletedInv = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(deletedInv.getDeletedAt()).isNotNull();

        // Clean up auto-generated cashflow entry to keep test db clean
        if (autoCashflow != null) {
            cashflowEntryRepository.delete(autoCashflow);
        }
    }
}

