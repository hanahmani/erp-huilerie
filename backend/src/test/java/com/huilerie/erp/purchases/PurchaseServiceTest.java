package com.huilerie.erp.purchases;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.CashMovementRepository;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.purchases.dto.PurchaseDto;
import com.huilerie.erp.suppliers.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "acheteur", authorities = {"PURCHASE_VIEW", "PURCHASE_CREATE", "PURCHASE_VALIDATE",
        "PURCHASE_RECEIVE", "FINANCE_MANAGE"})
class PurchaseServiceTest {

    @Autowired PurchaseService service;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired StockBalanceRepository balances;
    @Autowired CashMovementRepository cash;

    Article bouteille;
    Supplier fournisseur;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        bouteille = fx.article("BT1L", "1.000", false);
        fournisseur = fx.supplier("Verrerie");
    }

    private PurchaseDto.Detail order(String qty, String price, String remise) {
        return service.create(new PurchaseDto.Request(fournisseur.getId(), LocalDate.now(), null, null,
                List.of(new PurchaseDto.LineRequest(bouteille.getId(), new BigDecimal(qty), new BigDecimal(price),
                        remise == null ? null : new BigDecimal(remise), null))));
    }

    private BigDecimal stock() {
        return balances.findByArticleIdAndLotRef(bouteille.getId(), 0L).map(b -> b.getQuantite()).orElse(BigDecimal.ZERO);
    }

    @Test
    void totauxAvecRemise() {
        PurchaseDto.Detail p = order("100", "0.450", "10");
        assertThat(p.totalHt()).isEqualByComparingTo("40.500");
        assertThat(p.totalTtc()).isEqualByComparingTo("40.500");
        assertThat(p.numero()).startsWith("ACH-");
    }

    @Test
    void laCommandeNeModifiePasLeStockAvantReception() {
        PurchaseDto.Detail p = order("100", "0.5", null);
        service.validate(p.id());
        assertThat(stock()).isEqualByComparingTo("0");
    }

    @Test
    void receptionPartiellePuisComplete() {
        PurchaseDto.Detail p = service.validate(order("100", "0.5", null).id());
        Long lineId = p.lignes().get(0).id();

        PurchaseDto.Detail partial = service.receive(p.id(), new PurchaseDto.ReceiveRequest(
                List.of(new PurchaseDto.ReceiveLine(lineId, new BigDecimal("60"), null)), null));
        assertThat(partial.statut()).isEqualTo(PurchaseStatus.PARTIELLEMENT_RECUE);
        assertThat(stock()).isEqualByComparingTo("60");

        assertThatThrownBy(() -> service.receive(p.id(), new PurchaseDto.ReceiveRequest(
                List.of(new PurchaseDto.ReceiveLine(lineId, new BigDecimal("41"), null)), null)))
                .isInstanceOf(BusinessException.class);

        PurchaseDto.Detail full = service.receive(p.id(), new PurchaseDto.ReceiveRequest(
                List.of(new PurchaseDto.ReceiveLine(lineId, new BigDecimal("40"), null)), null));
        assertThat(full.statut()).isEqualTo(PurchaseStatus.RECUE);
        assertThat(stock()).isEqualByComparingTo("100");
        assertThat(full.receptions()).hasSize(2);
    }

    @Test
    void brouillonNonReceptionnable() {
        PurchaseDto.Detail p = order("10", "1", null);
        assertThatThrownBy(() -> service.receive(p.id(), new PurchaseDto.ReceiveRequest(
                List.of(new PurchaseDto.ReceiveLine(p.lignes().get(0).id(), BigDecimal.ONE, null)), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void commandeValideeNonModifiable() {
        PurchaseDto.Detail p = service.validate(order("10", "1", null).id());
        assertThatThrownBy(() -> service.update(p.id(), new PurchaseDto.Request(fournisseur.getId(), LocalDate.now(), null,
                null, List.of(new PurchaseDto.LineRequest(bouteille.getId(), BigDecimal.TEN, BigDecimal.ONE, null, null)))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void paiementsPartielsEtPlafondAuResteDu() {
        PurchaseDto.Detail p = service.validate(order("100", "0.5", null).id());
        PurchaseDto.Detail after = service.pay(p.id(), new PurchaseDto.PaymentRequest(PaymentMode.VIREMENT, new BigDecimal("20"), "VIR-1"));
        assertThat(after.reste()).isEqualByComparingTo("30.000");
        assertThat(cash.total(CashMovementType.DECAISSEMENT, null, null, null)).isEqualByComparingTo("20.000");
        assertThatThrownBy(() -> service.pay(p.id(), new PurchaseDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("31"), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void annulationImpossibleApresReception() {
        PurchaseDto.Detail p = service.validate(order("10", "1", null).id());
        service.receive(p.id(), new PurchaseDto.ReceiveRequest(
                List.of(new PurchaseDto.ReceiveLine(p.lignes().get(0).id(), BigDecimal.ONE, null)), null));
        assertThatThrownBy(() -> service.cancel(p.id(), "erreur")).isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(username = "magasinier", authorities = {"PURCHASE_VIEW", "PURCHASE_CREATE"})
    void validationSansPermissionRefusee() {
        PurchaseDto.Detail p = order("10", "1", null);
        assertThatThrownBy(() -> service.validate(p.id())).isInstanceOf(AccessDeniedException.class);
    }
}
