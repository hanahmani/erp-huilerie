package com.huilerie.erp.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Vérifie que les migrations s'appliquent et que seul le référentiel technique est présent. */
@SpringBootTest
@ActiveProfiles("test")
class MigrationsTest {

    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;

    @Test
    void lesRolesTechniquesSontPresents() {
        assertThat(roleRepository.findByCode("ADMIN")).isPresent();
        assertThat(roleRepository.findByCode("CAISSIER")).isPresent();
        assertThat(permissionRepository.count()).isGreaterThan(20);
    }

    @Test
    void ladministrateurPossedeToutesLesPermissions() {
        Role admin = roleRepository.findByCode("ADMIN").orElseThrow();
        assertThat(admin.getPermissions()).hasSize((int) permissionRepository.count());
    }
}
