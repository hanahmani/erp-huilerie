package com.huilerie.erp.pressing;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.CashMovementRepository;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StorageTank;
import com.huilerie.erp.inventory.TankService;
import com.huilerie.erp.inventory.dto.TankDto;
import com.huilerie.erp.pressing.dto.PressingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Compte d'huile des clients (puits réservé, remises en kg ou litres) et avances aux vendeurs d'olives. */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "peseur", authorities = {"PRESSING_VIEW", "PRESSING_MANAGE", "STOCK_VIEW", "STOCK_ADJUST"})
class ComptesClientsTest {

    @Autowired PressingService service;
    @Autowired ClientAccountService accounts;
    @Autowired TankService tanks;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired CashMovementRepository cash;

    Client x;
    Article huileClients;
    Article huileVrac;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        testUsers.create("peseur", false, "ADMIN");
        x = fx.client("X", null);
        huileClients = fx.articleOf("HCLI", ArticleType.HUILE_VRAC, "kg", true);
        huileVrac = fx.articleOf("HVR", ArticleType.HUILE_VRAC, "kg", true);
    }

    private TankDto.Response tank(String code, Article oil, Client owner) {
        return tanks.create(new TankDto.Request(code, "Puits " + code, StorageTank.Type.PUITS, oil.getId(),
                new BigDecimal("5000"), owner == null ? null : owner.getId()));
    }

    /** Pressage à façon de 1000 kg net à 0,100 DT/kg (100 DT), terminé dans le puits donné. */
    private PressingDto.Detail pressage(Long tankId, String huile) {
        PressingDto.Detail p = service.receive(new PressingDto.ReceiveRequest(x.getId(), huileClients.getId(), null, null,
                Pressing.OliveState.SAINES, new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("0.100"), null,
                null, null, null, null, Pressing.Mode.SERVICE, null));
        service.start(p.id());
        return service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal(huile), null, tankId));
    }

    private void paye(PressingDto.Detail p) {
        service.pay(p.id(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, p.reste(), null));
    }

    /** Achat de 1000 kg net d'olives à 0,900 DT/kg (900 DT), avec une avance payée le jour même. */
    private PressingDto.Detail achat(String avance) {
        return service.receive(new PressingDto.ReceiveRequest(x.getId(), null, null, null, Pressing.OliveState.SAINES,
                new BigDecimal("1000"), BigDecimal.ZERO, null, null, null, null, null, null, Pressing.Mode.ACHAT,
                new BigDecimal("0.900"), avance == null ? null : new BigDecimal(avance),
                avance == null ? null : PaymentMode.ESPECES));
    }

    private PressingDto.Detail recu() {
        PressingDto.Detail p = service.receive(new PressingDto.ReceiveRequest(x.getId(), huileClients.getId(), null, null,
                Pressing.OliveState.SAINES, new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("0.100"), null,
                null, null, null, null, Pressing.Mode.SERVICE, null));
        service.start(p.id());
        return p;
    }

    @Test
    void huileConserveeDansLaCiterneOuLeFutDuClient() {
        TankDto.Response citerne = tanks.create(new TankDto.Request("CIT-X", "Citerne de X", StorageTank.Type.CITERNE,
                huileClients.getId(), new BigDecimal("1000"), x.getId()));
        TankDto.Response fut = tanks.create(new TankDto.Request("FUT-X1", "Fût de X", StorageTank.Type.FUT,
                huileClients.getId(), new BigDecimal("200"), x.getId()));
        assertThat(pressage(citerne.id(), "150").tankCode()).isEqualTo("CIT-X");
        assertThat(pressage(fut.id(), "180").tankCode()).isEqualTo("FUT-X1");
        assertThat(accounts.oilAccount(x.getId()).solde()).isEqualByComparingTo("330");
        assertThatThrownBy(() -> pressage(fut.id(), "50")).isInstanceOf(BusinessException.class);   // fût de 200 kg plein
    }

    @Test
    @WithMockUser(username = "peseur", authorities = {"PRESSING_VIEW", "PRESSING_MANAGE", "STOCK_VIEW", "STOCK_ADJUST", "SALES_CREATE"})
    void priseDirecteEncaisseLeServiceEtRemetToutLHuile() {
        PressingDto.Detail p = recu();
        assertThatThrownBy(() -> service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("150"), null, null,
                true, null, null, null))).isInstanceOf(BusinessException.class).hasMessageContaining("mode de paiement");

        PressingDto.Detail fini = service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("150"), null, null,
                true, PaymentMode.ESPECES, null, "Fils de X"));
        assertThat(fini.statut()).isEqualTo(Pressing.Status.REMIS);
        assertThat(fini.reste()).isEqualByComparingTo("0");
        assertThat(fini.huileARemettre()).isEqualByComparingTo("0");
        assertThat(fini.remises()).singleElement().satisfies(r -> assertThat(r.remisA()).isEqualTo("Fils de X"));
        assertThat(cash.total(CashMovementType.ENCAISSEMENT, null, null, null)).isEqualByComparingTo("100");
        assertThat(accounts.oilAccount(x.getId()).solde()).isEqualByComparingTo("0");
    }

    @Test
    void paiementDuServiceSurLeCompteImputeAuxPressagesLesPlusAnciens() {
        PressingDto.Detail p1 = pressage(null, "150");             // 100 DT de service chacun
        PressingDto.Detail p2 = pressage(null, "120");
        assertThat(accounts.oilAccount(x.getId()).montantDu()).isEqualByComparingTo("200");

        accounts.payService(x.getId(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("50"), null));
        PressingDto.OilAccount compte = accounts.payService(x.getId(),
                new PressingDto.PaymentRequest(PaymentMode.CHEQUE, new BigDecimal("120"), "CH-77"));
        assertThat(compte.montantDu()).isEqualByComparingTo("30");
        assertThat(service.get(p1.id()).reste()).isEqualByComparingTo("0");
        assertThat(service.get(p2.id()).reste()).isEqualByComparingTo("30");
        assertThat(compte.paiements()).hasSize(3);                 // 50 + (50 sur p1, 70 sur p2)
        assertThat(cash.total(CashMovementType.ENCAISSEMENT, null, null, null)).isEqualByComparingTo("170");

        assertThatThrownBy(() -> accounts.payService(x.getId(), new PressingDto.PaymentRequest(PaymentMode.ESPECES,
                new BigDecimal("31"), null))).isInstanceOf(BusinessException.class).hasMessageContaining("dépasse");
        accounts.payService(x.getId(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("30"), null));
        assertThatThrownBy(() -> accounts.payService(x.getId(), new PressingDto.PaymentRequest(PaymentMode.ESPECES,
                new BigDecimal("1"), null))).isInstanceOf(BusinessException.class).hasMessageContaining("aucun service");
    }

    @Test
    void priseDirecteSansCiterne() {
        PressingDto.Detail p = recu();
        TankDto.Response citerne = tanks.create(new TankDto.Request("CIT-X", "Citerne de X", StorageTank.Type.CITERNE,
                huileClients.getId(), new BigDecimal("1000"), x.getId()));
        assertThatThrownBy(() -> service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("150"), null,
                citerne.id(), true, PaymentMode.ESPECES, null, null))).isInstanceOf(BusinessException.class);
    }

    private BigDecimal decaisse() {
        return cash.total(CashMovementType.DECAISSEMENT, null, null, null);
    }

    @Test
    void compteDHuileTracableDansLePuitsDuClient() {
        TankDto.Response puits3 = tank("P3", huileClients, x);
        PressingDto.Detail p1 = pressage(puits3.id(), "200");
        assertThat(p1.tankCode()).isEqualTo("P3");
        paye(p1);

        // Le lendemain : il reprend 100 L = 91,600 kg.
        PressingDto.OilAccount apresRemise = accounts.returnToClient(x.getId(),
                new PressingDto.ClientReturnRequest(new BigDecimal("100"), true, null));
        assertThat(apresRemise.solde()).isEqualByComparingTo("108.400");

        // Le surlendemain : nouveau pressage dans le même puits.
        paye(pressage(puits3.id(), "150"));
        PressingDto.OilAccount compte = accounts.oilAccount(x.getId());
        assertThat(compte.solde()).isEqualByComparingTo("258.400");
        assertThat(compte.puits()).singleElement().satisfies(t -> assertThat(t.quantite()).isEqualByComparingTo("258.400"));
        assertThat(compte.mouvements()).extracting(PressingDto.OilMovement::type).containsExactly("ENTREE", "SORTIE", "ENTREE");
        assertThat(compte.mouvements()).extracting(PressingDto.OilMovement::solde)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("200"), new BigDecimal("108.4"), new BigDecimal("258.4"));
        PressingDto.OilMovement sortie = compte.mouvements().get(1);
        assertThat(sortie.sortie()).isEqualByComparingTo("91.600");
        assertThat(sortie.litres()).isEqualByComparingTo("100");
        assertThat(sortie.numero()).startsWith("RMH-");

        // Remise de 220 kg prise sur les deux pressages (FIFO), sous un seul numéro.
        accounts.returnToClient(x.getId(), new PressingDto.ClientReturnRequest(new BigDecimal("220"), false, "Fils de X"));
        assertThat(accounts.oilAccount(x.getId()).solde()).isEqualByComparingTo("38.400");
        assertThat(service.get(p1.id()).statut()).isEqualTo(Pressing.Status.REMIS);
    }

    @Test
    void remiseRefuseeTantQueLeServiceNestPasPaye() {
        pressage(tank("P3", huileClients, x).id(), "200");
        assertThatThrownBy(() -> accounts.returnToClient(x.getId(),
                new PressingDto.ClientReturnRequest(new BigDecimal("10"), false, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("payé");
        assertThat(accounts.oilAccount(x.getId()).montantDu()).isEqualByComparingTo("100.000");
    }

    @Test
    void remiseLimiteeAuSolde() {
        paye(pressage(null, "200"));
        assertThatThrownBy(() -> accounts.returnToClient(x.getId(),
                new PressingDto.ClientReturnRequest(new BigDecimal("219"), true, null)))   // 219 L = 200,604 kg
                .isInstanceOf(BusinessException.class).hasMessageContaining("Il reste 200");
    }

    @Test
    void puitsReserveAuClientSeulement() {
        TankDto.Response puitsY = tank("PY", huileClients, fx.client("Y", null));
        assertThatThrownBy(() -> pressage(puitsY.id(), "200"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("réservé");

        TankDto.Response puitsX = tank("PX", huileClients, x);
        PressingDto.Detail a = achat(null);
        service.start(a.id());
        assertThatThrownBy(() -> service.finish(a.id(), new PressingDto.FinishRequest(new BigDecimal("100"), null, puitsX.id())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("réservé");
    }

    @Test
    void avanceVerseeAvantPuisDeduiteDesAchats() {
        PressingDto.SellerAccount compte = accounts.giveAdvance(x.getId(),
                new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("1000"), null));
        assertThat(compte.avanceDisponible()).isEqualByComparingTo("1000");
        assertThat(decaisse()).isEqualByComparingTo("1000");

        PressingDto.Detail a1 = achat(null);                       // 900 DT entièrement couverts par l'avance
        assertThat(a1.resteVendeur()).isEqualByComparingTo("0");
        assertThat(a1.reglementsVendeur()).singleElement()
                .satisfies(r -> assertThat(r.reference()).startsWith("Avance AVO-"));

        PressingDto.Detail a2 = achat("300");                      // 100 DT d'avance restante + 300 DT payés à la réception
        assertThat(a2.resteVendeur()).isEqualByComparingTo("500");
        assertThat(decaisse()).isEqualByComparingTo("1300");       // l'avance n'est pas décaissée deux fois

        compte = accounts.sellerAccount(x.getId());
        assertThat(compte.avanceDisponible()).isEqualByComparingTo("0");
        assertThat(compte.resteDu()).isEqualByComparingTo("500");
    }

    @Test
    void avanceDeduiteDUnAchatDejaEnregistreEtRendueSiAnnule() {
        PressingDto.Detail a = achat(null);
        accounts.giveAdvance(x.getId(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("1000"), null));
        assertThat(service.get(a.id()).resteVendeur()).isEqualByComparingTo("0");
        assertThat(accounts.sellerAccount(x.getId()).avanceDisponible()).isEqualByComparingTo("100");

        service.cancel(a.id(), "Erreur de pesée");                 // seule l'avance avait été déduite : annulation permise
        assertThat(accounts.sellerAccount(x.getId()).avanceDisponible()).isEqualByComparingTo("1000");
    }

    @Test
    void avanceRendueSiAchatSupprime() {
        accounts.giveAdvance(x.getId(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("1000"), null));
        PressingDto.Detail a = achat(null);
        assertThat(accounts.sellerAccount(x.getId()).avanceDisponible()).isEqualByComparingTo("100");

        service.delete(a.id());
        assertThat(accounts.sellerAccount(x.getId()).avanceDisponible()).isEqualByComparingTo("1000");
    }
}
