package com.vidyapeet.security;

import com.vidyapeet.common.exception.Exceptions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helpers for reading the authenticated principal from the security context. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw Exceptions.unauthorized("No authenticated user in context.");
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentUser().getUserId();
    }

    public static Long currentInstituteId() {
        return currentUser().getInstituteId();
    }
}
