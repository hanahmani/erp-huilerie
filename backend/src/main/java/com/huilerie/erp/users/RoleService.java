package com.huilerie.erp.users;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.users.dto.PermissionResponse;
import com.huilerie.erp.users.dto.RoleRequest;
import com.huilerie.erp.users.dto.RoleResponse;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rôles et matrice de permissions. Le rôle ADMIN garde toujours toutes les permissions.
 * Les permissions étant relues à chaque requête, une modification s'applique immédiatement.
 */
@Service
public class RoleService {

    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRepository users;
    private final AuditService audit;

    public RoleService(RoleRepository roles, PermissionRepository permissions, UserRepository users, AuditService audit) {
        this.roles = roles;
        this.permissions = permissions;
        this.users = users;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('ROLE_MANAGE') or hasAuthority('USER_MANAGE')")
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissions.findAll(Sort.by("code")).stream()
                .map(p -> new PermissionResponse(p.getCode(), p.getDescription())).toList();
    }

    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public RoleResponse create(RoleRequest r) {
        if (r.code() == null) {
            throw new BusinessException("Le code du rôle est obligatoire.");
        }
        if (roles.findByCode(r.code()).isPresent()) {
            throw new BusinessException("Un rôle porte déjà ce code.");
        }
        Role role = roles.save(new Role(r.code(), r.name().trim(), Text.clean(r.description()), resolve(r.permissions())));
        audit.log(SecurityUtils.username(), "ROLE_CREATE", "Role", String.valueOf(role.getId()), role.getCode());
        return RoleResponse.from(role);
    }

    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public RoleResponse update(Long id, RoleRequest r) {
        Role role = roles.findById(id).orElseThrow(() -> new NotFoundException("Rôle introuvable."));
        if (Role.ADMIN.equals(role.getCode())) {
            throw new BusinessException("Le rôle Administrateur possède toujours toutes les permissions.");
        }
        Set<Permission> perms = resolve(r.permissions());
        role.update(r.name().trim(), Text.clean(r.description()), perms);
        audit.log(SecurityUtils.username(), "ROLE_UPDATE", "Role", String.valueOf(id),
                role.getCode() + " : " + perms.stream().map(Permission::getCode).sorted().toList());
        return RoleResponse.from(role);
    }

    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public void delete(Long id) {
        Role role = roles.findById(id).orElseThrow(() -> new NotFoundException("Rôle introuvable."));
        if (role.isSysteme()) {
            throw new BusinessException("Un rôle système ne peut pas être supprimé.");
        }
        if (users.countWithRole(id) > 0) {
            throw new BusinessException("Ce rôle est attribué à des utilisateurs : retirez-le d'abord.");
        }
        roles.delete(role);
        audit.log(SecurityUtils.username(), "ROLE_DELETE", "Role", String.valueOf(id), role.getCode());
    }

    private Set<Permission> resolve(Set<String> codes) {
        Set<String> wanted = codes == null ? Set.of() : codes;
        List<Permission> found = permissions.findByCodeIn(wanted);
        if (found.size() != wanted.size()) {
            throw new BusinessException("Une ou plusieurs permissions sont inconnues.");
        }
        return new HashSet<>(found);
    }
}
