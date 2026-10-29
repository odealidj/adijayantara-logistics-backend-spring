package com.adijayantara.cashflow;

import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.LoginRequest;
import com.adijayantara.cashflow.dto.LoginResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuditAndAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String financeToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Finance user
        LoginRequest financeLogin = LoginRequest.builder()
                .identifier("finance.ayu@adijayantara.co.id")
                .password("password123")
                .build();
        MvcResult fRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financeLogin)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<LoginResponse> fResp = objectMapper.readValue(
                fRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        this.financeToken = fResp.getData().getToken();

        // Owner user (has audit.view & audit.manage)
        LoginRequest adminLogin = LoginRequest.builder()
                .identifier("owner.adi@adijayantara.co.id")
                .password("password123")
                .build();
        MvcResult aRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<LoginResponse> aResp = objectMapper.readValue(
                aRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        this.adminToken = aResp.getData().getToken();
    }

    @Test
    @DisplayName("Analytics: Cashflow Runway & CCC Gap returns 60 days rolling buckets")
    void testCashflowRunway() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/cashflow-runway?days=60")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.current_cash_balance").isNumber())
                .andExpect(jsonPath("$.data.minimum_cash_buffer").value(25000000.0))
                .andExpect(jsonPath("$.data.buckets", notNullValue()))
                .andExpect(jsonPath("$.data.avg_customer_dso").isNumber())
                .andExpect(jsonPath("$.data.avg_vendor_dpo").isNumber())
                .andExpect(jsonPath("$.data.financing_gap_days").isNumber());
    }

    @Test
    @DisplayName("Analytics: BCG Route Matrix calculates trip counts and margins")
    void testRouteMatrix() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/route-matrix")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.total_routes").isNumber())
                .andExpect(jsonPath("$.data.avg_margin_threshold").isNumber())
                .andExpect(jsonPath("$.data.routes", notNullValue()));
    }

    @Test
    @DisplayName("Analytics: Customer Discipline & Pareto 80/20")
    void testCustomerDisciplineAndPareto() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/customer-discipline-pareto")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.total_clients").isNumber())
                .andExpect(jsonPath("$.data.total_revenue").isNumber())
                .andExpect(jsonPath("$.data.customers", notNullValue()));
    }

    @Test
    @DisplayName("Analytics: Vendor Efficiency & Fleet Concentration")
    void testVendorEfficiency() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/vendor-efficiency")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.total_vendors").isNumber())
                .andExpect(jsonPath("$.data.vendors", notNullValue()));
    }

    @Test
    @DisplayName("Audit: Executive Summary KPI returns fraud index and lag metrics")
    void testAuditSummary() throws Exception {
        mockMvc.perform(get("/api/v1/audit/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.total_logs").isNumber())
                .andExpect(jsonPath("$.data.total_critical").isNumber())
                .andExpect(jsonPath("$.data.total_warning").isNumber())
                .andExpect(jsonPath("$.data.avg_cashflow_lag_days").isNumber());
    }

    @Test
    @DisplayName("Audit: Staff SLA Scorecard evaluates input discipline")
    void testStaffScorecard() throws Exception {
        mockMvc.perform(get("/api/v1/audit/staff-scorecard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("Audit: Anomalies, Input Lag, and Forensic Logs pagination")
    void testAuditListsAndPagination() throws Exception {
        // Anomalies
        mockMvc.perform(get("/api/v1/audit/anomalies?page=1&limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.meta.page").value(1));

        // Input Lag
        mockMvc.perform(get("/api/v1/audit/input-lag?page=1&limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.meta.page").value(1));

        // Forensic Logs
        mockMvc.perform(get("/api/v1/audit/logs?page=1&limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.meta.page").value(1));
    }
}
