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
        huileClient = fx.articleOf("HCLI", ArticleType.HUILE_VRAC, "L", true);
    }

    private PressingDto.Detail receive() {
        return service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), "Chemlali", "Sfax",
                Pressing.OliveState.SAINES, new BigDecimal("520"), new BigDecimal("20"), new BigDecimal("0.300"), null, null, null, null, null, null, null));
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
        PressingDto.Detail done = service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("100"), null, null));
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
        service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("100"), null, null));

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
    void tarifParDefautDe200MillimesSiAucunNEstSaisi() {
        PressingDto.Detail p = service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), null, null,
                Pressing.OliveState.SAINES, new BigDecimal("520"), new BigDecimal("20"), null, null, null, null, null, null, null, null));
        assertThat(p.tarifKg()).isEqualByComparingTo("0.200");
        assertThat(p.montant()).isEqualByComparingTo("100.000");
    }

    @Test
    void tareSuperieureAuBrutRefusee() {
        assertThatThrownBy(() -> service.receive(new PressingDto.ReceiveRequest(client.getId(), huileClient.getId(), null, null,
                Pressing.OliveState.SAINES, new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("0.3"), null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void suppressionDUnBulletinNonPresse() {
        PressingDto.Detail recu = receive();
        service.delete(recu.id());
        assertThatThrownBy(() -> service.get(recu.id())).isInstanceOf(com.huilerie.erp.common.exception.NotFoundException.class);

        PressingDto.Detail annule = receive();
        service.cancel(annule.id(), "erreur");
        service.delete(annule.id());
        assertThatThrownBy(() -> service.get(annule.id())).isInstanceOf(com.huilerie.erp.common.exception.NotFoundException.class);
    }

    @Test
    @WithMockUser(username = "peseur", authorities = {"PRESSING_VIEW", "PRESSING_MANAGE", "SALES_CREATE"})
    void suppressionRefuseeSiPayeOuTermine() {
        PressingDto.Detail paye = receive();
        service.pay(paye.id(), new PressingDto.PaymentRequest(PaymentMode.ESPECES, new BigDecimal("50"), null));
        assertThatThrownBy(() -> service.delete(paye.id())).isInstanceOf(BusinessException.class).hasMessageContaining("payé");

        PressingDto.Detail fini = receive();
        service.start(fini.id());
        service.finish(fini.id(), new PressingDto.FinishRequest(new BigDecimal("90"), null, null));
        assertThatThrownBy(() -> service.delete(fini.id())).isInstanceOf(BusinessException.class).hasMessageContaining("terminé");
    }

    @Test
    void annulationImpossibleApresLaFin() {
        PressingDto.Detail p = receive();
        service.start(p.id());
        service.finish(p.id(), new PressingDto.FinishRequest(new BigDecimal("90"), null, null));
        assertThatThrownBy(() -> service.cancel(p.id(), "erreur")).isInstanceOf(BusinessException.class);
    }
}
