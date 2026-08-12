package com.shiftsync.security;

import com.shiftsync.staff.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Wraps an AppUser for Spring Security. Carries businessId explicitly so
 * every authenticated request has its tenant available WITHOUT trusting
 * anything the client sent — the JWT is the only source of truth for which
 * tenant a request belongs to.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID businessId;
    private final String email;
    private final String passwordHash;
    private final String role;

    public UserPrincipal(AppUser user) {
        this.userId = user.getId();
        this.businessId = user.getBusinessId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();
    }

    /** Used when reconstructing a principal from JWT claims (no DB hit needed per request). */
    public UserPrincipal(UUID userId, UUID businessId, String email, String role) {
        this.userId = userId;
        this.businessId = businessId;
        this.email = email;
        this.role = role;
        this.passwordHash = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}