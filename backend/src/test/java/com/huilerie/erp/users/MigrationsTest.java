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
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void lesRolesTechniquesSontPresents() {
        assertThat(roleRepository.findByCode("ADMIN")).isPresent();
        assertThat(roleRepository.findByCode("CAISSIER")).isPresent();
        assertThat(permissionRepository.count()).isGreaterThan(20);
    }

    @Test
    void laMigrationV9DuBulletinDePressageEstAppliquee() {
        Boolean success = jdbc.queryForObject(
                "select success from flyway_schema_history where version = '9'", Boolean.class);
        assertThat(success).isTrue();
        String nullable = jdbc.queryForObject("select is_nullable from information_schema.columns "
                + "where lower(table_name) = 'pressings' and lower(column_name) = 'numero_bulletin'", String.class);
        assertThat(nullable).isEqualTo("NO");
        Integer columns = jdbc.queryForObject("select count(*) from information_schema.columns "
                + "where lower(table_name) = 'pressings' and lower(column_name) in ('bl_numero', 'transporteur', 'vehicule', 'adresse')",
                Integer.class);
        assertThat(columns).isEqualTo(4);
    }

    @Test
    void ladministrateurPossedeToutesLesPermissions() {
        Role admin = roleRepository.findByCode("ADMIN").orElseThrow();
        assertThat(admin.getPermissions()).hasSize((int) permissionRepository.count());
    }
}
