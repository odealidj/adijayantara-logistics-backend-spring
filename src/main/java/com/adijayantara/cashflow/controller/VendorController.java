package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.domain.entity.Vendor;
import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.PaginationMeta;
import com.adijayantara.cashflow.service.VendorService;
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
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendor Management", description = "Endpoint master data vendor / mitra transporter logistik")
@SecurityRequirement(name = "BearerAuth")
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('*', 'vendors.view')")
    @Operation(summary = "Daftar Vendor", description = "Menampilkan data mitra vendor aktif dengan paginasi dan pencarian")
    public ResponseEntity<ApiResponse<List<Vendor>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort_by", defaultValue = "name") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "ASC") String sortDir
    ) {
        Map<String, Object> data = vendorService.listAllVendors(page, limit, search, sortBy, sortDir);

        @SuppressWarnings("unchecked")
        List<Vendor> items = (List<Vendor>) data.get("items");
        long total = (long) data.get("total");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved", items, meta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'vendors.view')")
    @Operation(summary = "Detail Vendor", description = "Mengambil data vendor berdasarkan ID")
    public ResponseEntity<ApiResponse<Vendor>> get(@PathVariable("id") int id) {
        Vendor vendor = vendorService.getVendorByID(id);
        return ResponseEntity.ok(ApiResponse.success("Vendor retrieved", vendor));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('*', 'vendors.manage')")
    @Operation(summary = "Tambah Vendor Baru", description = "Mendaftarkan mitra vendor transporter baru")
    public ResponseEntity<ApiResponse<Vendor>> create(@RequestBody Vendor vendor) {
        Vendor saved = vendorService.createVendor(vendor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vendor created successfully", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'vendors.manage')")
    @Operation(summary = "Perbarui Vendor", description = "Memperbarui informasi kontak dan catatan vendor")
    public ResponseEntity<ApiResponse<Vendor>> update(@PathVariable("id") int id, @RequestBody Vendor vendor) {
        Vendor updated = vendorService.updateVendor(id, vendor);
        return ResponseEntity.ok(ApiResponse.success("Vendor updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'vendors.manage')")
    @Operation(summary = "Hapus Vendor", description = "Soft delete mitra vendor dari master data")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") int id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok(ApiResponse.success("Vendor deleted successfully", null));
    }
}
