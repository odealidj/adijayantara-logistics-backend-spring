package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Role;
import com.adijayantara.cashflow.domain.entity.User;
import com.adijayantara.cashflow.domain.enums.UserRole;
import com.adijayantara.cashflow.domain.enums.UserStatus;
import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.repository.RoleRepository;
import com.adijayantara.cashflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcClient jdbcClient;

    public Map<String, Object> list(int page, int limit, String search, String role, String status,
                                    String currentUserRole, String sortBy, String sortDir) {
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 15;
        int offset = (page - 1) * limit;

        boolean isSuperAdmin = "super_admin".equalsIgnoreCase(currentUserRole);

        StringBuilder where = new StringBuilder(" WHERE u.deleted_at IS NULL ");
        Map<String, Object> params = new HashMap<>();

        if (!isSuperAdmin) {
            where.append(" AND u.role::text != 'super_admin' ");
        }

        if (search != null && !search.isBlank()) {
            where.append(" AND (u.full_name ILIKE :search OR u.email ILIKE :search OR u.phone ILIKE :search) ");
            params.put("search", "%" + search.trim() + "%");
        }

        if (role != null && !role.isBlank()) {
            where.append(" AND (u.role::text = :role OR r.code = :role) ");
            params.put("role", role.trim());
        }

        if (status != null && !status.isBlank()) {
            where.append(" AND u.status::text = :status ");
            params.put("status", status.trim().toUpperCase());
        }

        String countSql = "SELECT COUNT(*) FROM users u LEFT JOIN roles r ON u.role_id = r.id " + where;
        var countQuery = jdbcClient.sql(countSql);
        params.forEach(countQuery::param);
        long total = countQuery.query(Long.class).single();

        String orderCol = "u.created_at";
        if ("full_name".equalsIgnoreCase(sortBy)) orderCol = "u.full_name";
        else if ("email".equalsIgnoreCase(sortBy)) orderCol = "u.email";
        else if ("status".equalsIgnoreCase(sortBy)) orderCol = "u.status";

        String direction = "ASC".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        String dataSql = """
            SELECT u.id, u.email, u.full_name, u.phone, u.role::text as role,
                   u.role_id, r.name as role_name, u.status::text as status,
                   u.last_login_at, u.created_at, u.updated_at
            FROM users u
            LEFT JOIN roles r ON u.role_id = r.id
            """ + where + " ORDER BY " + orderCol + " " + direction + " LIMIT :limit OFFSET :offset";

        params.put("limit", limit);
        params.put("offset", offset);

        var dataQuery = jdbcClient.sql(dataSql);
        params.forEach(dataQuery::param);

        List<UserResponseDto> items = dataQuery.query((rs, rowNum) -> {
            Timestamp lastLogin = rs.getTimestamp("last_login_at");
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");

            return UserResponseDto.builder()
                    .id((UUID) rs.getObject("id"))
                    .email(rs.getString("email"))
                    .fullName(rs.getString("full_name"))
                    .phone(rs.getString("phone"))
                    .role(rs.getString("role"))
                    .roleId((UUID) rs.getObject("role_id"))
                    .roleName(rs.getString("role_name"))
                    .status(rs.getString("status"))
                    .lastLoginAt(lastLogin != null ? lastLogin.toLocalDateTime() : null)
                    .createdAt(createdAt != null ? createdAt.toLocalDateTime() : null)
                    .updatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null)
                    .build();
        }).list();

        return Map.of(
            "data", items,
            "total", total,
            "page", page,
            "limit", limit
        );
    }

    public UserResponseDto getByID(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan"));

        return toDto(user);
    }

    @Transactional
    public UserResponseDto create(CreateUserInput input, String currentUserRole) {
        if (input.getFullName() == null || input.getFullName().isBlank() ||
            input.getEmail() == null || input.getEmail().isBlank() ||
            input.getPassword() == null || input.getPassword().isBlank()) {
            throw new IllegalArgumentException("nama lengkap, email, dan password wajib diisi");
        }

        if (input.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password minimal 6 karakter");
        }

        String email = input.getEmail().trim().toLowerCase();
        if (userRepository.findByEmailAndDeletedAtIsNull(email).isPresent()) {
            throw new IllegalArgumentException("Email sudah terdaftar pada akun lain yang aktif");
        }

        String roleCode = input.getRole() != null && !input.getRole().isBlank() ? input.getRole().trim().toLowerCase() : "finance";

        // Proteksi: Hanya Super Admin yang bisa membuat akun Super Admin
        if ("super_admin".equalsIgnoreCase(roleCode) && !"super_admin".equalsIgnoreCase(currentUserRole)) {
            throw new IllegalArgumentException("Anda tidak memiliki wewenang untuk menetapkan role Super Admin IT");
        }

        // C-Level Immunity: Admin biasa tidak boleh membuat akun Direktur/Owner
        if ("admin".equalsIgnoreCase(currentUserRole) &&
            ("direktur".equalsIgnoreCase(roleCode) || "owner".equalsIgnoreCase(roleCode))) {
            throw new IllegalArgumentException("Administrator biasa tidak memiliki wewenang untuk membuat akun Direktur atau Pemilik Perusahaan");
        }

        UserRole userRoleEnum;
        try {
            userRoleEnum = UserRole.valueOf(roleCode);
        } catch (IllegalArgumentException e) {
            userRoleEnum = UserRole.viewer;
        }

        UserStatus userStatusEnum = UserStatus.ACTIVE;
        if (input.getStatus() != null && !input.getStatus().isBlank()) {
            try {
                userStatusEnum = UserStatus.valueOf(input.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Role roleEntity = roleRepository.findByCode(roleCode).orElse(null);

        User user = User.builder()
                .email(email)
                .phone(input.getPhone() != null && !input.getPhone().isBlank() ? input.getPhone().trim() : null)
                .fullName(input.getFullName().trim())
                .passwordHash(passwordEncoder.encode(input.getPassword()))
                .role(userRoleEnum)
                .roleEntity(roleEntity)
                .status(userStatusEnum)
                .build();

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    @Transactional
    public UserResponseDto update(UUID id, UpdateUserInput input, UUID currentUserId, String currentUserRole) {
        User targetUser = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan"));

        String targetRoleCode = targetUser.getRole().name();
        String newRoleCode = input.getRole() != null && !input.getRole().isBlank() ? input.getRole().trim().toLowerCase() : targetRoleCode;
        String newStatusStr = input.getStatus() != null && !input.getStatus().isBlank() ? input.getStatus().trim().toUpperCase() : targetUser.getStatus().name();

        // Proteksi: Super Admin kebal dari modifikasi oleh siapapun selain Super Admin
        if ("super_admin".equalsIgnoreCase(targetRoleCode) && !"super_admin".equalsIgnoreCase(currentUserRole)) {
            throw new IllegalArgumentException("akun Super Admin IT memiliki proteksi khusus dan tidak dapat diubah oleh Administrator Bisnis");
        }

        // Proteksi: Tidak boleh mempromosikan user menjadi Super Admin jika bukan Super Admin
        if ("super_admin".equalsIgnoreCase(newRoleCode) && !"super_admin".equalsIgnoreCase(currentUserRole)) {
            throw new IllegalArgumentException("Anda tidak memiliki wewenang untuk menetapkan role Super Admin IT");
        }

        // C-Level Immunity: Admin biasa tidak boleh mengubah akun Direktur atau Owner
        if ("admin".equalsIgnoreCase(currentUserRole) &&
            ("direktur".equalsIgnoreCase(targetRoleCode) || "owner".equalsIgnoreCase(targetRoleCode))) {
            throw new IllegalArgumentException("Administrator tidak memiliki wewenang untuk mengubah data akun Direktur maupun Pemilik Perusahaan");
        }

        // Last Admin Standing Protection
        if ("admin".equalsIgnoreCase(targetRoleCode) &&
            (!"ACTIVE".equalsIgnoreCase(newStatusStr) || !"admin".equalsIgnoreCase(newRoleCode))) {
            long activeAdmins = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE role::text = 'admin' AND status::text = 'ACTIVE' AND deleted_at IS NULL")
                    .query(Long.class).single();
            if (activeAdmins <= 1) {
                throw new IllegalArgumentException("tidak dapat menonaktifkan atau mengubah peran Administrator terakhir. Tunjuk Administrator lain terlebih dahulu untuk kelangsungan sistem.");
            }
        }

        boolean roleOrStatusChanged = !targetRoleCode.equalsIgnoreCase(newRoleCode) ||
                                      !targetUser.getStatus().name().equalsIgnoreCase(newStatusStr);

        if (input.getEmail() != null && !input.getEmail().isBlank()) {
            String newEmail = input.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(targetUser.getEmail())) {
                if (userRepository.findByEmailAndDeletedAtIsNull(newEmail).isPresent()) {
                    throw new IllegalArgumentException("Email sudah digunakan oleh akun lain");
                }
                targetUser.setEmail(newEmail);
            }
        }

        if (input.getFullName() != null && !input.getFullName().isBlank()) {
            targetUser.setFullName(input.getFullName().trim());
        }

        if (input.getPhone() != null) {
            String newPhone = input.getPhone().trim();
            if (newPhone.isBlank()) {
                targetUser.setPhone(null);
            } else if (!newPhone.equalsIgnoreCase(targetUser.getPhone())) {
                if (userRepository.findByPhoneAndDeletedAtIsNull(newPhone).isPresent()) {
                    throw new IllegalArgumentException("Nomor telepon sudah digunakan oleh akun lain");
                }
                targetUser.setPhone(newPhone);
            }
        }

        if (!targetRoleCode.equalsIgnoreCase(newRoleCode)) {
            try {
                targetUser.setRole(UserRole.valueOf(newRoleCode));
            } catch (IllegalArgumentException e) {
                targetUser.setRole(UserRole.viewer);
            }
            Role roleEntity = roleRepository.findByCode(newRoleCode).orElse(null);
            targetUser.setRoleEntity(roleEntity);
        }

        if (!targetUser.getStatus().name().equalsIgnoreCase(newStatusStr)) {
            try {
                targetUser.setStatus(UserStatus.valueOf(newStatusStr));
            } catch (IllegalArgumentException ignored) {}
        }

        User updated = userRepository.save(targetUser);

        // Revoke active sessions if role or status was modified
        if (roleOrStatusChanged) {
            sessionService.deleteAllUserSessions(id);
        }

        return toDto(updated);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword, UUID currentUserId, String currentUserRole) {
        User targetUser = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan"));

        String targetRoleCode = targetUser.getRole().name();

        // Proteksi Super Admin
        if ("super_admin".equalsIgnoreCase(targetRoleCode) && !"super_admin".equalsIgnoreCase(currentUserRole)) {
            throw new IllegalArgumentException("akun Super Admin IT memiliki proteksi khusus dan tidak dapat direset oleh Administrator Bisnis");
        }

        // C-Level Immunity
        if ("admin".equalsIgnoreCase(currentUserRole) &&
            ("direktur".equalsIgnoreCase(targetRoleCode) || "owner".equalsIgnoreCase(targetRoleCode))) {
            throw new IllegalArgumentException("Administrator tidak memiliki wewenang untuk mereset password akun Direktur maupun Pemilik Perusahaan");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password baru minimal 6 karakter");
        }

        targetUser.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(targetUser);

        // Force logout user
        sessionService.deleteAllUserSessions(id);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId, String currentUserRole) {
        // Golden Rule 1: Anti Self-Deletion
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("Anda tidak dapat menghapus atau menonaktifkan akun Anda sendiri");
        }

        User targetUser = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan"));

        String targetRoleCode = targetUser.getRole().name();

        // Proteksi Super Admin
        if ("super_admin".equalsIgnoreCase(targetRoleCode) && !"super_admin".equalsIgnoreCase(currentUserRole)) {
            throw new IllegalArgumentException("akun Super Admin IT memiliki proteksi khusus dan tidak dapat dihapus oleh siapapun");
        }

        // C-Level Immunity
        if ("admin".equalsIgnoreCase(currentUserRole) &&
            ("direktur".equalsIgnoreCase(targetRoleCode) || "owner".equalsIgnoreCase(targetRoleCode))) {
            throw new IllegalArgumentException("Administrator tidak memiliki wewenang untuk menghapus atau menonaktifkan akun Direktur maupun Pemilik Perusahaan");
        }

        // Last Admin Standing Protection
        if ("admin".equalsIgnoreCase(targetRoleCode)) {
            long activeAdmins = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE role::text = 'admin' AND status::text = 'ACTIVE' AND deleted_at IS NULL")
                    .query(Long.class).single();
            if (activeAdmins <= 1) {
                throw new IllegalArgumentException("tidak dapat menghapus atau menonaktifkan akun Administrator terakhir. Tunjuk Administrator lain terlebih dahulu untuk kelangsungan sistem.");
            }
        }

        // Soft-delete
        targetUser.setDeletedAt(LocalDateTime.now());
        targetUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(targetUser);

        // Force logout
        sessionService.deleteAllUserSessions(id);
    }

    private UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .roleId(user.getRoleEntity() != null ? user.getRoleEntity().getId() : null)
                .roleName(user.getRoleEntity() != null ? user.getRoleEntity().getName() : null)
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
