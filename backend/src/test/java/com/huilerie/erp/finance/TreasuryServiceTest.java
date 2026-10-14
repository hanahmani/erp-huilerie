package com.huilerie.erp.finance;

import com.huilerie.erp.TestUsers;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.dto.CashDto;
import com.huilerie.erp.settings.ReferenceService;
import com.huilerie.erp.settings.dto.RefDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "comptable", authorities = {"FINANCE_VIEW", "FINANCE_MANAGE", "CASH_SESSION", "SETTINGS_MANAGE"})
class TreasuryServiceTest {

    @Autowired TreasuryService treasury;
    @Autowired ReferenceService references;
    @Autowired CashMovementRepository cash;
    @Autowired TestUsers testUsers;

    @BeforeEach
    void setUp() {
        testUsers.reset();
    }

    @Test
    void depensePuisAnnulationParContrePassation() {
        Long cat = references.saveExpenseCategory(null, new RefDto.ExpenseCategoryRequest("Électricité")).id();
        CashDto.ExpenseLine e = treasury.createExpense(new CashDto.ExpenseRequest(cat, "Facture STEG", new BigDecimal("120.5"),
                PaymentMode.ESPECES, null, LocalDate.now()));
        assertThat(cash.total(CashMovementType.DECAISSEMENT, null, null, null)).isEqualByComparingTo("120.500");

        treasury.cancelExpense(e.id(), "doublon");
        assertThat(cash.total(CashMovementType.ENCAISSEMENT, null, null, null)).isEqualByComparingTo("120.500");
        assertThatThrownBy(() -> treasury.cancelExpense(e.id(), "encore")).isInstanceOf(BusinessException.class);
    }

    @Test
    void uneSeuleCaisseOuverte() {
        treasury.open(new BigDecimal("50"));
        assertThatThrownBy(() -> treasury.open(BigDecimal.ZERO)).isInstanceOf(BusinessException.class);
    }

    @Test
    void fermetureAvecEcartExigeUneJustification() {
        treasury.open(new BigDecimal("50"));
        assertThatThrownBy(() -> treasury.close(new BigDecimal("48"), null)).isInstanceOf(BusinessException.class);
        CashDto.Session closed = treasury.close(new BigDecimal("48"), "pièce de 2 DT manquante");
        assertThat(closed.ecart()).isEqualByComparingTo("-2.000");
        assertThat(closed.ouverte()).isFalse();
        assertThat(treasury.current()).isNull();
        treasury.open(new BigDecimal("48"));   // une nouvelle session peut être ouverte
    }
}
