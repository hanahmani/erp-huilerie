package com.huilerie.erp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.PermissionRepository;
import com.huilerie.erp.users.Role;
import com.huilerie.erp.users.RoleRepository;
import com.huilerie.erp.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.huilerie.erp.MockMvcSupport.bearer;
import static com.huilerie.erp.MockMvcSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TestUsers testUsers;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PermissionRepository permissionRepository;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        roleRepository.findByCode("TEST_GESTIONNAIRE").ifPresent(roleRepository::delete);
    }

    private String newUserBody(String username, String... roles) throws Exception {
        return om.writeValueAsString(Map.of(
                "username", username, "firstName", "Test", "lastName", "Utilisateur",
                "temporaryPassword", "Provisoire2026", "roles", List.of(roles)));
    }

    @Test
    void caissierNePeutPasListerLesUtilisateurs() throws Exception {
        testUsers.create("caissier1", false, "CAISSIER");
        String token = login(mvc, om, "caissier1", TestUsers.PASSWORD);

        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCES_REFUSE"));
    }

    @Test
    void administrateurPeutListerEtCreerDesUtilisateurs() throws Exception {
        testUsers.create("admin1", false, "ADMIN");
        String token = login(mvc, om, "admin1", TestUsers.PASSWORD);

        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(newUserBody("vendeur2", "CAISSIER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        AppUser created = userRepository.findByUsername("vendeur2").orElseThrow();
        assertThat(created.getPasswordHash()).doesNotContain("Provisoire2026");
    }

    @Test
    void gestionnaireNonAdminNePeutPasCreerUnAdministrateur() throws Exception {
        // Rôle créé uniquement pour ce test : il possède USER_MANAGE sans être ADMIN
        Role manager = roleRepository.save(new Role("TEST_GESTIONNAIRE", "Gestionnaire test", null,
                new HashSet<>(permissionRepository.findByCodeIn(Set.of("USER_MANAGE")))));
        AppUser user = testUsers.create("gest1", false);
        user.getRoles().add(manager);
        userRepository.save(user);
        String token = login(mvc, om, "gest1", TestUsers.PASSWORD);

        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(newUserBody("pirate", "ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(userRepository.existsByUsername("pirate")).isFalse();

        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(newUserBody("vendeur3", "CAISSIER")))
                .andExpect(status().isCreated());
    }

    @Test
    void motDePasseProvisoireBloqueLesAutresModules() throws Exception {
        testUsers.create("admin1", true, "ADMIN");
        String token = login(mvc, om, "admin1", TestUsers.PASSWORD);

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void desactiverUnCompteCoupeSaSessionImmediatement() throws Exception {
        testUsers.create("admin1", false, "ADMIN");
        AppUser cashier = testUsers.create("caissier1", false, "CAISSIER");
        String adminToken = login(mvc, om, "admin1", TestUsers.PASSWORD);
        String cashierToken = login(mvc, om, "caissier1", TestUsers.PASSWORD);

        mvc.perform(patch("/api/users/" + cashier.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(cashierToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationDesChampsRenvoie400() throws Exception {
        testUsers.create("admin1", false, "ADMIN");
        String token = login(mvc, om, "admin1", TestUsers.PASSWORD);

        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(newUserBody("A B", "CAISSIER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }
}
