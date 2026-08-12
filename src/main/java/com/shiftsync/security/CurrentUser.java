package com.shiftsync.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * The ONLY sanctioned way for application code to obtain "which tenant is
 * this request for" — see JwtAuthFilter for why that matters.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static UserPrincipal get() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("No authenticated ShiftSync user in the current security context");
        }
        return userPrincipal;
    }

    public static UUID businessId() {
        return get().getBusinessId();
    }

    public static UUID userId() {
        return get().getUserId();
    }
}