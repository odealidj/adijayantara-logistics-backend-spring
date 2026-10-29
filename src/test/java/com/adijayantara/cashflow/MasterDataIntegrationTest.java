package com.adijayantara.cashflow;

import com.adijayantara.cashflow.domain.entity.ActivityPreset;
import com.adijayantara.cashflow.domain.entity.Customer;
import com.adijayantara.cashflow.domain.entity.Vendor;
import com.adijayantara.cashflow.dto.LoginRequest;
import com.adijayantara.cashflow.repository.ActivityPresetRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MasterDataIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ActivityPresetRepository activityPresetRepository;

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
    @DisplayName("Vendor Master Data CRUD Lifecycle")
    void testVendorCrud() throws Exception {
        String vendorName = "Vendor CRUD Test " + System.currentTimeMillis();
        Vendor newVendor = Vendor.builder()
                .name(vendorName)
                .email("transporter@test.co.id")
                .phone("08123456789")
                .notes("Armada Wingbox Jawa-Bali")
                .build();

        // 1. Create
        MvcResult createRes = mockMvc.perform(post("/api/v1/vendors")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newVendor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.name").value(vendorName))
                .andReturn();

        int vendorId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asInt();

        // 2. List & Search
        mockMvc.perform(get("/api/v1/vendors?search=" + vendorName)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data[0].id").value(vendorId));

        // 3. Get by ID
        mockMvc.perform(get("/api/v1/vendors/" + vendorId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("08123456789"));

        // 4. Update
        Vendor updateData = Vendor.builder()
                .name(vendorName + " (Updated)")
                .phone("08999999999")
                .build();

        mockMvc.perform(put("/api/v1/vendors/" + vendorId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("08999999999"));

        // 5. Delete (Soft Delete)
        mockMvc.perform(delete("/api/v1/vendors/" + vendorId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        Vendor deleted = vendorRepository.findById(vendorId).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Customer Master Data CRUD Lifecycle")
    void testCustomerCrud() throws Exception {
        String customerName = "Customer CRUD Test " + System.currentTimeMillis();
        Customer newCustomer = Customer.builder()
                .name(customerName)
                .picName("Budi Santoso")
                .email("budi@klien.co.id")
                .phone("08555555555")
                .address("Kawasan Industri Rungkut Surabaya")
                .notes("Klien FMCG")
                .build();

        // 1. Create
        MvcResult createRes = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCustomer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.name").value(customerName))
                .andReturn();

        int customerId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asInt();

        // 2. Get by ID
        mockMvc.perform(get("/api/v1/customers/" + customerId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pic_name").value("Budi Santoso"));

        // 3. Delete (Soft Delete)
        mockMvc.perform(delete("/api/v1/customers/" + customerId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        Customer deleted = customerRepository.findById(customerId).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Activity Preset Master Data CRUD Lifecycle")
    void testActivityPresetCrud() throws Exception {
        String presetName = "RUTE-TEST-" + System.currentTimeMillis();
        ActivityPreset newPreset = ActivityPreset.builder()
                .category("ACT_EXPLAIN")
                .name(presetName)
                .description("Rute uji ekspedisi Surabaya - Banyuwangi")
                .isActive(true)
                .build();

        // 1. Create
        MvcResult createRes = mockMvc.perform(post("/api/v1/activity-presets")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPreset)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.name").value(presetName))
                .andReturn();

        int presetId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asInt();

        // 2. List with Category Filter
        mockMvc.perform(get("/api/v1/activity-presets?category=ACT_EXPLAIN&search=" + presetName)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(presetId));

        // 3. Delete (Soft Delete)
        mockMvc.perform(delete("/api/v1/activity-presets/" + presetId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        ActivityPreset deleted = activityPresetRepository.findById(presetId).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }
}
