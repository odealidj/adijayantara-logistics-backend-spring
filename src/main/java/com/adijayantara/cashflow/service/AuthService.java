package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Permission;
import com.adijayantara.cashflow.domain.entity.Role;
import com.adijayantara.cashflow.domain.entity.User;
import com.adijayantara.cashflow.domain.enums.UserRole;
import com.adijayantara.cashflow.domain.enums.UserStatus;
import com.adijayantara.cashflow.dto.LoginRequest;
import com.adijayantara.cashflow.dto.LoginResponse;
import com.adijayantara.cashflow.dto.UserSessionDto;
import com.adijayantara.cashflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();

        // Cari berdasarkan Email atau Nomor Telepon aktif
        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhoneAndDeletedAtIsNull(identifier);
        }

        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("kredensial tidak valid");
        }

        User user = userOpt.get();

        // Validasi Status Akun (Persis aturan bisnis Go)
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalStateException("Akun Anda sedang menunggu persetujuan dan aktivasi dari Administrator.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("Akun Anda telah dinonaktifkan atau ditangguhkan oleh Administrator.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Akun Anda tidak aktif. Silakan hubungi Administrator.");
        }

        // Validasi Password BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("kredensial tidak valid");
        }

        // Update timestamp last_login_at
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Kumpulkan Dynamic Permissions
        List<String> permissions = new ArrayList<>();
        if (user.getRole() == UserRole.super_admin) {
            permissions.add("*");
        } else if (user.getRoleEntity() != null && user.getRoleEntity().getPermissions() != null) {
            for (Permission p : user.getRoleEntity().getPermissions()) {
                permissions.add(p.getCode());
            }
        }

        // Buat sesi di Redis (L2 Cache)
        UserSessionDto sessionDto = sessionService.createSession(user, permissions);

        return LoginResponse.builder()
                .token(sessionDto.getSessionId())
                .user(sessionDto)
                .build();
    }

    public void logout(String token) {
        sessionService.deleteSession(token);
    }
}
