package com.huilerie.erp.pressing;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.clients.ClientRepository;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.pressing.dto.PressingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Bulletin de pressage : numérotation, transport, adresse, correction avant pressage. */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "peseur", authorities = {"PRESSING_VIEW", "PRESSING_MANAGE"})
class BulletinPressageTest {

    @Autowired PressingService service;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired ClientRepository clients;
    @Autowired JdbcTemplate jdbc;

    Client client;
    Article huileClient;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        client = fx.client("Salah", null);
        client.setAdresse("Route de Tunis km 4, Sfax");
        client = clients.save(client);
        huileClient = fx.articleOf("HCLI", ArticleType.HUILE_VRAC, "L", true);
    }

    private PressingDto.Detail receive(String brut, String tare, String bl, String transporteur, String vehicule,
                                       String adresse) {
        return service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), "Chemlali", "Sfax",
                Pressing.OliveState.SAINES, new BigDecimal(brut), new BigDecimal(tare), new BigDecimal("0.200"), null,
                bl, transporteur, vehicule, adresse, null, null));
    }

    @Test
    void numerotationContinueEtSansTrou() {
        assertThat(receive("500", "0", null, null, null, null).numeroBulletin()).isEqualTo("000001");
        assertThatThrownBy(() -> receive("10", "10", null, null, null, null)).isInstanceOf(BusinessException.class);
        assertThat(receive("300", "0", null, null, null, null).numeroBulletin()).isEqualTo("000002");
        assertThat(receive("200", "0", null, null, null, null).numeroBulletin()).isEqualTo("000003");
    }

    @Test
    void numeroDeBulletinUnique() {
        receive("500", "0", null, null, null, null);
        PressingDto.Detail second = receive("300", "0", null, null, null, null);
        assertThatThrownBy(() -> jdbc.update("UPDATE pressings SET numero_bulletin = '000001' WHERE id = ?", second.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void numeroCarnetSaisiALaReceptionModifiableATousStatutsEtUnique() {
        PressingDto.Detail p = service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), null,
                null, Pressing.OliveState.SAINES, new BigDecimal("500"), BigDecimal.ZERO, new BigDecimal("0.200"), null,
                null, null, null, null, null, null, null, null, null, "  A-1651 "));
        assertThat(p.numeroCarnet()).isEqualTo("A-1651");

        PressingDto.Detail autre = receive("300", "0", null, null, null, null);
        assertThat(autre.numeroCarnet()).isNull();
        assertThatThrownBy(() -> service.updateCarnet(autre.id(), "a-1651"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("bulletin " + p.numeroBulletin());

        service.start(p.id());   // modifiable même après le début du pressage
        assertThat(service.updateCarnet(p.id(), "A-1651").numeroCarnet()).isEqualTo("A-1651");
        assertThat(service.updateCarnet(p.id(), "1700").numeroCarnet()).isEqualTo("1700");
        assertThat(service.updateCarnet(autre.id(), "A-1651").numeroCarnet()).isEqualTo("A-1651");
        assertThat(service.search("1700", null, null, null, null, 0, 20).content())
                .extracting(PressingDto.Summary::id).containsExactly(p.id());
        assertThat(service.updateCarnet(p.id(), "  ").numeroCarnet()).isNull();
    }

    @Test
    void adressePreRemplieDepuisLeClientEtVehiculeEnMajuscules() {
        PressingDto.Detail p = receive("500", "20", "  BL-778 ", "Transport Jaziri", " 123 tu  4567 ", "  ");
        assertThat(p.adresse()).isEqualTo("Route de Tunis km 4, Sfax");
        assertThat(p.blNumero()).isEqualTo("BL-778");
        assertThat(p.transporteur()).isEqualTo("Transport Jaziri");
        assertThat(p.vehicule()).isEqualTo("123 TU 4567");

        PressingDto.Detail autre = receive("400", "0", null, null, null, "Henchir El Mahrouk, Agareb");
        assertThat(autre.adresse()).isEqualTo("Henchir El Mahrouk, Agareb");
    }

    @Test
    void correctionDuBulletinSeulementAuStatutRecu() {
        PressingDto.Detail p = receive("500", "0", null, null, null, null);
        PressingDto.Detail corrige = service.updateTransport(p.id(),
                new PressingDto.TransportRequest("BL-12", "Hédi", "45 tu 999", "Agareb"));
        assertThat(corrige.vehicule()).isEqualTo("45 TU 999");
        assertThat(corrige.numeroBulletin()).isEqualTo(p.numeroBulletin());

        service.start(p.id());
        assertThatThrownBy(() -> service.updateTransport(p.id(), new PressingDto.TransportRequest(null, null, null, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ne peut plus être modifié");
    }

    @Test
    void dateDeReceptionParDefautModifiableMaisPasDansLeFutur() {
        java.time.Instant avant = java.time.Instant.now().minusSeconds(1);
        PressingDto.Detail auto = receive("500", "0", null, null, null, null);
        assertThat(auto.dateReception()).isAfter(avant);

        java.time.Instant hier = java.time.Instant.parse("2026-01-10T08:30:00Z");
        PressingDto.Detail saisie = service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), null, null,
                Pressing.OliveState.SAINES, new BigDecimal("500"), BigDecimal.ZERO, new BigDecimal("0.200"), null,
                null, null, null, null, null, null, null, null, hier));
        assertThat(saisie.dateReception()).isEqualTo(hier);

        java.time.Instant corrigee = java.time.Instant.parse("2026-01-11T07:00:00Z");
        assertThat(service.updateTransport(saisie.id(), new PressingDto.TransportRequest(null, null, null, null, corrigee))
                .dateReception()).isEqualTo(corrigee);
        assertThat(service.updateTransport(saisie.id(), new PressingDto.TransportRequest(null, null, null, null))
                .dateReception()).isEqualTo(corrigee);              // date absente : inchangée

        assertThatThrownBy(() -> service.updateTransport(saisie.id(), new PressingDto.TransportRequest(null, null, null, null,
                java.time.Instant.now().plusSeconds(3600)))).isInstanceOf(BusinessException.class).hasMessageContaining("futur");
    }

    @Test
    void dateModifiableApresLeDebutDuPressageMaisPasApres() {
        PressingDto.Detail p = receive("500", "0", null, null, null, null);
        service.start(p.id());
        service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("80"), null, null));

        java.time.Instant hier = java.time.Instant.now().minus(java.time.Duration.ofDays(1));
        assertThat(service.updateDate(p.id(), hier).dateReception()).isEqualTo(hier);
        assertThatThrownBy(() -> service.updateDate(p.id(), java.time.Instant.now().plusSeconds(60)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("début du pressage");
    }

    @Test
    void rechercheParBulletinEtVehicule() {
        receive("500", "0", null, null, "77 TU 1234", null);
        receive("300", "0", null, null, null, null);
        assertThat(service.search("000002", null, null, null, null, 0, 20).content())
                .extracting(PressingDto.Summary::numeroBulletin).containsExactly("000002");
        assertThat(service.search("77 tu", null, null, null, null, 0, 20).content())
                .extracting(PressingDto.Summary::vehicule).containsExactly("77 TU 1234");
    }
}
