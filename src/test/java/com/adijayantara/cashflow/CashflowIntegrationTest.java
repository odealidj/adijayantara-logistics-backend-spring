package com.adijayantara.cashflow;

import com.adijayantara.cashflow.domain.entity.CashflowEntry;
import com.adijayantara.cashflow.domain.entity.Customer;
import com.adijayantara.cashflow.domain.entity.Vendor;
import com.adijayantara.cashflow.domain.enums.EntryType;
import com.adijayantara.cashflow.domain.enums.PaymentStatus;
import com.adijayantara.cashflow.dto.CreateShipmentRequest;
import com.adijayantara.cashflow.dto.CreateTopUpRequest;
import com.adijayantara.cashflow.dto.LoginRequest;
import com.adijayantara.cashflow.dto.UpdateCashflowRequest;
import com.adijayantara.cashflow.dto.UpdateStatusRequest;
import com.adijayantara.cashflow.repository.CashflowEntryHistoryRepository;
import com.adijayantara.cashflow.repository.CashflowEntryRepository;
import com.adijayantara.cashflow.repository.CustomerRepository;
import com.adijayantara.cashflow.repository.VendorRepository;
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
class CashflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CashflowEntryRepository cashflowEntryRepository;

    @Autowired
    private CashflowEntryHistoryRepository cashflowEntryHistoryRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Login as finance.ayu to obtain session token
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
    @DisplayName("End-to-End Cashflow & Shipment Lifecycle: Top-Up -> Shipment -> Auto-Vendor/Customer -> Update Cascading -> Delete")
    void testCashflowLifecycle() throws Exception {
        // 1. Get initial summary
        mockMvc.perform(get("/api/v1/cashflow/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.current_saldo").exists());

        // 2. Record Top-Up (Rp 50.000.000)
        CreateTopUpRequest topUpReq = CreateTopUpRequest.builder()
                .dateOfEntry(LocalDate.now())
                .kredit(BigDecimal.valueOf(50_000_000))
                .actInformation("TOP UP MODAL AWAL")
                .actExplaination("Injeksi kas operasional test")
                .build();

        MvcResult topUpResult = mockMvc.perform(post("/api/v1/cashflow/topup")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topUpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.kredit").value(50000000.0))
                .andExpect(jsonPath("$.data.entry_type").value("TOP_UP"))
                .andExpect(jsonPath("$.data.remarks").value("PAID"))
                .andReturn();

        JsonNode topUpJson = objectMapper.readTree(topUpResult.getResponse().getContentAsString());
        int topUpId = topUpJson.path("data").path("id").asInt();
        double saldoAfterTopUp = topUpJson.path("data").path("saldo").asDouble();

        // 3. Record Shipment (Cost Rp 12.000.000, Selling Rp 15.000.000, TOP 14 days)
        String vendorName = "Vendor Test " + System.currentTimeMillis();
        String customerName = "Customer Test " + System.currentTimeMillis();

        CreateShipmentRequest shipmentReq = CreateShipmentRequest.builder()
                .dateOfEntry(LocalDate.now())
                .vendorNameRaw(vendorName)
                .customerNameRaw(customerName)
                .actInformation("ARMADA CDD - RUTE SURABAYA")
                .actExplaination("Pengiriman consumer goods 5 ton")
                .grandCost(BigDecimal.valueOf(12_000_000))
                .grandSelling(BigDecimal.valueOf(15_000_000))
                .debit(BigDecimal.valueOf(12_000_000))
                .topDays(14)
                .remarks(PaymentStatus.UNPAID)
                .build();

        MvcResult shipmentResult = mockMvc.perform(post("/api/v1/cashflow/shipment")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipmentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.debit").value(12000000.0))
                .andExpect(jsonPath("$.data.profit").value(3000000.0))
                .andExpect(jsonPath("$.data.margin_pct").value(0.2))
                .andExpect(jsonPath("$.data.remarks").value("UNPAID"))
                .andReturn();

        JsonNode shipmentJson = objectMapper.readTree(shipmentResult.getResponse().getContentAsString());
        int shipmentId = shipmentJson.path("data").path("id").asInt();
        int shipmentSeqNo = shipmentJson.path("data").path("sequence_no").asInt();
        double saldoAfterShipment = shipmentJson.path("data").path("saldo").asDouble();

        // Verify rolling balance decrement
        assertThat(saldoAfterShipment).isEqualTo(saldoAfterTopUp - 12_000_000.0);

        // Verify auto-registered vendor and customer in DB
        Vendor autoVendor = vendorRepository.findByNameIgnoreCaseAndDeletedAtIsNull(vendorName).orElse(null);
        assertThat(autoVendor).isNotNull();

        Customer autoCustomer = customerRepository.findByNameIgnoreCaseAndDeletedAtIsNull(customerName).orElse(null);
        assertThat(autoCustomer).isNotNull();

        // 4. Record second Shipment (Cost Rp 8.000.000, Selling Rp 10.000.000)
        CreateShipmentRequest shipment2Req = CreateShipmentRequest.builder()
                .dateOfEntry(LocalDate.now())
                .vendorNameRaw(vendorName)
                .customerNameRaw(customerName)
                .actInformation("ARMADA FUSO - RUTE SEMARANG")
                .grandCost(BigDecimal.valueOf(8_000_000))
                .grandSelling(BigDecimal.valueOf(10_000_000))
                .debit(BigDecimal.valueOf(8_000_000))
                .topDays(7)
                .build();

        MvcResult shipment2Result = mockMvc.perform(post("/api/v1/cashflow/shipment")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipment2Req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode shipment2Json = objectMapper.readTree(shipment2Result.getResponse().getContentAsString());
        int shipment2Id = shipment2Json.path("data").path("id").asInt();
        double saldoAfterShipment2 = shipment2Json.path("data").path("saldo").asDouble();

        // Verify second shipment decrement
        assertThat(saldoAfterShipment2).isEqualTo(saldoAfterShipment - 8_000_000.0);

        // 5. Test List Cashflow pagination
        mockMvc.perform(get("/api/v1/cashflow?page=1&limit=10&sort=DESC")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(10))
                .andExpect(jsonPath("$.meta.total_items").isNumber());

        // 6. Test Update Payment Status (PATCH remarks to PAID)
        UpdateStatusRequest statusReq = UpdateStatusRequest.builder()
                .remarks(PaymentStatus.PAID)
                .build();

        mockMvc.perform(patch("/api/v1/cashflow/" + shipmentId + "/status")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.remarks").value("PAID"));

        CashflowEntry updatedRemarksEntry = cashflowEntryRepository.findById(shipmentId).orElseThrow();
        assertThat(updatedRemarksEntry.getRemarks()).isEqualTo(PaymentStatus.PAID);

        // 7. Test Retroactive Update with Cascading Balance
        // Ubah shipment 1: grand_cost & debit turun dari Rp 12.000.000 menjadi Rp 10.000.000 (hemat Rp 2.000.000)
        UpdateCashflowRequest updateReq = UpdateCashflowRequest.builder()
                .entryType(EntryType.SHIPMENT)
                .grandCost(BigDecimal.valueOf(10_000_000))
                .grandSelling(BigDecimal.valueOf(15_000_000))
                .debit(BigDecimal.valueOf(10_000_000))
                .remarks(PaymentStatus.PAID)
                .actInformation("ARMADA CDD - RUTE SURABAYA (REVISI HPP)")
                .build();

        mockMvc.perform(put("/api/v1/cashflow/" + shipmentId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.debit").value(10000000.0))
                .andExpect(jsonPath("$.data.profit").value(5000000.0));

        // Verify history table contains the archive
        boolean historyExists = !cashflowEntryHistoryRepository.findByEntryIdOrderByArchivedAtDesc(shipmentId).isEmpty();
        assertThat(historyExists).isTrue();

        // Verify cascading balance on shipment 2
        CashflowEntry shipment2AfterCascade = cashflowEntryRepository.findById(shipment2Id).orElseThrow();
        assertThat(shipment2AfterCascade.getSaldo().doubleValue()).isEqualTo(saldoAfterShipment2 + 2_000_000.0);

        // 8. Test Available Periods
        mockMvc.perform(get("/api/v1/cashflow/periods")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").isArray());

        // 9. Clean up test entries: Delete Shipment 2 and Top-Up
        mockMvc.perform(delete("/api/v1/cashflow/" + shipment2Id)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));

        mockMvc.perform(delete("/api/v1/cashflow/" + shipmentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));

        mockMvc.perform(delete("/api/v1/cashflow/" + topUpId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));

        assertThat(cashflowEntryRepository.findById(shipment2Id)).isEmpty();
        assertThat(cashflowEntryRepository.findById(shipmentId)).isEmpty();
        assertThat(cashflowEntryRepository.findById(topUpId)).isEmpty();
    }
}
