package com.huilerie.erp.quality;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.inventory.LotOwner;
import com.huilerie.erp.inventory.LotStatus;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.inventory.StockLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "qualite", authorities = {"QUALITY_VIEW", "QUALITY_MANAGE"})
class QualityServiceTest {

    @Autowired QualityService service;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired StockLotRepository lots;

    StockLot lot;
    Long acidite;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        Article huile = fx.article("EVQ", "15", true);
        lot = fx.lot(huile, "Q-1", LotOwner.ENTREPRISE, null);
        acidite = service.saveCriterion(null, new QualityDto.CriterionRequest("Acidité", "%", null, new BigDecimal("0.8"))).id();
    }

    @Test
    void nonConformeBloqueLeLotEtOuvreUneAnomalie() {
        QualityDto.Control c = service.record(new QualityDto.ControlRequest(lot.getId(),
                List.of(new QualityDto.ResultRequest(acidite, new BigDecimal("1.2"))), null, true));
        assertThat(c.resultat()).isEqualTo("NON_CONFORME");
        assertThat(lots.findById(lot.getId()).orElseThrow().getStatut()).isEqualTo(LotStatus.BLOQUE);
        assertThat(service.searchAnomalies("OUVERTE", null, 0, 10).totalElements()).isEqualTo(1);
    }

    @Test
    void conformeDebloqueLeLot() {
        service.record(new QualityDto.ControlRequest(lot.getId(), List.of(new QualityDto.ResultRequest(acidite, new BigDecimal("1.0"))), null, true));
        QualityDto.Control ok = service.record(new QualityDto.ControlRequest(lot.getId(),
                List.of(new QualityDto.ResultRequest(acidite, new BigDecimal("0.5"))), "Nouvelle analyse", true));
        assertThat(ok.resultat()).isEqualTo("CONFORME");
        assertThat(lots.findById(lot.getId()).orElseThrow().getStatut()).isEqualTo(LotStatus.DISPONIBLE);
    }
}
