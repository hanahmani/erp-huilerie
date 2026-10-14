package com.huilerie.erp.sales;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.CashMovementRepository;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.LotOwner;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.inventory.StockMovementRepository;
import com.huilerie.erp.sales.dto.SaleDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "caissier1", authorities = {"SALES_CREATE", "SALES_VIEW"})
class SaleServiceTest {

    @Autowired SaleService saleService;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired SaleRepository saleRepository;
    @Autowired StockBalanceRepository balances;
    @Autowired StockMovementRepository movements;
    @Autowired CashMovementRepository cash;

    Article huile;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        testUsers.create("caissier1", false, "CAISSIER");
        huile = fx.article("EV1L", "18.000", false);
        fx.receive(huile, null, "10");
    }

    private SaleDto.QuickSaleRequest request(String qty, String cash, Long clientId, String remise) {
        return new SaleDto.QuickSaleRequest(UUID.randomUUID().toString(), clientId,
                remise == null ? null : new BigDecimal(remise),
                List.of(new SaleDto.LineRequest(huile.getId(), null, new BigDecimal(qty), null)),
                cash == null ? List.of() : List.of(new SaleDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal(cash))));
    }

    private BigDecimal stockOf(Article a) {
        return balances.findByArticleIdAndLotRef(a.getId(), 0L).map(b -> b.getQuantite()).orElse(BigDecimal.ZERO);
    }

    @Test
    void venteValideeDeduitLeStockEtEncaisse() {
        SaleDto.Detail sale = saleService.createQuickSale(request("2", "40", null, null));

        assertThat(sale.numeroTicket()).matches("TCK-\\d{4}-000001");
        assertThat(sale.total()).isEqualByComparingTo("36.000");
        assertThat(sale.monnaieRendue()).isEqualByComparingTo("4.000");
        assertThat(sale.paiementStatut()).isEqualTo("PAYEE");
        assertThat(stockOf(huile)).isEqualByComparingTo("8.000");
        assertThat(movements.findByDocumentTypeAndDocumentIdOrderByIdAsc("VENTE", sale.id())).hasSize(1);
        assertThat(cash.total(CashMovementType.ENCAISSEMENT, null, null, null)).isEqualByComparingTo("36.000");
    }

    @Test
    void numerosDeTicketSequentiels() {
        String n1 = saleService.createQuickSale(request("1", "18", null, null)).numeroTicket();
        String n2 = saleService.createQuickSale(request("1", "18", null, null)).numeroTicket();
        assertThat(n1).endsWith("000001");
        assertThat(n2).endsWith("000002");
    }

    @Test
    void stockInsuffisantRefuseSansRienEnregistrer() {
        assertThatThrownBy(() -> saleService.createQuickSale(request("11", "300", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuffisant");
        assertThat(saleRepository.count()).isZero();
        assertThat(stockOf(huile)).isEqualByComparingTo("10.000");
        assertThat(cash.count()).isZero();
        // l'échec n'a pas consommé de numéro : la vente suivante reste n° 1
        assertThat(saleService.createQuickSale(request("1", "18", null, null)).numeroTicket()).endsWith("000001");
    }

    @Test
    void memeCleNeCreeJamaisDeuxVentes() {
        SaleDto.QuickSaleRequest r = request("1", "18", null, null);
        Long first = saleService.createQuickSale(r).id();
        Long second = saleService.createQuickSale(r).id();
        assertThat(second).isEqualTo(first);
        assertThat(saleRepository.count()).isEqualTo(1);
        assertThat(stockOf(huile)).isEqualByComparingTo("9.000");
    }

    @Test
    void paiementIncompletSansClientRefuse() {
        assertThatThrownBy(() -> saleService.createQuickSale(request("2", "10", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("client identifié");
    }

    @Test
    void creditRefuseQuandDesactiveDansLesParametres() {
        Client c = fx.client("Ali", null);
        assertThatThrownBy(() -> saleService.createQuickSale(request("2", "10", c.getId(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("désactivées");
    }

    @Test
    void remiseSansPermissionRefusee() {
        assertThatThrownBy(() -> saleService.createQuickSale(request("1", "18", null, "1")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void prixModifieSansPermissionRefuse() {
        var r = new SaleDto.QuickSaleRequest(UUID.randomUUID().toString(), null, null,
                List.of(new SaleDto.LineRequest(huile.getId(), null, BigDecimal.ONE, new BigDecimal("10"))),
                List.of(new SaleDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("10"))));
        assertThatThrownBy(() -> saleService.createQuickSale(r)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void huileDUnClientNeSeVendJamais() {
        Article vrac = fx.article("VRAC", "15.000", true);
        Client owner = fx.client("Mohamed", null);
        StockLot lotClient = fx.lot(vrac, "P-001", LotOwner.CLIENT, owner);
        fx.receive(vrac, lotClient, "100");

        var r = new SaleDto.QuickSaleRequest(UUID.randomUUID().toString(), null, null,
                List.of(new SaleDto.LineRequest(vrac.getId(), lotClient.getId(), BigDecimal.ONE, null)),
                List.of(new SaleDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("15"))));
        assertThatThrownBy(() -> saleService.createQuickSale(r))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("appartient à un client");
        assertThat(saleService.sellableCatalog().stream().filter(a -> a.id().equals(vrac.getId()))
                .findFirst().orElseThrow().disponible()).isEqualByComparingTo("0");
    }

    @Test
    void reimpressionNeTouchePasAuStock() {
        SaleDto.Detail sale = saleService.createQuickSale(request("1", "18", null, null));
        saleService.recordPrint(sale.id());
        SaleDto.Detail again = saleService.recordPrint(sale.id());
        assertThat(again.nbImpressions()).isEqualTo(2);
        assertThat(saleRepository.count()).isEqualTo(1);
        assertThat(stockOf(huile)).isEqualByComparingTo("9.000");
    }

    @Test
    @WithMockUser(username = "caissier1", authorities = {"SALES_CREATE", "SALES_VIEW", "SALES_CANCEL"})
    void annulationRemetEnStockEtDecaisse() {
        SaleDto.Detail sale = saleService.createQuickSale(request("3", "54", null, null));
        SaleDto.Detail cancelled = saleService.cancel(sale.id(), "Erreur de saisie");

        assertThat(cancelled.statut()).isEqualTo(SaleStatus.ANNULEE);
        assertThat(stockOf(huile)).isEqualByComparingTo("10.000");
        assertThat(cash.total(CashMovementType.DECAISSEMENT, null, null, null)).isEqualByComparingTo("54.000");
        assertThatThrownBy(() -> saleService.cancel(sale.id(), "encore")).isInstanceOf(BusinessException.class);
    }

    @Test
    void caissierSansPermissionNePeutPasAnnuler() {
        SaleDto.Detail sale = saleService.createQuickSale(request("1", "18", null, null));
        assertThatThrownBy(() -> saleService.cancel(sale.id(), "test")).isInstanceOf(AccessDeniedException.class);
    }
}
