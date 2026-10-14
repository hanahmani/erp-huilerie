package com.huilerie.erp.packaging;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.inventory.LotOwner;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.packaging.dto.PackagingDto;
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

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "chef", authorities = {"PACKAGING_VIEW", "PACKAGING_MANAGE"})
class PackagingServiceTest {

    @Autowired PackagingService service;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired StockBalanceRepository balances;

    Article vrac;
    Article bidon5;
    Article emballage;
    StockLot lot;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        vrac = fx.articleOf("VRAC", ArticleType.HUILE_VRAC, "L", true, null);
        bidon5 = fx.articleOf("B5L", ArticleType.HUILE_CONDITIONNEE, "U", false, "5");
        emballage = fx.articleOf("BID-VIDE", ArticleType.EMBALLAGE, "U", false, null);
        lot = fx.lot(vrac, "L-1", LotOwner.ENTREPRISE, null);
        fx.receive(vrac, lot, "100");
        fx.receive(emballage, null, "30");
    }

    private BigDecimal qty(Article a, long ref) {
        return balances.findByArticleIdAndLotRef(a.getId(), ref).map(b -> b.getQuantite()).orElse(BigDecimal.ZERO);
    }

    @Test
    void conditionnementPuisAnnulation() {
        PackagingDto.Detail o = service.create(new PackagingDto.Request(vrac.getId(), lot.getId(), bidon5.getId(),
                new BigDecimal("10"), List.of(new PackagingDto.ConsumptionRequest(emballage.getId(), new BigDecimal("10"))), null));
        assertThat(o.volumeUtilise()).isEqualByComparingTo("50");
        assertThat(qty(vrac, lot.getId())).isEqualByComparingTo("50");
        assertThat(qty(bidon5, 0)).isEqualByComparingTo("10");
        assertThat(qty(emballage, 0)).isEqualByComparingTo("20");

        service.cancel(o.id(), "erreur de format");
        assertThat(qty(vrac, lot.getId())).isEqualByComparingTo("100");
        assertThat(qty(bidon5, 0)).isEqualByComparingTo("0");
        assertThat(qty(emballage, 0)).isEqualByComparingTo("30");
    }

    @Test
    void volumeInsuffisantRefuse() {
        assertThatThrownBy(() -> service.create(new PackagingDto.Request(vrac.getId(), lot.getId(), bidon5.getId(),
                new BigDecimal("21"), List.of(), null))).isInstanceOf(BusinessException.class);
        assertThat(qty(vrac, lot.getId())).isEqualByComparingTo("100");
    }

    @Test
    void huileDUnClientNonConditionnable() {
        StockLot clientLot = fx.lot(vrac, "P-1", LotOwner.CLIENT, fx.client("Salah", null));
        fx.receive(vrac, clientLot, "20");
        assertThatThrownBy(() -> service.create(new PackagingDto.Request(vrac.getId(), clientLot.getId(), bidon5.getId(),
                BigDecimal.ONE, List.of(), null))).isInstanceOf(BusinessException.class);
    }

    @Test
    void contenanceObligatoire() {
        Article sansContenance = fx.articleOf("BX", ArticleType.HUILE_CONDITIONNEE, "U", false, null);
        assertThatThrownBy(() -> service.create(new PackagingDto.Request(vrac.getId(), lot.getId(), sansContenance.getId(),
                BigDecimal.ONE, List.of(), null))).isInstanceOf(BusinessException.class);
    }
}
