package com.adijayantara.cashflow.dto;

import com.adijayantara.cashflow.domain.entity.Notification;
import com.adijayantara.cashflow.domain.enums.NotificationCategory;
import com.adijayantara.cashflow.domain.enums.NotificationSeverity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponseDto {

    @JsonProperty("id")
    private UUID id;

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

    @JsonProperty("is_read")
    private Boolean isRead;

    @JsonProperty("read_at")
    private LocalDateTime readAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static NotificationResponseDto fromEntity(Notification entity) {
        if (entity == null) return null;
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .targetRole(entity.getTargetRole())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .category(entity.getCategory())
                .severity(entity.getSeverity())
                .actionUrl(entity.getActionUrl())
                .metadata(entity.getMetadata())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
