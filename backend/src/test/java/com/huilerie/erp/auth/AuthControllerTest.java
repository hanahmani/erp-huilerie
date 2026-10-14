package com.huilerie.erp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.audit.AuditLogRepository;
import com.huilerie.erp.users.AppUser;
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

import java.util.Map;

import static com.huilerie.erp.MockMvcSupport.bearer;
import static com.huilerie.erp.MockMvcSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TestUsers testUsers;
    @Autowired UserRepository userRepository;
    @Autowired AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        testUsers.reset();
    }

    @Test
    void loginAvecBonsIdentifiantsRenvoieUnJetonEtLesPermissions() throws Exception {
        testUsers.create("caissier1", false, "CAISSIER");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("username", "caissier1", "password", TestUsers.PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("caissier1"))
                .andExpect(jsonPath("$.user.permissions", hasItem("SALES_CREATE")));
    }

    @Test
    void motDePasseIncorrectRenvoie401EtEstTrace() throws Exception {
        testUsers.create("caissier1", false, "CAISSIER");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("username", "caissier1", "password", "mauvais123456"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiant ou mot de passe incorrect."));

        assertThat(auditLogRepository.findByActionOrderByCreatedAtDesc("LOGIN_FAILURE")).hasSize(1);
    }

    @Test
    void lesMotsDePasseSontHachesEnBase() {
        AppUser user = testUsers.create("caissier1", false, "CAISSIER");
        assertThat(user.getPasswordHash()).isNotEqualTo(TestUsers.PASSWORD).startsWith("$2");
    }

    @Test
    void compteDesactiveNePeutPasSeConnecter() throws Exception {
        AppUser user = testUsers.create("caissier1", false, "CAISSIER");
        user.setActive(false);
        userRepository.save(user);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("username", "caissier1", "password", TestUsers.PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meSansJetonRenvoie401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void jetonFalsifieRenvoie401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changerLeMotDePasseInvalideLAncienJeton() throws Exception {
        testUsers.create("caissier1", false, "CAISSIER");
        String oldToken = login(mvc, om, "caissier1", TestUsers.PASSWORD);

        String body = om.writeValueAsString(Map.of("currentPassword", TestUsers.PASSWORD, "newPassword", "NouveauMdp2026"));
        mvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, bearer(oldToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(oldToken)))
                .andExpect(status().isUnauthorized());
        login(mvc, om, "caissier1", "NouveauMdp2026");
    }

    @Test
    void motDePasseTropFaibleEstRefuse() throws Exception {
        testUsers.create("caissier1", false, "CAISSIER");
        String token = login(mvc, om, "caissier1", TestUsers.PASSWORD);

        String body = om.writeValueAsString(Map.of("currentPassword", TestUsers.PASSWORD, "newPassword", "court"));
        mvc.perform(post("/api/auth/change-password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void ilNExisteAucuneRouteDInscription() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
