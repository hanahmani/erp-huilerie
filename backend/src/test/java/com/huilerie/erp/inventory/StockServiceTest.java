package com.huilerie.erp.inventory;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.inventory.dto.StockDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "magasinier", authorities = {"STOCK_VIEW", "STOCK_ADJUST"})
class StockServiceTest {

    @Autowired StockService stockService;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;

    Article huile;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        huile = fx.article("EV5L", "80.000", false);
    }

    private StockDto.MovementRequest mvt(MovementType type, String qty) {
        return new StockDto.MovementRequest(huile.getId(), null, type, new BigDecimal(qty), "contrôle");
    }

    @Test
    void stockInitialPuisSortie() {
        stockService.manualMovement(mvt(MovementType.STOCK_INITIAL, "20"));
        stockService.manualMovement(mvt(MovementType.SORTIE, "5"));
        var state = stockService.stockState(null, null, false, 0, 20).content().get(0);
        assertThat(state.disponible()).isEqualByComparingTo("15.000");
    }

    @Test
    void stockNegatifImpossible() {
        stockService.manualMovement(mvt(MovementType.STOCK_INITIAL, "2"));
        assertThatThrownBy(() -> stockService.manualMovement(mvt(MovementType.SORTIE, "3")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Stock insuffisant");
    }

    @Test
    void stockInitialUneSeuleFois() {
        stockService.manualMovement(mvt(MovementType.STOCK_INITIAL, "2"));
        assertThatThrownBy(() -> stockService.manualMovement(mvt(MovementType.STOCK_INITIAL, "2")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void typesAutomatiquesInterditsALaMain() {
        assertThatThrownBy(() -> stockService.manualMovement(mvt(MovementType.VENTE, "1")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void plusDeTroisDecimalesRefusees() {
        assertThatThrownBy(() -> stockService.manualMovement(mvt(MovementType.STOCK_INITIAL, "1.0005")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(username = "caissier", authorities = {"STOCK_VIEW"})
    void ajustementSansPermissionRefuse() {
        assertThatThrownBy(() -> stockService.manualMovement(mvt(MovementType.STOCK_INITIAL, "1")))
                .isInstanceOf(AccessDeniedException.class);
    }
}
