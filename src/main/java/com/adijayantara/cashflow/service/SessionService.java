package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.User;
import com.adijayantara.cashflow.dto.UserSessionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Membuat token sesi acak aman berformat 'sess_' + 32-byte hex string (kompatibel penuh dengan Go)
     */
    public String generateSessionToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "sess_" + HexFormat.of().formatHex(randomBytes);
    }

    /**
     * Menyimpan sesi ke Redis pada key 'session:sess_...' dan 'user_sessions:{userId}'
     */
    public UserSessionDto createSession(User user, List<String> permissions) {
        String token = generateSessionToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_TTL);

        UserSessionDto sessionDto = UserSessionDto.builder()
                .sessionId(token)
                .userId(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .roleId(user.getRoleEntity() != null ? user.getRoleEntity().getId() : null)
                .status(user.getStatus().name())
                .permissions(permissions)
                .createdAt(DateTimeFormatter.ISO_INSTANT.format(now))
                .expiresAt(DateTimeFormatter.ISO_INSTANT.format(expiresAt))
                .build();

        try {
            String jsonPayload = objectMapper.writeValueAsString(sessionDto);
            String sessionKey = SESSION_KEY_PREFIX + token;
            String userSessionsKey = USER_SESSIONS_PREFIX + user.getId().toString();

            redisTemplate.opsForValue().set(sessionKey, jsonPayload, SESSION_TTL);
            redisTemplate.opsForSet().add(userSessionsKey, token);
            redisTemplate.expire(userSessionsKey, SESSION_TTL);

            log.debug("Session created successfully in Redis for user: {} with token prefix: {}", user.getEmail(), token.substring(0, 10));
            return sessionDto;
        } catch (Exception e) {
            log.error("Failed to serialize and save session to Redis", e);
            throw new RuntimeException("Gagal menyimpan sesi ke Redis", e);
        }
    }

    /**
     * Mengambil dan memvalidasi sesi dari Redis
     */
    public Optional<UserSessionDto> getSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String sessionKey = SESSION_KEY_PREFIX + token.trim();
        String jsonPayload = redisTemplate.opsForValue().get(sessionKey);

        if (jsonPayload == null || jsonPayload.isBlank()) {
            return Optional.empty();
        }

        try {
            UserSessionDto sessionDto = objectMapper.readValue(jsonPayload, UserSessionDto.class);
            return Optional.of(sessionDto);
        } catch (Exception e) {
            log.error("Failed to deserialize session payload from Redis for token: {}", token, e);
            return Optional.empty();
        }
    }

    /**
     * Menghapus sesi spesifik saat logout
     */
    public void deleteSession(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        getSession(token).ifPresent(session -> {
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId().toString();
            redisTemplate.opsForSet().remove(userSessionsKey, token);
        });

        String sessionKey = SESSION_KEY_PREFIX + token.trim();
        redisTemplate.delete(sessionKey);
        log.debug("Session revoked in Redis for token: {}", token);
    }

    /**
     * Menghapus seluruh sesi aktif milik seorang user
     */
    public void deleteAllUserSessions(UUID userId) {
        String userSessionsKey = USER_SESSIONS_PREFIX + userId.toString();
        var sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            for (String sId : sessionIds) {
                redisTemplate.delete(SESSION_KEY_PREFIX + sId);
            }
        }
        redisTemplate.delete(userSessionsKey);
    }
}
