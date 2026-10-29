package com.adijayantara.cashflow.config;

import com.adijayantara.cashflow.dto.UserSessionDto;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SessionAuthenticationToken extends AbstractAuthenticationToken {

    private final UserSessionDto principal;

    public SessionAuthenticationToken(UserSessionDto principal) {
        super(extractAuthorities(principal));
        this.principal = principal;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> extractAuthorities(UserSessionDto session) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (session.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + session.getRole().toUpperCase()));
        }
        if (session.getPermissions() != null) {
            for (String perm : session.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public UserSessionDto getPrincipal() {
        return principal;
    }
}

