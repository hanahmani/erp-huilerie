package com.huilerie.erp.production;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.inventory.LotOwner;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.production.dto.ProductionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "chef", authorities = {"PRODUCTION_VIEW", "PRODUCTION_MANAGE"})
class ProductionServiceTest {

    @Autowired ProductionService service;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired StockBalanceRepository balances;

    Article olives;
    Article huile;
    Article grignons;
    StockLot lotOlives;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        olives = fx.articleOf("OLV", ArticleType.OLIVES, "kg", true, null);
        huile = fx.articleOf("EVV", ArticleType.HUILE_VRAC, "L", true, null);
        grignons = fx.articleOf("GRG", ArticleType.SOUS_PRODUIT, "kg", false, null);
        lotOlives = fx.lot(olives, "OL-1", LotOwner.ENTREPRISE, null);
        fx.receive(olives, lotOlives, "1000");
    }

    private ProductionDto.Detail plan(String kg) {
        return service.plan(null, new ProductionDto.PlanRequest(LocalDate.now(), "Ligne 1", huile.getId(),
                List.of(new ProductionDto.InputRequest(olives.getId(), lotOlives.getId(), new BigDecimal(kg))), null));
    }

    @Test
    void triturationCompleteAvecRendementEtCategorie() {
        ProductionDto.Detail o = plan("1000");
        service.start(o.id());
        assertThat(balances.findByArticleIdAndLotRef(olives.getId(), lotOlives.getId()).orElseThrow().getQuantite())
                .isEqualByComparingTo("0");

        ProductionDto.Detail done = service.finish(o.id(), new ProductionDto.FinishRequest(new BigDecimal("180"),
                new BigDecimal("15"), new BigDecimal("0.4"), null, true,
                List.of(new ProductionDto.ByProductRequest(grignons.getId(), new BigDecimal("400")))));
        assertThat(done.rendement()).isEqualByComparingTo("18.000");
        assertThat(done.categorie()).isEqualTo(OilCategory.EXTRA_VIERGE);
        assertThat(done.huileLot()).isEqualTo(o.numero());
        assertThat(balances.findByArticleIdAndLotRef(grignons.getId(), 0L).orElseThrow().getQuantite())
                .isEqualByComparingTo("400");
    }

    @Test
    void demarrageRefuseSiOlivesInsuffisantes() {
        ProductionDto.Detail o = plan("1200");
        assertThatThrownBy(() -> service.start(o.id())).isInstanceOf(BusinessException.class).hasMessageContaining("Stock insuffisant");
    }

    @Test
    void olivesDUnClientRefusees() {
        StockLot clientLot = fx.lot(olives, "CL-1", LotOwner.CLIENT, fx.client("Ali", null));
        assertThatThrownBy(() -> service.plan(null, new ProductionDto.PlanRequest(LocalDate.now(), null, huile.getId(),
                List.of(new ProductionDto.InputRequest(olives.getId(), clientLot.getId(), BigDecimal.TEN)), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void classementParAcidite() {
        assertThat(OilCategory.fromAcidity(new BigDecimal("0.80"))).isEqualTo(OilCategory.EXTRA_VIERGE);
        assertThat(OilCategory.fromAcidity(new BigDecimal("1.5"))).isEqualTo(OilCategory.VIERGE);
        assertThat(OilCategory.fromAcidity(new BigDecimal("3.0"))).isEqualTo(OilCategory.COURANTE);
        assertThat(OilCategory.fromAcidity(new BigDecimal("4"))).isEqualTo(OilCategory.LAMPANTE);
    }
}
