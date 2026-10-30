package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.enums.NotificationCategory;
import com.adijayantara.cashflow.domain.enums.NotificationSeverity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationInput {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("target_role")
    private String targetRole;

    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    @JsonProperty("category")
    private NotificationCategory category;

    @JsonProperty("severity")
    private NotificationSeverity severity;

    @JsonProperty("action_url")
    private String actionUrl;

    @JsonProperty("metadata")
    private String metadata;
}

