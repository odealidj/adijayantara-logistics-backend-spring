package com.adijayantara.cashflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionGroupDto {

    @JsonProperty("module")
    private String module;

    @JsonProperty("permissions")
    @Builder.Default
    private List<PermissionItemDto> permissions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionItemDto {
        @JsonProperty("id")
        private UUID id;

        @JsonProperty("module")
        private String module;

        @JsonProperty("code")
        private String code;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;
    }
}
