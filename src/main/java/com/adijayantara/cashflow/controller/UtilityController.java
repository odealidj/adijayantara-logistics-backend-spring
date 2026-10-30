package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "Utility", description = "Endpoints utilitas sistem seperti idempotency key dan health check")
public class UtilityController {

    @Operation(summary = "Generate fresh Idempotency Key", description = "Menghasilkan RFC 4122 UUID v4 unik untuk header 'X-Idempotency-Key'")
    @GetMapping({"/api/v1/utility/idempotency-key", "/api/v1/idempotency-key"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateIdempotencyKey() {
        String key = UUID.randomUUID().toString();
        Map<String, Object> data = Map.of(
            "idempotency_key", key,
            "expires_in_hours", 24,
            "usage_hint", "Sertakan key ini pada header 'X-Idempotency-Key' untuk request mutasi (POST, PUT, PATCH, DELETE) guna mencegah duplikasi."
        );
        return ResponseEntity.ok(ApiResponse.success("Idempotency key baru berhasil digenerate", data));
    }

    @Operation(summary = "Health check", description = "Healthcheck status server")
    @GetMapping({"/health", "/api/v1/health"})
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}

