package com.huilerie.erp.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication current() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String username() {
        Authentication auth = current();
        return auth == null ? "system" : auth.getName();
    }

    public static boolean has(String authority) {
        Authentication auth = current();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
    }
}
