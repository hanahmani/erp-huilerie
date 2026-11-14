package com.huilerie.erp.sales;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.catalog.CatalogService;
import com.huilerie.erp.catalog.Unit;
import com.huilerie.erp.catalog.UnitRepository;
import com.huilerie.erp.catalog.dto.ArticleDto;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.sales.dto.SaleDto;
import com.huilerie.erp.salesdocs.SalesDocument;
import com.huilerie.erp.salesdocs.SalesDocumentService;
import com.huilerie.erp.salesdocs.dto.SalesDocDto;
import com.huilerie.erp.settings.SettingsService;
import com.huilerie.erp.settings.dto.SettingsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Huile stockée en kg, prix fixé au litre, vendue au kg ou au litre (1 L = 0,916 kg). */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "caissier1", authorities = {"SALES_CREATE", "SALES_VIEW", "SALES_CANCEL", "CATALOG_MANAGE",
        "SETTINGS_MANAGE",
        "SALESDOC_MANAGE"})
class HuileAuLitreTest {

    @Autowired SaleService sales;
    @Autowired CatalogService catalog;
    @Autowired SettingsService settings;
    @Autowired UnitRepository units;
    @Autowired ArticleRepository articles;
    @Autowired StockBalanceRepository balances;
    @Autowired SalesDocumentService docs;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;

    Unit kg;
    Article huile;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        testUsers.create("caissier1", false, "CAISSIER");
        kg = units.save(new Unit("kg", "Kilogramme"));
        // 16,488 DT le litre → 16,488 / 0,916 = 18,000 DT le kg
        huile = articles.findById(catalog.saveArticle(null, oil(kg, "16.488")).id()).orElseThrow();
        fx.receive(huile, null, "200");
    }

    private ArticleDto.Request oil(Unit unit, String prixLitre) {
        return new ArticleDto.Request("Huile en vrac extra vierge", null, ArticleType.HUILE_VRAC, null, unit.getId(),
                null, null, new BigDecimal(prixLitre), null, null, null, true, false);
    }

    private SaleDto.Detail sell(String qty, boolean litres, String cash) {
        return sales.createQuickSale(new SaleDto.QuickSaleRequest(UUID.randomUUID().toString(), null, null,
                List.of(new SaleDto.LineRequest(huile.getId(), null, new BigDecimal(qty), null, litres)),
                List.of(new SaleDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal(cash)))));
    }

    private BigDecimal stock() {
        return balances.findByArticleIdAndLotRef(huile.getId(), 0L).orElseThrow().getQuantite();
    }

    @Test
    void prixAuKgDeduitDuPrixAuLitre() {
        assertThat(huile.getPrixVente()).isEqualByComparingTo("18.000");
        assertThat(huile.getPrixVenteLitre()).isEqualByComparingTo("16.488");
    }

    @Test
    void venteAuLitreSortDesKgDuStock() {
        SaleDto.Detail sale = sell("10", true, "164.880");

        SaleDto.Line line = sale.lignes().get(0);
        assertThat(line.unite()).isEqualTo("L");
        assertThat(line.quantite()).isEqualByComparingTo("10");
        assertThat(line.prixUnitaire()).isEqualByComparingTo("16.488");
        assertThat(line.quantiteStock()).isEqualByComparingTo("9.160");
        assertThat(sale.total()).isEqualByComparingTo("164.880");
        assertThat(stock()).isEqualByComparingTo("190.840");

        sales.cancel(sale.id(), "Erreur de saisie");
        assertThat(stock()).isEqualByComparingTo("200.000");
    }

    @Test
    void venteAuKgInchangee() {
        SaleDto.Detail sale = sell("5", false, "90");
        assertThat(sale.lignes().get(0).unite()).isEqualTo("kg");
        assertThat(sale.total()).isEqualByComparingTo("90.000");
        assertThat(stock()).isEqualByComparingTo("195.000");
    }

    @Test
    void stockInsuffisantCompteEnKg() {
        // 220 L × 0,916 = 201,52 kg > 200 kg
        assertThatThrownBy(() -> sell("220", true, "4000")).isInstanceOf(BusinessException.class);
        assertThat(stock()).isEqualByComparingTo("200.000");
    }

    @Test
    void prixAuLitreReserveALHuileEnKg() {
        Unit litre = units.save(new Unit("L", "Litre"));
        assertThatThrownBy(() -> catalog.saveArticle(null, oil(litre, "16.000")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("kg");
    }

    @Test
    void venteAuLitreRefuseeSansPrixAuLitre() {
        huile = fx.article("SANS-L", "18.000", false);
        fx.receive(huile, null, "10");
        assertThatThrownBy(() -> sell("1", true, "18")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("prix au litre");
    }

    @Test
    void changerLaDensiteRecalculeLePrixAuKg() {
        assertThat(settings.oilDensity()).isEqualByComparingTo("0.916");
        settings.update(new SettingsRequest("Huilerie", null, null, null, null, null, null, null, null, null, null, false, null, null, null, null,
                new BigDecimal("0.92")));
        assertThat(articles.findById(huile.getId()).orElseThrow().getPrixVente()).isEqualByComparingTo("17.922");

        SaleDto.Detail sale = sell("10", true, "164.880");
        assertThat(sale.lignes().get(0).quantiteStock()).isEqualByComparingTo("9.200");
    }

    @Test
    void bonDeLivraisonAuLitreSortDesKgEtSAnnule() {
        SalesDocDto.Detail draft = docs.create(new SalesDocDto.Request(SalesDocument.Type.LIVRAISON,
                fx.client("Epicerie", null).getId(), LocalDate.now(), null, null, null,
                List.of(new SalesDocDto.LineRequest(huile.getId(), null, new BigDecimal("50"), null, null, null, true))));
        SalesDocDto.Line line = draft.lignes().get(0);
        assertThat(line.unite()).isEqualTo("L");
        assertThat(line.enLitres()).isTrue();
        assertThat(line.prixUnitaire()).isEqualByComparingTo("16.488");
        assertThat(line.quantiteStock()).isEqualByComparingTo("45.800");
        assertThat(draft.totalHt()).isEqualByComparingTo("824.400");

        SalesDocDto.Detail bl = docs.validate(draft.id());
        assertThat(stock()).isEqualByComparingTo("154.200");

        // La facture reprend la ligne au litre sans sortir le stock une seconde fois.
        SalesDocDto.Detail facture = docs.transform(bl.id(), SalesDocument.Type.FACTURE);
        assertThat(facture.lignes().get(0).quantiteStock()).isEqualByComparingTo("45.800");

        docs.cancel(facture.id(), "Erreur");                         // brouillon annulé : le BL est rouvert
        docs.cancel(bl.id(), "Erreur");
        assertThat(stock()).isEqualByComparingTo("200.000");
    }
}
