package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSessionDto implements Serializable {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("role")
    private String role;

    @JsonProperty("role_id")
    private UUID roleId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("permissions")
    @Builder.Default
    private List<String> permissions = new ArrayList<>();

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("expires_at")
    private String expiresAt;
}

