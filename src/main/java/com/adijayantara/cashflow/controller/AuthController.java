package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.config.SessionAuthenticationFilter;
import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.LoginRequest;
import com.adijayantara.cashflow.dto.LoginResponse;
import com.adijayantara.cashflow.dto.UserSessionDto;
import com.adijayantara.cashflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints autentikasi dan manajemen sesi pengguna")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Login menggunakan email atau nomor telepon dan password")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse loginResponse = authService.login(request);

        // Pasang HttpOnly cookie 'auth_session' (Persis Go backend)
        ResponseCookie cookie = ResponseCookie.from(SessionAuthenticationFilter.COOKIE_AUTH_NAME, loginResponse.getToken())
                .httpOnly(true)
                .secure(false) // Set true in production behind HTTPS
                .path("/")
                .maxAge(86400) // 24 jam
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Login berhasil", loginResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Mencabut token sesi dan menghapus cookie auth_session")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal UserSessionDto session) {

        String token = null;
        if (session != null) {
            token = session.getSessionId();
        } else if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (SessionAuthenticationFilter.COOKIE_AUTH_NAME.equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            authService.logout(token);
        }

        // Hapus cookie
        ResponseCookie cookie = ResponseCookie.from(SessionAuthenticationFilter.COOKIE_AUTH_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(ApiResponse.success("Logout berhasil", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current Profile", description = "Mengambil profil pengguna dan hak akses sesi aktif")
    public ResponseEntity<ApiResponse<UserSessionDto>> me(@AuthenticationPrincipal UserSessionDto session) {
        if (session == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Sesi tidak valid"));
        }
        return ResponseEntity.ok(ApiResponse.success("Sesi pengguna aktif", session));
    }
}
