package com.huilerie.erp.pressing;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.dashboard.DashboardService;
import com.huilerie.erp.dashboard.dto.DashboardResponse;
import com.huilerie.erp.finance.CashMovementRepository;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StorageTank;
import com.huilerie.erp.inventory.TankService;
import com.huilerie.erp.inventory.dto.TankDto;
import com.huilerie.erp.pressing.dto.PressingDto;
import com.huilerie.erp.sales.SaleService;
import com.huilerie.erp.sales.dto.SaleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Achat d'olives au bulletin : huile versée dans un puits, vendue en caisse, vendeur payé librement. */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "peseur", authorities = {"PRESSING_VIEW", "PRESSING_MANAGE", "STOCK_VIEW", "STOCK_ADJUST",
        "SALES_CREATE", "SALES_VIEW"})
class AchatOlivesTest {

    @Autowired PressingService service;
    @Autowired TankService tanks;
    @Autowired SaleService sales;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired PressingRepository pressings;
    @Autowired CashMovementRepository cash;
    @Autowired DashboardService dashboard;

    Client vendeur;
    Article huileVrac;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        testUsers.create("peseur", false, "ADMIN");
        vendeur = fx.client("Ridha", null);
        huileVrac = fx.article("HVRAC", "12.000", true);
    }

    private TankDto.Response puits(String code, String capacite) {
        return tanks.create(new TankDto.Request(code, "Puits " + code, StorageTank.Type.PILE, huileVrac.getId(),
                new BigDecimal(capacite), null));
    }

    /** 1020 kg brut − 20 kg de tare = 1000 kg net, achetés 0,900 DT/kg. */
    private PressingDto.Detail achat() {
        return service.receive(new PressingDto.ReceiveRequest(vendeur.getId(), null, "Chemlali", "Sfax",
                Pressing.OliveState.SAINES, new BigDecimal("1020"), new BigDecimal("20"), null, null,
                null, null, null, null, Pressing.Mode.ACHAT, new BigDecimal("0.900")));
    }

    private PressingDto.Detail presserDans(PressingDto.Detail p, Long tankId, String huile) {
        service.start(p.id());
        return service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal(huile), null, tankId));
    }

    private BigDecimal contenu(String code) {
        return tanks.list().stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow().quantite();
    }

    @Test
    @WithMockUser(username = "peseur", authorities = {"DASHBOARD_VIEW", "PRESSING_VIEW", "PRESSING_MANAGE"})
    void tableauDeBordTotaliseOlivesAcheteesEtPresseesEnService() {
        DashboardResponse.Olives avant = dashboard.build().olivesMois();
        achat();
        service.receive(new PressingDto.ReceiveRequest(vendeur.getId(), huileVrac.getId(), null, null,
                Pressing.OliveState.SAINES, new BigDecimal("520"), new BigDecimal("20"), new BigDecimal("0.300"),
                null, null, null, null, null, null, null));
        PressingDto.Detail annule = achat();
        service.cancel(annule.id(), "erreur");

        DashboardResponse r = dashboard.build();
        assertThat(r.olivesMois().achatKg().subtract(avant.achatKg())).isEqualByComparingTo("1000");
        assertThat(r.olivesMois().achatMontant().subtract(avant.achatMontant())).isEqualByComparingTo("900.000");
        assertThat(r.olivesMois().serviceKg().subtract(avant.serviceKg())).isEqualByComparingTo("500");
        assertThat(r.olivesMois().serviceMontant().subtract(avant.serviceMontant())).isEqualByComparingTo("150.000");
        assertThat(r.olivesJour().achatKg()).isGreaterThanOrEqualTo(new BigDecimal("1000"));
    }

    @Test
    void achatCalculeAuKgSansFraisDePressage() {
        PressingDto.Detail p = achat();
        assertThat(p.mode()).isEqualTo(Pressing.Mode.ACHAT);
        assertThat(p.montantAchat()).isEqualByComparingTo("900.000");
        assertThat(p.resteVendeur()).isEqualByComparingTo("900.000");
        assertThat(p.montant()).isEqualByComparingTo("0");
        assertThat(p.numeroBulletin()).isEqualTo("000001");

        assertThatThrownBy(() -> service.receive(new PressingDto.ReceiveRequest(vendeur.getId(), null, null, null,
                Pressing.OliveState.SAINES, new BigDecimal("100"), new BigDecimal("0"), null, null,
                null, null, null, null, Pressing.Mode.ACHAT, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("prix d'achat");
    }

    @Test
    void huileVerseeDansLePuitsPuisDiminueeParLaVenteRapide() {
        TankDto.Response puits = puits("PUITS-1", "1000");
        PressingDto.Detail fini = presserDans(achat(), puits.id(), "200");
        assertThat(fini.statut()).isEqualTo(Pressing.Status.TERMINE);
        assertThat(fini.tankCode()).isEqualTo("PUITS-1");
        assertThat(fini.huileARemettre()).isEqualByComparingTo("0");
        assertThat(contenu("PUITS-1")).isEqualByComparingTo("200");

        sales.createQuickSale(new SaleDto.QuickSaleRequest("vente-puits-1", null, null,
                List.of(new SaleDto.LineRequest(huileVrac.getId(), puits.lotId(), new BigDecimal("15"), null, false)),
                List.of(new SaleDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("180")))));
        assertThat(contenu("PUITS-1")).isEqualByComparingTo("185");
    }

    @Test
    void huileAcheteeEnFutVendueAuDetailPuisLeResteVerseDansLaCiterne() {
        TankDto.Response fut = tanks.create(new TankDto.Request("FUT-1", "Fût 1", StorageTank.Type.FUT, huileVrac.getId(),
                new BigDecimal("220"), null));
        TankDto.Response citerne = tanks.create(new TankDto.Request("CIT-1", "Citerne 1", StorageTank.Type.CITERNE,
                huileVrac.getId(), new BigDecimal("5000"), null));
        presserDans(achat(), fut.id(), "200");

        sales.createQuickSale(new SaleDto.QuickSaleRequest("vente-fut-1", null, null,
                List.of(new SaleDto.LineRequest(huileVrac.getId(), fut.lotId(), new BigDecimal("5"), null, false)),
                List.of(new SaleDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("60")))));
        assertThat(contenu("FUT-1")).isEqualByComparingTo("195");

        assertThatThrownBy(() -> tanks.transfer(fut.id(), citerne.id(), new BigDecimal("196")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ne contient que");
        List<TankDto.Response> apres = tanks.transfer(fut.id(), citerne.id(), new BigDecimal("195"));
        assertThat(apres.get(0).quantite()).isEqualByComparingTo("0");
        assertThat(contenu("CIT-1")).isEqualByComparingTo("195");
    }

    @Test
    void lHuileDUnClientNeSeTransvasePas() {
        TankDto.Response a = tanks.create(new TankDto.Request("CIT-A", "Citerne A", StorageTank.Type.CITERNE, huileVrac.getId(),
                new BigDecimal("500"), null));
        TankDto.Response duClient = tanks.create(new TankDto.Request("FUT-V", "Fût du vendeur", StorageTank.Type.FUT,
                huileVrac.getId(), new BigDecimal("500"), vendeur.getId()));
        assertThatThrownBy(() -> tanks.transfer(a.id(), duClient.id(), new BigDecimal("1")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("huilerie");
    }

    @Test
    void puitsPleinRefuse() {
        TankDto.Response puits = puits("SILO-2", "150");
        PressingDto.Detail p = achat();
        service.start(p.id());
        assertThatThrownBy(() -> service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("200"), null, puits.id())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("plein");
        assertThat(contenu("SILO-2")).isEqualByComparingTo("0");
        assertThat(pressings.findById(p.id()).orElseThrow().getStatut()).isEqualTo(Pressing.Status.EN_COURS);
    }

    @Test
    void puitsObligatoireALaFinDUnAchat() {
        PressingDto.Detail p = achat();
        service.start(p.id());
        assertThatThrownBy(() -> service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("200"), null, null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("pile");
    }

    @Test
    void prixDAchatModifiableSurLeBulletinSansDescendreSousLeDejaPaye() {
        PressingDto.Detail p = presserDans(achat(), puits("PUITS-3", "1000").id(), "200");    // 1000 kg à 0,900
        service.paySeller(p.id(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("500"), null));

        PressingDto.Detail plus = service.updatePrice(p.id(), new BigDecimal("1.100"));
        assertThat(plus.prixOlivesKg()).isEqualByComparingTo("1.100");
        assertThat(plus.montantAchat()).isEqualByComparingTo("1100.000");
        assertThat(plus.resteVendeur()).isEqualByComparingTo("600.000");

        assertThatThrownBy(() -> service.updatePrice(p.id(), new BigDecimal("0.400")))      // 400 DT < 500 DT déjà payés
                .isInstanceOf(BusinessException.class).hasMessageContaining("déjà été payé");
        assertThat(service.updatePrice(p.id(), new BigDecimal("0.500")).resteVendeur()).isEqualByComparingTo("0");
    }

    @Test
    void paiementDuVendeurLibreEtPlafonne() {
        PressingDto.Detail p = achat();
        service.paySeller(p.id(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("500"), null));
        PressingDto.Detail solde = service.paySeller(p.id(), new PressingDto.PaymentRequest(PaymentMode.CHEQUE,
                new BigDecimal("400"), "CHQ 123"));
        assertThat(solde.resteVendeur()).isEqualByComparingTo("0");
        assertThat(solde.reglementsVendeur()).hasSize(2);
        assertThatThrownBy(() -> service.paySeller(p.id(), new PressingDto.PaymentRequest(PaymentMode.ESPECES,
                new BigDecimal("1"), null))).isInstanceOf(BusinessException.class);
        assertThat(cash.total(CashMovementType.DECAISSEMENT, null, null, null)).isEqualByComparingTo("900");
        assertThatThrownBy(() -> service.cancel(p.id(), "erreur")).isInstanceOf(BusinessException.class);
    }

    @Test
    void niRemiseNiFraisPourUnAchat() {
        TankDto.Response puits = puits("PUITS-3", "1000");
        PressingDto.Detail fini = presserDans(achat(), puits.id(), "200");
        assertThatThrownBy(() -> service.returnOil(fini.id(), new PressingDto.ReturnRequest(new BigDecimal("10"), null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.pay(fini.id(), new PressingDto.PaymentRequest(PaymentMode.ESPECES,
                new BigDecimal("10"), null))).isInstanceOf(BusinessException.class);
        assertThat(pressings.oilToReturn()).isEqualByComparingTo("0");
    }

    @Test
    void capaciteNePeutPasDescendreSousLeContenu() {
        TankDto.Response puits = puits("PUITS-4", "1000");
        presserDans(achat(), puits.id(), "200");
        assertThatThrownBy(() -> tanks.update(puits.id(), new TankDto.Request(null, "Puits 4", StorageTank.Type.PILE,
                null, new BigDecimal("100"), null))).isInstanceOf(BusinessException.class);
    }
}
