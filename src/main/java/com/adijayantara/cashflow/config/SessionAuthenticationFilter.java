package com.adijayantara.cashflow.config;

import com.adijayantara.cashflow.dto.UserSessionDto;
import com.adijayantara.cashflow.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_AUTH_NAME = "auth_session";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && !token.isBlank()) {
            Optional<UserSessionDto> sessionOpt = sessionService.getSession(token);
            if (sessionOpt.isPresent()) {
                UserSessionDto session = sessionOpt.get();
                SessionAuthenticationToken authentication = new SessionAuthenticationToken(session);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.trace("Authenticated user {} via Redis session token", session.getEmail());
            } else {
                log.trace("Token provided but no active session found in Redis: {}", token);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Cek dari HttpOnly Cookie 'auth_session' (Persis Go backend)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_AUTH_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue().trim();
                }
            }
        }

        // 2. Fallback ke Header Authorization: Bearer <token>
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        return null;
    }
}
