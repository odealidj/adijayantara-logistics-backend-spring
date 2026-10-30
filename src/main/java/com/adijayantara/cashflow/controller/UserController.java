package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.config.SessionAuthenticationToken;
import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Manajemen data akun staf dan hak otorisasi")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    private UserSessionDto getCurrentUser(Authentication auth) {
        if (auth instanceof SessionAuthenticationToken sat) {
            return sat.getPrincipal();
        }
        return null;
    }

    @Operation(summary = "List users", description = "Mengambil daftar staf dengan filter nama/email, peran, status, dan paginasi")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(name = "sort_by", defaultValue = "created_at") String sortBy,
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
            Authentication auth) {

        UserSessionDto currentUser = getCurrentUser(auth);
        String currentUserRole = currentUser != null ? currentUser.getRole() : "viewer";

        Map<String, Object> result = userService.list(page, limit, search, role, status, currentUserRole, sortBy, sortDir);
        @SuppressWarnings("unchecked")
        List<UserResponseDto> items = (List<UserResponseDto>) result.get("data");
        long total = (long) result.get("total");

        return ResponseEntity.ok(ApiResponse.paginated("Daftar pengguna berhasil dimuat", items, page, limit, total));
    }

    @Operation(summary = "Get user by ID", description = "Mengambil profil pengguna berdasarkan ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> get(@PathVariable UUID id) {
        UserResponseDto user = userService.getByID(id);
        return ResponseEntity.ok(ApiResponse.success("Data pengguna berhasil dimuat", user));
    }

    @Operation(summary = "Create new user", description = "Mendaftarkan staf baru beserta penugasan peran")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> create(@RequestBody CreateUserInput input, Authentication auth) {
        UserSessionDto currentUser = getCurrentUser(auth);
        String currentUserRole = currentUser != null ? currentUser.getRole() : "viewer";

        UserResponseDto created = userService.create(input, currentUserRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pengguna baru berhasil ditambahkan", created));
    }

    @Operation(summary = "Update user", description = "Memperbarui profil, peran, atau status pengguna")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(
            @PathVariable UUID id,
            @RequestBody UpdateUserInput input,
            Authentication auth) {

        UserSessionDto currentUser = getCurrentUser(auth);
        UUID currentUserId = currentUser != null ? currentUser.getUserId() : null;
        String currentUserRole = currentUser != null ? currentUser.getRole() : "viewer";

        UserResponseDto updated = userService.update(id, input, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.success("Data pengguna berhasil diperbarui", updated));
    }

    @Operation(summary = "Reset user password", description = "Mereset password akun pengguna")
    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @RequestBody ResetPasswordRequest req,
            Authentication auth) {

        UserSessionDto currentUser = getCurrentUser(auth);
        UUID currentUserId = currentUser != null ? currentUser.getUserId() : null;
        String currentUserRole = currentUser != null ? currentUser.getRole() : "viewer";

        userService.resetPassword(id, req.getPassword(), currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.success("Password pengguna berhasil direset", null));
    }

    @Operation(summary = "Soft delete / deactivate user", description = "Menonaktifkan akun staf")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, Authentication auth) {
        UserSessionDto currentUser = getCurrentUser(auth);
        UUID currentUserId = currentUser != null ? currentUser.getUserId() : null;
        String currentUserRole = currentUser != null ? currentUser.getRole() : "viewer";

        userService.delete(id, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.success("Pengguna berhasil dinonaktifkan/dihapus", null));
    }
}

