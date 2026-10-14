package com.huilerie.erp.auth.dto;

import com.huilerie.erp.users.AppUser;

import java.util.Set;

public record MeResponse(Long id, String username, String firstName, String lastName,
                         boolean mustChangePassword, Set<String> roles, Set<String> permissions) {

    public static MeResponse from(AppUser u) {
        return new MeResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                u.isMustChangePassword(), u.roleCodes(),
                u.isMustChangePassword() ? Set.of() : u.permissionCodes());
    }
}
