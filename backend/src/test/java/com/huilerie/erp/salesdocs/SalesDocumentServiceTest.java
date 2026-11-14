package com.huilerie.erp.salesdocs;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.salesdocs.SalesDocument.Status;
import com.huilerie.erp.salesdocs.SalesDocument.Type;
import com.huilerie.erp.salesdocs.dto.SalesDocDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "commercial", authorities = {"SALES_VIEW", "SALESDOC_MANAGE", "INVOICE_VALIDATE"})
class SalesDocumentServiceTest {

    @Autowired SalesDocumentService service;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired StockBalanceRepository balances;
    @Autowired com.huilerie.erp.settings.TaxRepository taxes;
    @Autowired com.huilerie.erp.settings.CompanySettingsRepository companySettings;

    Article bidon;
    Client client;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        bidon = fx.article("B5", "80.000", false);
        fx.receive(bidon, null, "20");
        client = fx.client("Epicerie", null);
    }

    private BigDecimal stock() {
        return balances.findByArticleIdAndLotRef(bidon.getId(), 0L).orElseThrow().getQuantite();
    }

    private SalesDocDto.Detail draft(Type type, String qty) {
        return service.create(new SalesDocDto.Request(type, client.getId(), LocalDate.now(), LocalDate.now().plusDays(30), null, null,
                List.of(new SalesDocDto.LineRequest(bidon.getId(), null, new BigDecimal(qty), null, new BigDecimal("10"), null, false))));
    }

    @Test
    void cycleCompletDevisCommandeLivraisonFacturePaiement() {
        SalesDocDto.Detail devis = service.validate(draft(Type.DEVIS, "5").id());
        assertThat(devis.numero()).startsWith("DEV-");
        assertThat(devis.totalHt()).isEqualByComparingTo("360.000");   // 5 × 80 − 10 %

        SalesDocDto.Detail cmd = service.validate(service.transform(devis.id(), Type.COMMANDE).id());
        SalesDocDto.Detail bl = service.validate(service.transform(cmd.id(), Type.LIVRAISON).id());
        assertThat(stock()).isEqualByComparingTo("15");              // le stock sort à la livraison

        SalesDocDto.Detail fac = service.validate(service.transform(bl.id(), Type.FACTURE).id());
        assertThat(fac.numero()).startsWith("FAC-");
        assertThat(stock()).isEqualByComparingTo("15");              // pas une seconde fois à la facture

        SalesDocDto.Detail paid = service.pay(fac.id(), new SalesDocDto.PaymentRequest(PaymentMode.VIREMENT, new BigDecimal("100"), null));
        assertThat(paid.reste()).isEqualByComparingTo("260.000");
        assertThatThrownBy(() -> service.pay(fac.id(), new SalesDocDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("261"), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void factureDirecteSortLeStockEtSAnnuleParAvoir() {
        SalesDocDto.Detail fac = service.validate(draft(Type.FACTURE, "4").id());
        assertThat(stock()).isEqualByComparingTo("16");
        assertThatThrownBy(() -> service.cancel(fac.id(), "erreur")).isInstanceOf(BusinessException.class);

        SalesDocDto.Detail avoir = service.creditNote(fac.id(), new SalesDocDto.CreditNoteRequest("Retour client", true, null));
        assertThat(avoir.type()).isEqualTo(Type.AVOIR);
        assertThat(stock()).isEqualByComparingTo("20");
        assertThat(service.get(fac.id()).statut()).isEqualTo(Status.ANNULE);
    }

    @Test
    void documentValideNonModifiable() {
        SalesDocDto.Detail devis = service.validate(draft(Type.DEVIS, "1").id());
        assertThatThrownBy(() -> service.update(devis.id(), new SalesDocDto.Request(Type.DEVIS, client.getId(), LocalDate.now(),
                null, null, null, List.of(new SalesDocDto.LineRequest(bidon.getId(), null, BigDecimal.ONE, null, null, null, false)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void plafondDeCreditRespecte() {
        Client limited = fx.client("Limite", "100");
        SalesDocDto.Detail f = service.create(new SalesDocDto.Request(Type.FACTURE, limited.getId(), LocalDate.now(), null, null, null,
                List.of(new SalesDocDto.LineRequest(bidon.getId(), null, new BigDecimal("2"), null, null, null, false))));
        assertThatThrownBy(() -> service.validate(f.id())).isInstanceOf(BusinessException.class).hasMessageContaining("Plafond");
    }

    @Test
    @WithMockUser(username = "vendeur", authorities = {"SALES_VIEW", "SALESDOC_MANAGE"})
    void validationDeFactureReserveeAuxPersonnesAutorisees() {
        SalesDocDto.Detail f = draft(Type.FACTURE, "1");
        assertThatThrownBy(() -> service.validate(f.id())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void totauxHtTvaParTauxTimbreEtTtc() {
        var s = new com.huilerie.erp.settings.CompanySettings();
        s.setRaisonSociale("Huilerie test");
        s.setTimbreFiscal(new BigDecimal("1.000"));
        companySettings.save(s);
        Long t19 = taxes.save(new com.huilerie.erp.settings.Tax("T19", new BigDecimal("19"))).getId();
        Long t7 = taxes.save(new com.huilerie.erp.settings.Tax("T7", new BigDecimal("7"))).getId();
        SalesDocDto.Detail f = service.create(new SalesDocDto.Request(Type.FACTURE, client.getId(), LocalDate.now(), null, null, null, List.of(
                new SalesDocDto.LineRequest(bidon.getId(), null, new BigDecimal("5"), null, new BigDecimal("10"), t19, false),   // 360 HT, 68,400 TVA
                new SalesDocDto.LineRequest(bidon.getId(), null, new BigDecimal("2"), null, null, t7, false))));                 // 160 HT, 11,200 TVA
        assertThat(f.totalHt()).isEqualByComparingTo("520.000");
        assertThat(f.totalTva()).isEqualByComparingTo("79.600");
        assertThat(f.timbre()).isEqualByComparingTo("0");                       // le timbre s'ajoute à la validation
        SalesDocDto.Detail v = service.validate(f.id());
        assertThat(v.timbre()).isEqualByComparingTo("1.000");
        assertThat(v.totalTtc()).isEqualByComparingTo("600.600");
        assertThat(v.montantEnLettres()).isEqualTo("six cents dinars et six cents millimes");

        // Le devis ne porte jamais de timbre.
        SalesDocDto.Detail devis = service.validate(draft(Type.DEVIS, "1").id());
        assertThat(devis.timbre()).isEqualByComparingTo("0");
    }

    @Test
    void enEurosLesMontantsSontArrondisAuCentimeEtLeCatalogueNEstPasUtilise() {
        SalesDocDto.Request sansPrix = new SalesDocDto.Request(Type.DEVIS, client.getId(), LocalDate.now(), null, null, null,
                List.of(new SalesDocDto.LineRequest(bidon.getId(), null, BigDecimal.ONE, null, null, null, false)),
                com.huilerie.erp.common.Devise.EUR, null, null, null, null, null);
        assertThatThrownBy(() -> service.create(sansPrix)).isInstanceOf(BusinessException.class).hasMessageContaining("en EUR");

        SalesDocDto.Detail d = service.create(new SalesDocDto.Request(Type.DEVIS, client.getId(), LocalDate.now(), null, null, null,
                List.of(new SalesDocDto.LineRequest(bidon.getId(), null, new BigDecimal("3"), new BigDecimal("6.33"), new BigDecimal("7.5"), null, false)),
                com.huilerie.erp.common.Devise.EUR, null, null, null, null, null));
        assertThat(d.totalHt()).isEqualByComparingTo("17.570");                 // 18,99 × 0,925 = 17,565 75 → 17,57
        assertThat(d.devise()).isEqualTo(com.huilerie.erp.common.Devise.EUR);
    }

    @Test
    void transformationGardeDeviseRegimeEtInformationsExport() {
        SalesDocDto.Detail devis = service.create(new SalesDocDto.Request(Type.DEVIS, client.getId(), LocalDate.now(), null, null, null,
                List.of(new SalesDocDto.LineRequest(bidon.getId(), null, BigDecimal.ONE, new BigDecimal("10"), null, null, false, "1509.20")),
                com.huilerie.erp.common.Devise.USD, SalesDocument.Regime.EXPORT, "Tunisie", "CIF", new BigDecimal("1"), new BigDecimal("1.2")));
        devis = service.validate(devis.id());
        SalesDocDto.Detail fac = service.transform(devis.id(), Type.FACTURE);
        assertThat(fac.devise()).isEqualTo(com.huilerie.erp.common.Devise.USD);
        assertThat(fac.regime()).isEqualTo(SalesDocument.Regime.EXPORT);
        assertThat(fac.incoterm()).isEqualTo("CIF");
        assertThat(fac.lignes().get(0).codeSh()).isEqualTo("1509.20");
        assertThat(fac.source().numero()).isEqualTo(devis.numero());
    }
}
