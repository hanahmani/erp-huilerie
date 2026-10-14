package com.huilerie.erp.pressing;

import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.LotOwner;
import com.huilerie.erp.inventory.StockBalanceRepository;
import com.huilerie.erp.inventory.StockLotRepository;
import com.huilerie.erp.pressing.dto.PressingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(username = "peseur", authorities = {"PRESSING_VIEW", "PRESSING_MANAGE"})
class PressingServiceTest {

    @Autowired PressingService service;
    @Autowired SalesFixtures fx;
    @Autowired TestUsers testUsers;
    @Autowired StockLotRepository lots;
    @Autowired StockBalanceRepository balances;

    Client client;
    Article huileClient;

    @BeforeEach
    void setUp() {
        testUsers.reset();
        client = fx.client("Mohamed", null);
        huileClient = fx.articleOf("HCLI", ArticleType.HUILE_VRAC, "L", true, null);
    }

    private PressingDto.Detail receive() {
        return service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), "Chemlali", "Sfax",
                Pressing.OliveState.SAINES, new BigDecimal("520"), new BigDecimal("20"), new BigDecimal("0.300"), null));
    }

    @Test
    void peseeEtCalculDuService() {
        PressingDto.Detail p = receive();
        assertThat(p.poidsNet()).isEqualByComparingTo("500");
        assertThat(p.montant()).isEqualByComparingTo("150.000");
        assertThat(p.statut()).isEqualTo(Pressing.Status.RECU);
    }

    @Test
    void lHuileObtenueAppartientAuClient() {
        PressingDto.Detail p = receive();
        service.start(p.id());
        PressingDto.Detail done = service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("100"), null));
        assertThat(done.rendement()).isEqualByComparingTo("20.00");
        var lot = lots.findAll().stream().filter(l -> l.getNumero().equals(p.numero())).findFirst().orElseThrow();
        assertThat(lot.getProprietaire()).isEqualTo(LotOwner.CLIENT);
        assertThat(lot.isSellable()).isFalse();
        assertThat(balances.findByArticleIdAndLotRef(huileClient.getId(), lot.getId()).orElseThrow().getQuantite())
                .isEqualByComparingTo("100");
    }

    @Test
    @WithMockUser(username = "peseur", authorities = {"PRESSING_VIEW", "PRESSING_MANAGE", "SALES_CREATE"})
    void remiseApresPaiementSeulementPuisSoldee() {
        PressingDto.Detail p = receive();
        service.start(p.id());
        service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("100"), null));

        assertThatThrownBy(() -> service.returnOil(p.id(), new PressingDto.ReturnRequest(new BigDecimal("10"), null)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("payé");

        service.pay(p.id(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("150"), null));
        service.returnOil(p.id(), new PressingDto.ReturnRequest(new BigDecimal("60"), null));
        assertThatThrownBy(() -> service.returnOil(p.id(), new PressingDto.ReturnRequest(new BigDecimal("41"), null)))
                .isInstanceOf(BusinessException.class);
        PressingDto.Detail end = service.returnOil(p.id(), new PressingDto.ReturnRequest(new BigDecimal("40"), "Fils du client"));
        assertThat(end.statut()).isEqualTo(Pressing.Status.REMIS);
        assertThat(end.huileARemettre()).isEqualByComparingTo("0");
    }

    @Test
    void tareSuperieureAuBrutRefusee() {
        assertThatThrownBy(() -> service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), null, null,
                Pressing.OliveState.SAINES, new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("0.3"), null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void annulationImpossibleApresLaFin() {
        PressingDto.Detail p = receive();
        service.start(p.id());
        service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("90"), null));
        assertThatThrownBy(() -> service.cancel(p.id(), "erreur")).isInstanceOf(BusinessException.class);
    }
}
