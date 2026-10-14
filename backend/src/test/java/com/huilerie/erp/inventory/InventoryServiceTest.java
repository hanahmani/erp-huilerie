package com.huilerie.erp.inventory;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.inventory.dto.InventoryDto;
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
@WithMockUser(username = "magasinier", authorities = {"STOCK_VIEW", "STOCK_ADJUST"})
class InventoryServiceTest {

    @Autowired InventoryService service;
    @Autowired StockBalanceRepository balances;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;

    Article huile;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        huile = fx.article("INV1", "10", false);
        fx.receive(huile, null, "50");
    }

    @Test
    void validationRameneLeStockAuComptage() {
        InventoryDto.Detail inv = service.create(new InventoryDto.CreateRequest("Fin de campagne", List.of(huile.getId())));
        assertThat(inv.lignes()).hasSize(1);
        assertThat(inv.lignes().get(0).theorique()).isEqualByComparingTo("50");

        Long lineId = inv.lignes().get(0).id();
        service.count(inv.entete().id(), new InventoryDto.CountRequest(List.of(new InventoryDto.CountLine(lineId, new BigDecimal("47")))));
        InventoryDto.Detail done = service.validate(inv.entete().id());

        assertThat(done.entete().statut()).isEqualTo("VALIDE");
        assertThat(balances.findByArticleIdAndLotRef(huile.getId(), 0L).orElseThrow().getQuantite()).isEqualByComparingTo("47");
        assertThatThrownBy(() -> service.validate(inv.entete().id())).isInstanceOf(BusinessException.class);
    }

    @Test
    void validationSansComptageRefusee() {
        InventoryDto.Detail inv = service.create(new InventoryDto.CreateRequest("Contrôle", List.of(huile.getId())));
        assertThatThrownBy(() -> service.validate(inv.entete().id())).isInstanceOf(BusinessException.class);
    }
}
