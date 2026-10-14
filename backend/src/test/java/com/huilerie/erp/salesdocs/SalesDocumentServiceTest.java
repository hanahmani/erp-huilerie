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
                List.of(new SalesDocDto.LineRequest(bidon.getId(), null, new BigDecimal(qty), null, new BigDecimal("10"), null))));
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
                null, null, null, List.of(new SalesDocDto.LineRequest(bidon.getId(), null, BigDecimal.ONE, null, null, null)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void plafondDeCreditRespecte() {
        Client limited = fx.client("Limite", "100");
        SalesDocDto.Detail f = service.create(new SalesDocDto.Request(Type.FACTURE, limited.getId(), LocalDate.now(), null, null, null,
                List.of(new SalesDocDto.LineRequest(bidon.getId(), null, new BigDecimal("2"), null, null, null))));
        assertThatThrownBy(() -> service.validate(f.id())).isInstanceOf(BusinessException.class).hasMessageContaining("Plafond");
    }

    @Test
    @WithMockUser(username = "vendeur", authorities = {"SALES_VIEW", "SALESDOC_MANAGE"})
    void validationDeFactureReserveeAuxPersonnesAutorisees() {
        SalesDocDto.Detail f = draft(Type.FACTURE, "1");
        assertThatThrownBy(() -> service.validate(f.id())).isInstanceOf(AccessDeniedException.class);
    }
}
