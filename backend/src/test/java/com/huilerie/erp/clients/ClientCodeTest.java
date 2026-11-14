package com.huilerie.erp.clients;

import com.huilerie.erp.TestUsers;
import com.huilerie.erp.clients.dto.ClientDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Code client automatique : plus grand code CLI- existant + 1, non modifiable depuis le formulaire. */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "gerant", authorities = {"CLIENT_VIEW", "CLIENT_MANAGE"})
class ClientCodeTest {

    @Autowired ClientService service;
    @Autowired ClientRepository clients;
    @Autowired TestUsers testUsers;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        testUsers.create("gerant", false, "ADMIN");
    }

    private static ClientDto.Request req(String nom) {
        return new ClientDto.Request(ClientType.PARTICULIER, nom, null, null, null, null, null, null, null, null);
    }

    private Client withCode(String nom, String code) {
        Client c = new Client();
        c.setCode(code);
        c.setType(ClientType.PARTICULIER);
        c.setNom(nom);
        return clients.save(c);
    }

    @Test
    void premierClient() {
        assertThat(service.save(null, req("Ali")).code()).isEqualTo("CLI-000001");
    }

    @Test
    void suitLePlusGrandCodeExistant() {
        withCode("Ali", "CLI-000023");
        withCode("Salah", "CLI-000007");
        withCode("Hors série", "BC-900");
        assertThat(service.save(null, req("Nouveau")).code()).isEqualTo("CLI-000024");
    }

    @Test
    void suppressionDuDernierClientLibereSonNumero() {
        withCode("Ali", "CLI-000001");
        Client dernier = withCode("Erreur", "CLI-000002");
        clients.delete(dernier);
        assertThat(service.save(null, req("Salah")).code()).isEqualTo("CLI-000002");
    }

    @Test
    void modificationGardeLeCode() {
        ClientDto.Response c = service.save(null, req("Ali"));
        assertThat(service.save(c.id(), req("Ali Ben")).code()).isEqualTo(c.code());
    }
}
