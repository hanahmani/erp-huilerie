package com.huilerie.erp.security;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.validation.PasswordPolicy;
import com.huilerie.erp.common.validation.ValidationPatterns;
import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.Role;
import com.huilerie.erp.users.RoleRepository;
import com.huilerie.erp.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Crée le tout premier administrateur, UNE seule fois, quand la table users est vide.
 * Identifiant et mot de passe proviennent de variables d'environnement (aucun mot de passe par défaut).
 * L'administrateur devra changer ce mot de passe provisoire à sa première connexion.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final String username;
    private final String password;
    private final String firstName;
    private final String lastName;

    public AdminBootstrap(UserRepository userRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder, AuditService auditService,
                          @Value("${app.bootstrap.admin-username:}") String username,
                          @Value("${app.bootstrap.admin-password:}") String password,
                          @Value("${app.bootstrap.admin-first-name:Administrateur}") String firstName,
                          @Value("${app.bootstrap.admin-last-name:Principal}") String lastName) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.username = username == null ? "" : username.trim().toLowerCase();
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            if (!username.isEmpty()) {
                log.warn("ADMIN_INITIAL_* est encore défini alors que des utilisateurs existent : retirez ces variables.");
            }
            return;
        }
        if (username.isEmpty() || password == null || password.isEmpty()) {
            log.warn("Aucun utilisateur en base. Définissez ADMIN_INITIAL_USERNAME et ADMIN_INITIAL_PASSWORD "
                    + "puis redémarrez pour créer le premier administrateur.");
            return;
        }
        if (!username.matches(ValidationPatterns.USERNAME)) {
            throw new IllegalStateException("ADMIN_INITIAL_USERNAME invalide (minuscules, chiffres, . _ -).");
        }
        PasswordPolicy.check(password);

        Role admin = roleRepository.findByCode(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rôle ADMIN absent : migrations Flyway non appliquées ?"));
        AppUser user = userRepository.save(new AppUser(username, firstName, lastName,
                passwordEncoder.encode(password), Set.of(admin), true));
        auditService.log("system", "ADMIN_BOOTSTRAP", "User", String.valueOf(user.getId()), null);
        log.info("Premier administrateur « {} » créé. Changez le mot de passe à la première connexion, "
                + "puis retirez ADMIN_INITIAL_PASSWORD de l'environnement.", username);
    }
}
