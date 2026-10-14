package com.huilerie.erp.users.dto;

import com.huilerie.erp.users.AppUser;

import java.time.Instant;
import java.util.Set;

public record UserResponse(Long id, String username, String firstName, String lastName, String email,
                           String telephone, String service, boolean active, boolean mustChangePassword,
                           Set<String> roles, Instant createdAt, Instant derniereConnexion) {

    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail(),
                u.getTelephone(), u.getService(), u.isActive(), u.isMustChangePassword(), u.roleCodes(),
                u.getCreatedAt(), u.getDerniereConnexion());
    }
}
