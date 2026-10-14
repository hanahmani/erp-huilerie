package com.huilerie.erp.sales;

import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.PaymentMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Calculs de caisse, sans accès à la base : facile à tester.
 * Règles : seules les espèces peuvent dépasser le total (monnaie rendue) ;
 * carte, virement et chèque ne dépassent jamais le total.
 */
public final class SaleCalculator {

    private SaleCalculator() {
    }

    public record Totals(BigDecimal sousTotal, BigDecimal remise, BigDecimal total, BigDecimal recu,
                         BigDecimal paye, BigDecimal reste, BigDecimal monnaie, Map<PaymentMode, BigDecimal> affecte) {
    }

    public static BigDecimal lineTotal(BigDecimal quantite, BigDecimal prix) {
        return quantite.multiply(prix).setScale(Money.SCALE, RoundingMode.HALF_UP);
    }

    public static Totals compute(List<BigDecimal> lineTotals, BigDecimal remise, Map<PaymentMode, BigDecimal> payments) {
        BigDecimal sousTotal = Money.zero();
        for (BigDecimal t : lineTotals) {
            sousTotal = sousTotal.add(t);
        }
        BigDecimal r = Money.of(remise);
        if (r.compareTo(sousTotal) > 0) {
            throw new BusinessException("La remise ne peut pas dépasser le sous-total.");
        }
        BigDecimal total = sousTotal.subtract(r);

        BigDecimal especes = Money.of(payments.get(PaymentMode.ESPECES));
        BigDecimal autres = Money.zero();
        for (Map.Entry<PaymentMode, BigDecimal> e : payments.entrySet()) {
            if (e.getKey() != PaymentMode.ESPECES) {
                autres = autres.add(Money.of(e.getValue()));
            }
        }
        if (autres.compareTo(total) > 0) {
            throw new BusinessException("Les paiements par carte, virement ou chèque ne peuvent pas dépasser le total.");
        }
        BigDecimal recu = especes.add(autres);
        BigDecimal monnaie = recu.compareTo(total) > 0 ? recu.subtract(total) : Money.zero();
        BigDecimal paye = recu.subtract(monnaie);
        BigDecimal reste = total.subtract(paye);

        Map<PaymentMode, BigDecimal> affecte = new EnumMap<>(PaymentMode.class);
        payments.forEach((mode, amount) -> {
            BigDecimal value = Money.of(amount);
            if (mode == PaymentMode.ESPECES) {
                value = value.subtract(monnaie);
            }
            if (value.signum() > 0) {
                affecte.put(mode, value);
            }
        });
        return new Totals(sousTotal, r, total, recu, paye, reste, monnaie, affecte);
    }
}
