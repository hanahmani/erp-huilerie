package com.huilerie.erp.users;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.validation.PasswordPolicy;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.users.dto.CreateUserRequest;
import com.huilerie.erp.users.dto.UpdateUserRequest;
import com.huilerie.erp.users.dto.RoleResponse;
import com.huilerie.erp.users.dto.UserResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByLastNameAscFirstNameAsc().stream().map(UserResponse::from).toList();
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAllByOrderByNameAsc().stream().map(RoleResponse::from).toList();
    }

    /**
     * Crée un utilisateur. Il n'existe aucune inscription libre : seule une personne ayant
     * USER_MANAGE peut créer un compte, et seul un administrateur peut attribuer le rôle ADMIN.
     */
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request, String actorUsername) {
        AppUser actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur courant introuvable."));

        String username = request.username().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Cet identifiant est déjà utilisé.");
        }

        Set<String> requestedRoles = new HashSet<>(request.roles());
        if (requestedRoles.contains(Role.ADMIN) && !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Seul un administrateur peut créer un compte administrateur.");
        }

        List<Role> roles = roleRepository.findByCodeIn(requestedRoles);
        if (roles.size() != requestedRoles.size()) {
            throw new BusinessException("Un ou plusieurs rôles sont inconnus.");
        }

        PasswordPolicy.check(request.temporaryPassword());

        AppUser user = new AppUser(username, request.firstName().trim(), request.lastName().trim(),
                passwordEncoder.encode(request.temporaryPassword()), new HashSet<>(roles), true);
        user.updateProfile(request.firstName().trim(), request.lastName().trim(), Text.clean(request.email()),
                Text.clean(request.telephone()), Text.clean(request.service()));
        user = userRepository.save(user);

        auditService.log(actorUsername, "USER_CREATE", "User", String.valueOf(user.getId()),
                "Rôles : " + user.roleCodes());
        return UserResponse.from(user);
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public UserResponse updateStatus(Long userId, boolean active, String actorUsername) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        if (user.getUsername().equals(actorUsername) && !active) {
            throw new BusinessException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        AppUser actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur courant introuvable."));
        if (user.hasRole(Role.ADMIN) && !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Seul un administrateur peut modifier un administrateur.");
        }
        if (!active && user.hasRole(Role.ADMIN) && userRepository.countActiveWithRole(Role.ADMIN) <= 1) {
            throw new BusinessException("Impossible de désactiver le dernier administrateur actif.");
        }
        user.setActive(active);
        auditService.log(actorUsername, active ? "USER_ACTIVATE" : "USER_DEACTIVATE",
                "User", String.valueOf(user.getId()), null);
        return UserResponse.from(user);
    }

    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable.")));
    }

    /** Modification de la fiche et des rôles, avec les mêmes garde-fous que la création. */
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest r, String actorUsername) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        AppUser actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur courant introuvable."));
        Set<String> requested = new HashSet<>(r.roles());
        boolean touchesAdmin = requested.contains(Role.ADMIN) != user.hasRole(Role.ADMIN);
        if ((touchesAdmin || user.hasRole(Role.ADMIN)) && !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Seul un administrateur peut modifier un administrateur.");
        }
        if (user.hasRole(Role.ADMIN) && !requested.contains(Role.ADMIN)
                && userRepository.countActiveWithRole(Role.ADMIN) <= 1) {
            throw new BusinessException("Impossible de retirer le rôle du dernier administrateur.");
        }
        List<Role> roles = roleRepository.findByCodeIn(requested);
        if (roles.size() != requested.size()) {
            throw new BusinessException("Un ou plusieurs rôles sont inconnus.");
        }
        user.updateProfile(r.firstName().trim(), r.lastName().trim(), Text.clean(r.email()),
                Text.clean(r.telephone()), Text.clean(r.service()));
        if (!user.roleCodes().equals(new java.util.TreeSet<>(requested))) {
            user.replaceRoles(new HashSet<>(roles));
        }
        auditService.log(actorUsername, "USER_UPDATE", "User", String.valueOf(id), "Rôles : " + user.roleCodes());
        return UserResponse.from(user);
    }

    /** Mot de passe provisoire fixé par l'administrateur : l'utilisateur devra le changer. */
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public UserResponse resetPassword(Long id, String temporaryPassword, String actorUsername) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        AppUser actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new AccessDeniedException("Utilisateur courant introuvable."));
        if (user.hasRole(Role.ADMIN) && !actor.hasRole(Role.ADMIN)) {
            throw new AccessDeniedException("Seul un administrateur peut réinitialiser un administrateur.");
        }
        PasswordPolicy.check(temporaryPassword);
        user.resetPassword(passwordEncoder.encode(temporaryPassword));
        auditService.log(actorUsername, "PASSWORD_RESET", "User", String.valueOf(id), null);
        return UserResponse.from(user);
    }

    /** Suppression physique seulement pour un compte jamais utilisé ; sinon, désactiver. */
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public void delete(Long id, String actorUsername) {
        AppUser user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        if (user.getUsername().equals(actorUsername)) {
            throw new BusinessException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        if (user.getDerniereConnexion() != null) {
            throw new BusinessException("Ce compte a déjà été utilisé : désactivez-le pour conserver l'historique.");
        }
        if (user.hasRole(Role.ADMIN)) {
            throw new BusinessException("Un administrateur ne peut pas être supprimé.");
        }
        userRepository.delete(user);
        auditService.log(actorUsername, "USER_DELETE", "User", String.valueOf(id), user.getUsername());
    }
}
