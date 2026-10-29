package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Permission;
import com.adijayantara.cashflow.domain.entity.Role;
import com.adijayantara.cashflow.dto.*;
import com.adijayantara.cashflow.repository.PermissionRepository;
import com.adijayantara.cashflow.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final JdbcClient jdbcClient;

    public List<RoleResponseDto> listRoles(String currentUserRole) {
        String sql = """
            SELECT r.id, r.code, r.name, r.description, r.is_system, r.created_at, r.updated_at,
                   COALESCE(COUNT(u.id), 0) AS user_count
            FROM roles r
            LEFT JOIN users u ON u.role_id = r.id AND u.deleted_at IS NULL
            GROUP BY r.id, r.code, r.name, r.description, r.is_system, r.created_at, r.updated_at
            ORDER BY r.is_system DESC, r.name ASC
        """;

        return jdbcClient.sql(sql).query((rs, rowNum) -> {
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            return RoleResponseDto.builder()
                    .id((UUID) rs.getObject("id"))
                    .code(rs.getString("code"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .isSystem(rs.getBoolean("is_system"))
                    .userCount(rs.getLong("user_count"))
                    .createdAt(createdAt != null ? createdAt.toLocalDateTime() : null)
                    .updatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null)
                    .build();
        }).list();
    }

    public RoleDetailDto getRoleDetail(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role tidak ditemukan"));

        String countSql = "SELECT COUNT(*) FROM users WHERE role_id = :roleId AND deleted_at IS NULL";
        long userCount = jdbcClient.sql(countSql)
                .param("roleId", id)
                .query(Long.class).single();

        String permSql = """
            SELECT p.code
            FROM role_permissions rp
            JOIN permissions p ON rp.permission_id = p.id
            WHERE rp.role_id = :roleId
            ORDER BY p.code ASC
        """;

        List<String> permissionCodes = jdbcClient.sql(permSql)
                .param("roleId", id)
                .query(String.class).list();

        return RoleDetailDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .userCount(userCount)
                .permissions(permissionCodes)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    @Transactional
    public RoleResponseDto createRole(CreateRoleInput input) {
        if (input.getCode() == null || input.getCode().isBlank()) {
            throw new IllegalArgumentException("Kode role wajib diisi");
        }
        if (input.getName() == null || input.getName().isBlank()) {
            throw new IllegalArgumentException("Nama role wajib diisi");
        }

        String code = input.getCode().trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (roleRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Role dengan kode tersebut sudah terdaftar");
        }

        Role role = Role.builder()
                .code(code)
                .name(input.getName().trim())
                .description(input.getDescription() != null ? input.getDescription().trim() : null)
                .isSystem(false)
                .build();

        Role saved = roleRepository.saveAndFlush(role);

        if (input.getPermissions() != null && !input.getPermissions().isEmpty()) {
            updateRolePermissions(saved.getId(), input.getPermissions());
        }

        return RoleResponseDto.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .name(saved.getName())
                .description(saved.getDescription())
                .isSystem(saved.getIsSystem())
                .userCount(0L)
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional
    public void updateRole(UUID id, UpdateRoleInput input) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role tidak ditemukan"));

        if (input.getName() != null && !input.getName().isBlank()) {
            role.setName(input.getName().trim());
        }
        if (input.getDescription() != null) {
            role.setDescription(input.getDescription().trim());
        }

        roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role tidak ditemukan"));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalArgumentException("Role sistem permanen tidak boleh dihapus");
        }

        String countSql = "SELECT COUNT(*) FROM users WHERE role_id = :roleId AND deleted_at IS NULL";
        long userCount = jdbcClient.sql(countSql)
                .param("roleId", id)
                .query(Long.class).single();

        if (userCount > 0) {
            throw new IllegalArgumentException("Tidak dapat menghapus peran yang masih digunakan oleh " + userCount + " pengguna");
        }

        // Delete mappings first
        jdbcClient.sql("DELETE FROM role_permissions WHERE role_id = :roleId")
                .param("roleId", id)
                .update();

        roleRepository.delete(role);
    }

    public List<PermissionGroupDto> listPermissionsGrouped() {
        String sql = """
            SELECT id, module, code, name, description
            FROM permissions
            ORDER BY module ASC, code ASC
        """;

        List<PermissionGroupDto.PermissionItemDto> allPerms = jdbcClient.sql(sql).query((rs, rowNum) ->
            PermissionGroupDto.PermissionItemDto.builder()
                    .id((UUID) rs.getObject("id"))
                    .module(rs.getString("module"))
                    .code(rs.getString("code"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .build()
        ).list();

        Map<String, List<PermissionGroupDto.PermissionItemDto>> grouped = new LinkedHashMap<>();
        for (var p : allPerms) {
            grouped.computeIfAbsent(p.getModule(), k -> new ArrayList<>()).add(p);
        }

        List<PermissionGroupDto> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            result.add(PermissionGroupDto.builder()
                    .module(entry.getKey())
                    .permissions(entry.getValue())
                    .build());
        }

        return result;
    }

    @Transactional
    public void updateRolePermissions(UUID roleID, List<String> permissionCodes) {
        Role role = roleRepository.findById(roleID)
                .orElseThrow(() -> new IllegalArgumentException("Role tidak ditemukan"));

        if ("super_admin".equalsIgnoreCase(role.getCode())) {
            throw new IllegalArgumentException("Role IT Super Admin kebal mutlak dan memiliki akses penuh (*)");
        }

        // Clear existing permissions
        jdbcClient.sql("DELETE FROM role_permissions WHERE role_id = :roleId")
                .param("roleId", roleID)
                .update();

        if (permissionCodes != null && !permissionCodes.isEmpty()) {
            String insertSql = """
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT :roleId, p.id
                FROM permissions p
                WHERE p.code = :permCode
                ON CONFLICT DO NOTHING
            """;

            for (String code : permissionCodes) {
                if (code != null && !code.isBlank()) {
                    jdbcClient.sql(insertSql)
                            .param("roleId", roleID)
                            .param("permCode", code.trim())
                            .update();
                }
            }
        }
    }
}
