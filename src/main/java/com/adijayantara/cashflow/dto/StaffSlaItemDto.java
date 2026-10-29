package com.adijayantara.cashflow.dto;

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
public class StaffSlaItemDto {

    @JsonProperty("actor_id")
    private UUID actorId;

    @JsonProperty("actor_name")
    private String actorName;

    @JsonProperty("actor_role")
    private String actorRole;

    @JsonProperty("total_entries")
    private long totalEntries;

    @JsonProperty("on_time_entries")
    private long onTimeEntries;

    @JsonProperty("acceptable_entries")
    private long acceptableEntries;

    @JsonProperty("critical_late_entries")
    private long criticalLateEntries;

    @JsonProperty("on_time_percentage")
    private double onTimePercentage;

    @JsonProperty("avg_lag_days")
    private double avgLagDays;

    @JsonProperty("total_edits")
    private long totalEdits;

    @JsonProperty("total_deletions")
    private long totalDeletions;

    @JsonProperty("last_active_at")
    private String lastActiveAt;
}
