package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.config.SessionAuthenticationToken;
import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.NotificationResponseDto;
import com.adijayantara.cashflow.dto.UserSessionDto;
import com.adijayantara.cashflow.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Pusat notifikasi dan alert operasional finansial")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    private UserSessionDto getCurrentUser(Authentication auth) {
        if (auth instanceof SessionAuthenticationToken sat) {
            return sat.getPrincipal();
        }
        return null;
    }

    @Operation(summary = "Get notifications feed", description = "Mengambil feed notifikasi dengan filter penerima, kategori, dan urgensi")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String search,
            Authentication auth) {

        UserSessionDto user = getCurrentUser(auth);
        UUID userId = user != null ? user.getUserId() : null;
        String userRole = user != null ? user.getRole() : "viewer";

        Map<String, Object> result = notificationService.list(userId, userRole, page, limit, category, severity, unreadOnly, search);
        @SuppressWarnings("unchecked")
        List<NotificationResponseDto> items = (List<NotificationResponseDto>) result.get("data");
        long total = (long) result.get("total");

        return ResponseEntity.ok(ApiResponse.paginated("Daftar notifikasi berhasil diambil", items, page, limit, total));
    }

    @Operation(summary = "Get unread count", description = "Menghitung jumlah notifikasi yang belum dibaca")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(Authentication auth) {
        UserSessionDto user = getCurrentUser(auth);
        UUID userId = user != null ? user.getUserId() : null;
        String userRole = user != null ? user.getRole() : "viewer";

        int unreadCount = notificationService.getUnreadCount(userId, userRole);
        return ResponseEntity.ok(ApiResponse.success("Jumlah notifikasi belum dibaca berhasil diambil", Map.of("unread_count", unreadCount)));
    }

    @Operation(summary = "Mark notification as read", description = "Menandai sebuah notifikasi telah dibaca")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id, Authentication auth) {
        UserSessionDto user = getCurrentUser(auth);
        UUID userId = user != null ? user.getUserId() : null;

        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notifikasi telah ditandai sebagai dibaca", null));
    }

    @Operation(summary = "Mark all notifications as read", description = "Menandai seluruh notifikasi yang belum dibaca menjadi dibaca")
    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication auth) {
        UserSessionDto user = getCurrentUser(auth);
        UUID userId = user != null ? user.getUserId() : null;
        String userRole = user != null ? user.getRole() : "viewer";

        notificationService.markAllAsRead(userId, userRole);
        return ResponseEntity.ok(ApiResponse.success("Semua notifikasi berhasil ditandai sebagai telah dibaca", null));
    }

    @Operation(summary = "Delete notification", description = "Menghapus notifikasi")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Notifikasi berhasil dihapus", null));
    }

    @Operation(summary = "Trigger check invoices", description = "Memindai jatuh tempo seluruh invoice dan menerbitkan notifikasi H-3, Hari-H, atau Overdue")
    @PostMapping("/check-invoices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkInvoices() {
        int count = notificationService.checkInvoiceDueDates();
        return ResponseEntity.ok(ApiResponse.success("Pemindaian jatuh tempo invoice berhasil diproses", Map.of("dispatched_count", count)));
    }
}

