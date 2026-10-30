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

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UtilityAndNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String financeToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .identifier("finance.ayu@adijayantara.co.id")
                .password("password123")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<LoginResponse> resp = objectMapper.readValue(
                res.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        this.financeToken = resp.getData().getToken();
    }

    @Test
    @DisplayName("Public Utility: Idempotency Key & Healthcheck")
    void testPublicUtilityEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/utility/idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.idempotency_key").isNotEmpty())
                .andExpect(jsonPath("$.data.expires_in_hours").value(24));

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Notification: Check Invoices & Get Feeds")
    void testNotificationFlow() throws Exception {
        // 1. Trigger scan check invoices
        mockMvc.perform(post("/api/v1/notifications/check-invoices")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.dispatched_count").isNumber());

        // 2. Get unread count
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.unread_count").isNumber());

        // 3. List notifications
        MvcResult listRes = mockMvc.perform(get("/api/v1/notifications?page=1&limit=10")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        ApiResponse<List<Map<String, Object>>> listResp = objectMapper.readValue(
                listRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        if (!listResp.getData().isEmpty()) {
            String firstId = (String) listResp.getData().get(0).get("id");

            // 4. Mark single as read
            mockMvc.perform(patch("/api/v1/notifications/" + firstId + "/read")
                            .header("Authorization", "Bearer " + financeToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(true));
        }

        // 5. Mark all as read
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }
}

