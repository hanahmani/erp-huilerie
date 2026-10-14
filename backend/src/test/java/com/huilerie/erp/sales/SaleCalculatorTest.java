package com.huilerie.erp.sales;

import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.PaymentMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Calculs de caisse purs, sans base de données. */
class SaleCalculatorTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static Map<PaymentMode, BigDecimal> pay(PaymentMode mode, String amount) {
        Map<PaymentMode, BigDecimal> m = new EnumMap<>(PaymentMode.class);
        m.put(mode, d(amount));
        return m;
    }

    @Test
    void totalLigneArrondiAuMillime() {
        assertThat(SaleCalculator.lineTotal(d("1.250"), d("18.333"))).isEqualByComparingTo("22.916");
    }

    @Test
    void sousTotalRemiseEtMonnaieARendre() {
        var t = SaleCalculator.compute(List.of(d("36.000"), d("12.500")), d("1.500"), pay(PaymentMode.ESPECES, "50"));
        assertThat(t.sousTotal()).isEqualByComparingTo("48.500");
        assertThat(t.total()).isEqualByComparingTo("47.000");
        assertThat(t.monnaie()).isEqualByComparingTo("3.000");
        assertThat(t.paye()).isEqualByComparingTo("47.000");
        assertThat(t.reste()).isEqualByComparingTo("0");
        assertThat(t.affecte().get(PaymentMode.ESPECES)).isEqualByComparingTo("47.000");
    }

    @Test
    void paiementMixteEspecesEtCarte() {
        Map<PaymentMode, BigDecimal> p = pay(PaymentMode.CARTE, "30");
        p.put(PaymentMode.ESPECES, d("20"));
        var t = SaleCalculator.compute(List.of(d("45.000")), null, p);
        assertThat(t.monnaie()).isEqualByComparingTo("5.000");
        assertThat(t.affecte().get(PaymentMode.ESPECES)).isEqualByComparingTo("15.000");
        assertThat(t.affecte().get(PaymentMode.CARTE)).isEqualByComparingTo("30.000");
    }

    @Test
    void carteNePeutPasDepasserLeTotal() {
        assertThatThrownBy(() -> SaleCalculator.compute(List.of(d("10")), null, pay(PaymentMode.CARTE, "12")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void remiseSuperieureAuSousTotalRefusee() {
        assertThatThrownBy(() -> SaleCalculator.compute(List.of(d("10")), d("11"), Map.of()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void paiementPartielLaisseUnReste() {
        var t = SaleCalculator.compute(List.of(d("100")), null, pay(PaymentMode.ESPECES, "40"));
        assertThat(t.reste()).isEqualByComparingTo("60.000");
        assertThat(t.monnaie()).isEqualByComparingTo("0");
    }
}
