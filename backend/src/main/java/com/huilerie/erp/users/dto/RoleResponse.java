package com.huilerie.erp.users.dto;

import com.huilerie.erp.users.Role;

import java.util.Set;
import java.util.TreeSet;

public record RoleResponse(Long id, String code, String name, String description, boolean systeme,
                           Set<String> permissions) {

    public static RoleResponse from(Role r) {
        Set<String> perms = new TreeSet<>();
        r.getPermissions().forEach(p -> perms.add(p.getCode()));
        return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isSysteme(), perms);
    }
}
