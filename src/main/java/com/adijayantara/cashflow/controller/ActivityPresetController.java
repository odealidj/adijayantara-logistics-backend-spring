package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.domain.entity.ActivityPreset;
import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.PaginationMeta;
import com.adijayantara.cashflow.service.ActivityPresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/activity-presets")
@RequiredArgsConstructor
@Tag(name = "Activity Presets Management", description = "Endpoint master data preset aktivitas, armada, dan rute pengiriman")
@SecurityRequirement(name = "BearerAuth")
public class ActivityPresetController {

    private final ActivityPresetService presetService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('*', 'presets.view')")
    @Operation(summary = "Daftar Preset Aktivitas", description = "Mengambil daftar preset berdasarkan kategori (ACT_INFO / ACT_EXPLAIN) atau pencarian kata kunci")
    public ResponseEntity<ApiResponse<List<ActivityPreset>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "100") int limit,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort_by", defaultValue = "name") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "ASC") String sortDir
    ) {
        Map<String, Object> data = presetService.listPresets(category, search, page, limit, sortBy, sortDir);

        @SuppressWarnings("unchecked")
        List<ActivityPreset> items = (List<ActivityPreset>) data.get("items");
        long total = (long) data.get("total");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Daftar preset berhasil diambil", items, meta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'presets.view')")
    @Operation(summary = "Detail Preset Aktivitas", description = "Mengambil data preset aktivitas berdasarkan ID")
    public ResponseEntity<ApiResponse<ActivityPreset>> getByID(@PathVariable("id") int id) {
        ActivityPreset preset = presetService.getPresetByID(id);
        return ResponseEntity.ok(ApiResponse.success("Detail preset ditemukan", preset));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('*', 'presets.manage')")
    @Operation(summary = "Tambah Preset Baru", description = "Mendaftarkan preset aktivitas baru")
    public ResponseEntity<ApiResponse<ActivityPreset>> create(@RequestBody ActivityPreset preset) {
        ActivityPreset saved = presetService.createPreset(preset);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Preset created successfully", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'presets.manage')")
    @Operation(summary = "Perbarui Preset", description = "Mengubah nama atau deskripsi preset aktivitas")
    public ResponseEntity<ApiResponse<ActivityPreset>> update(@PathVariable("id") int id, @RequestBody ActivityPreset preset) {
        ActivityPreset updated = presetService.updatePreset(id, preset);
        return ResponseEntity.ok(ApiResponse.success("Preset updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'presets.manage')")
    @Operation(summary = "Hapus Preset", description = "Soft delete preset aktivitas dari master data")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") int id) {
        presetService.deletePreset(id);
        return ResponseEntity.ok(ApiResponse.success("Preset deleted successfully", null));
    }
}

