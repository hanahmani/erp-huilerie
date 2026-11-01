package com.huilerie.erp.catalog;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.dto.ArticleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Référence automatique des articles : préfixe du type + numéro, un compteur par type. */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "stock", authorities = {"CATALOG_VIEW", "CATALOG_MANAGE"})
class ArticleReferenceTest {

    @Autowired CatalogService service;
    @Autowired UnitRepository units;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;

    Unit litre;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        litre = units.save(new Unit("L", "Litre"));
    }

    private ArticleDto.Response create(String nom, ArticleType type) {
        return service.saveArticle(null, request(nom, type));
    }

    private ArticleDto.Request request(String nom, ArticleType type) {
        return new ArticleDto.Request(nom, null, type, null, litre.getId(), null, null, null, null, null, null, false,
                type == ArticleType.HUILE_VRAC);
    }

    @Test
    void prefixeDuTypeEtNumeroParType() {
        assertThat(create("Huile en vrac", ArticleType.HUILE_VRAC).reference()).isEqualTo("HVR-0001");
        assertThat(create("Huile en vrac extra vierge", ArticleType.HUILE_VRAC).reference()).isEqualTo("HVR-0002");
        assertThat(create("Olives Chemlali", ArticleType.OLIVES).reference()).isEqualTo("OLV-0001");
        assertThat(create("Bouteille verre 1 L", ArticleType.EMBALLAGE).reference()).isEqualTo("EMB-0001");
    }

    @Test
    void uneReferenceDejaPriseALaMainEstSautee() {
        Article manuel = fx.articleOf("HVR-0001", ArticleType.HUILE_VRAC, "L", true, null);
        assertThat(manuel.getReference()).isEqualTo("HVR-0001");
        assertThat(create("Huile en vrac", ArticleType.HUILE_VRAC).reference()).isEqualTo("HVR-0002");
    }

    @Test
    void laReferenceNeChangePasALaModification() {
        ArticleDto.Response a = create("Huile en vrac", ArticleType.HUILE_VRAC);
        ArticleDto.Request renamed = new ArticleDto.Request("Huile en vrac extra vierge", null, ArticleType.HUILE_VRAC, null,
                litre.getId(), null, null, new BigDecimal("15.000"), null, null, null, true, true);
        assertThat(service.saveArticle(a.id(), renamed).reference()).isEqualTo("HVR-0001");
    }
}
