package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("actor_id")
    private UUID actorId;

    @JsonProperty("actor_name")
    private String actorName;

    @JsonProperty("actor_role")
    private String actorRole;

    @JsonProperty("action")
    private String action;

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("entity_reference")
    private String entityReference;

    @JsonProperty("event_date")
    private LocalDate eventDate;

    @JsonProperty("system_timestamp")
    private LocalDateTime systemTimestamp;

    @JsonProperty("lag_days")
    private Integer lagDays;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("flag_reason")
    private String flagReason;

    @JsonProperty("old_values")
    private Object oldValues;

    @JsonProperty("new_values")
    private Object newValues;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}

