package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.config.SessionAuthenticationToken;
import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles & Permissions", description = "Manajemen peran sistem dan matriks izin akses (PBAC)")
@SecurityRequirement(name = "BearerAuth")
public class RoleController {

    private final RoleService roleService;

    private UserSessionDto getCurrentUser(Authentication auth) {
        if (auth instanceof SessionAuthenticationToken sat) {
            return sat.getPrincipal();
        }
        return null;
    }

    @Operation(summary = "List all roles", description = "Mengambil daftar seluruh peran aktif beserta jumlah penggunanya")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponseDto>>> list(Authentication auth) {
        UserSessionDto user = getCurrentUser(auth);
        String userRole = user != null ? user.getRole() : "viewer";
        List<RoleResponseDto> roles = roleService.listRoles(userRole);
        return ResponseEntity.ok(ApiResponse.success("Daftar peran berhasil dimuat", roles));
    }

    @Operation(summary = "List all available system permissions", description = "Mengambil kamus seluruh hak akses yang tersedia, dikelompokkan per modul")
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionGroupDto>>> listPermissions() {
        List<PermissionGroupDto> groups = roleService.listPermissionsGrouped();
        return ResponseEntity.ok(ApiResponse.success("Kamus izin berhasil dimuat", groups));
    }

    @Operation(summary = "Get role detail with active permissions", description = "Mengambil detail peran beserta daftar kode izin yang aktif")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDetailDto>> getRole(@PathVariable UUID id) {
        RoleDetailDto detail = roleService.getRoleDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Detail role berhasil dimuat", detail));
    }

    @Operation(summary = "Create new custom role", description = "Membuat peran kustom baru")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponseDto>> create(@RequestBody CreateRoleInput input) {
        RoleResponseDto created = roleService.createRole(input);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role baru berhasil dibuat", created));
    }

    @Operation(summary = "Update role information", description = "Memperbarui nama dan deskripsi peran")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable UUID id, @RequestBody UpdateRoleInput input) {
        roleService.updateRole(id, input);
        return ResponseEntity.ok(ApiResponse.success("Informasi role berhasil diperbarui", null));
    }

    @Operation(summary = "Delete custom role", description = "Menghapus peran kustom jika tidak ada pengguna yang menggunakannya")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role berhasil dihapus", null));
    }

    @Operation(summary = "Update role permissions matrix", description = "Memperbarui matriks hak akses yang diberikan kepada peran")
    @PutMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<Void>> updatePermissions(@PathVariable UUID id, @RequestBody UpdatePermissionsInput input) {
        roleService.updateRolePermissions(id, input != null ? input.getPermissions() : List.of());
        return ResponseEntity.ok(ApiResponse.success("Matriks hak akses role berhasil diperbarui", null));
    }
}

