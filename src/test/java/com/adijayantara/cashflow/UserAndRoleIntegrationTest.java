package com.adijayantara.cashflow;

import com.adijayantara.cashflow.dto.*;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserAndRoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String superAdminToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Super admin
        LoginRequest saLogin = LoginRequest.builder()
                .identifier("admin@example.com")
                .password("password123")
                .build();

        MvcResult saRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saLogin)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<LoginResponse> saResp = objectMapper.readValue(
                saRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        this.superAdminToken = saResp.getData().getToken();

        // Business admin
        LoginRequest aLogin = LoginRequest.builder()
                .identifier("admin.bisnis@adijayantara.co.id")
                .password("password123")
                .build();

        MvcResult aRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aLogin)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<LoginResponse> aResp = objectMapper.readValue(
                aRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        this.adminToken = aResp.getData().getToken();
    }

    @Test
    @DisplayName("Roles: List, Groups, Detail, Create, Update Permissions")
    void testRolesFlow() throws Exception {
        // 1. List roles
        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").isNotEmpty());

        // 2. List permissions grouped
        mockMvc.perform(get("/api/v1/roles/permissions")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].module").isNotEmpty());

        // 3. Create custom role
        String randRoleCode = "custom_qa_" + UUID.randomUUID().toString().substring(0, 6);
        CreateRoleInput roleInput = CreateRoleInput.builder()
                .code(randRoleCode)
                .name("QA Tester " + randRoleCode)
                .description("Quality Assurance Role")
                .permissions(List.of("cashflow.view", "invoices.view"))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleInput)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.code").value(randRoleCode))
                .andReturn();

        ApiResponse<RoleResponseDto> roleResp = objectMapper.readValue(
                createRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        UUID roleId = roleResp.getData().getId();

        // 4. Get Role Detail
        mockMvc.perform(get("/api/v1/roles/" + roleId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.permissions", hasItem("cashflow.view")));

        // 5. Update Permissions
        UpdatePermissionsInput permUpdate = UpdatePermissionsInput.builder()
                .permissions(List.of("cashflow.view", "cashflow.create"))
                .build();

        mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));

        // 6. Delete role
        mockMvc.perform(delete("/api/v1/roles/" + roleId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @DisplayName("Users: List, Create, Get, Update, Reset Password, Delete")
    void testUsersFlow() throws Exception {
        // 1. List users
        mockMvc.perform(get("/api/v1/users?page=1&limit=10")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total_items").isNumber());

        // 2. Create staff user
        String randEmail = "staff_" + UUID.randomUUID().toString().substring(0, 6) + "@adijayantara.co.id";
        CreateUserInput userInput = CreateUserInput.builder()
                .fullName("Staf Baru Testing")
                .email(randEmail)
                .phone("0899" + (int)(Math.random() * 8999999 + 1000000))
                .role("finance")
                .status("ACTIVE")
                .password("password123")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userInput)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.email").value(randEmail))
                .andReturn();

        ApiResponse<UserResponseDto> userResp = objectMapper.readValue(
                createRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        UUID newUserId = userResp.getData().getId();

        // 3. Get single user
        mockMvc.perform(get("/api/v1/users/" + newUserId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.full_name").value("Staf Baru Testing"));

        // 4. Update user
        String updatedPhone = "0888" + (int)(Math.random() * 8999999 + 1000000);
        UpdateUserInput updateInput = UpdateUserInput.builder()
                .fullName("Staf Baru Testing Terupdate")
                .email(randEmail)
                .phone(updatedPhone)
                .role("viewer")
                .status("ACTIVE")
                .build();

        mockMvc.perform(put("/api/v1/users/" + newUserId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.full_name").value("Staf Baru Testing Terupdate"));

        // 5. Reset Password
        ResetPasswordRequest pwdReq = ResetPasswordRequest.builder()
                .password("newSecretPass123")
                .build();

        mockMvc.perform(patch("/api/v1/users/" + newUserId + "/password")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pwdReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));

        // 6. Delete User
        mockMvc.perform(delete("/api/v1/users/" + newUserId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @DisplayName("System Metrics: Telemetry, Simulate Slow Query, Benchmark Status")
    void testSystemMetricsFlow() throws Exception {
        // 1. Get metrics
        mockMvc.perform(get("/api/v1/system/metrics")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.runtime.go_version").isNotEmpty())
                .andExpect(jsonPath("$.data.database_pool.max_open").isNumber())
                .andExpect(jsonPath("$.data.business_kpi.shipments_today").isNumber());

        // 2. Simulate slow query
        mockMvc.perform(post("/api/v1/system/simulate-slow-query?duration_ms=50")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true));

        // 3. Benchmark status & start
        mockMvc.perform(get("/api/v1/system/benchmark/status")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IDLE"));

        mockMvc.perform(post("/api/v1/system/benchmark/start")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }
}
