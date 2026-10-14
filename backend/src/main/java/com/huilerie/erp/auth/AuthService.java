package com.huilerie.erp.auth;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.auth.dto.LoginResponse;
import com.huilerie.erp.auth.dto.MeResponse;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.validation.PasswordPolicy;
import com.huilerie.erp.security.JwtService;
import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /** Message volontairement identique dans tous les cas d'échec. */
    private static final String INVALID = "Identifiant ou mot de passe incorrect.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public LoginResponse login(String rawUsername, String password) {
        String username = rawUsername.trim().toLowerCase();
        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            auditService.log(username, "LOGIN_FAILURE", "User", null, null);
            throw new BadCredentialsException(INVALID);
        }
        user.markLogin();
        userRepository.save(user);
        auditService.log(username, "LOGIN_SUCCESS", "User", String.valueOf(user.getId()), null);
        return new LoginResponse(jwtService.issue(user), jwtService.ttlSeconds(), MeResponse.from(user));
    }

    @Transactional(readOnly = true)
    public MeResponse me(String username) {
        return MeResponse.from(findActive(username));
    }

    /** Change le mot de passe et renvoie un nouveau jeton (les anciens deviennent invalides). */
    @Transactional
    public LoginResponse changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = findActive(username);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("Le mot de passe actuel est incorrect.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'actuel.");
        }
        PasswordPolicy.check(newPassword);
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);
        auditService.log(username, "PASSWORD_CHANGE", "User", String.valueOf(user.getId()), null);
        return new LoginResponse(jwtService.issue(user), jwtService.ttlSeconds(), MeResponse.from(user));
    }

    private AppUser findActive(String username) {
        return userRepository.findByUsername(username)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new BadCredentialsException("Compte inactif ou introuvable."));
    }
}
