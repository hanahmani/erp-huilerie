package com.huilerie.erp.reports;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.finance.CashMovementRepository;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.ExpenseRepository;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StockService;
import com.huilerie.erp.inventory.dto.StockDto;
import com.huilerie.erp.purchases.PurchaseRepository;
import com.huilerie.erp.sales.SaleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Rapports calculés à la demande à partir des données enregistrées. Aucun chiffre stocké ni inventé. */
@Service
public class ReportService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    private static final long MAX_DAYS = 400;

    private final SaleRepository sales;
    private final PurchaseRepository purchases;
    private final CashMovementRepository cash;
    private final ExpenseRepository expenses;
    private final ArticleRepository articles;
    private final StockService stock;
    private com.huilerie.erp.production.ProductionOrderRepository productions;
    private com.huilerie.erp.pressing.PressingRepository pressings;
    private com.huilerie.erp.packaging.PackagingOrderRepository packagings;

    @org.springframework.beans.factory.annotation.Autowired
    void setOperations(com.huilerie.erp.production.ProductionOrderRepository productions,
                       com.huilerie.erp.pressing.PressingRepository pressings,
                       com.huilerie.erp.packaging.PackagingOrderRepository packagings) {
        this.productions = productions;
        this.pressings = pressings;
        this.packagings = packagings;
    }

    public ReportService(SaleRepository sales, PurchaseRepository purchases, CashMovementRepository cash,
                         ExpenseRepository expenses, ArticleRepository articles, StockService stock) {
        this.sales = sales;
        this.purchases = purchases;
        this.cash = cash;
        this.expenses = expenses;
        this.articles = articles;
        this.stock = stock;
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("Choisissez une période valide.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new BusinessException("Période limitée à " + MAX_DAYS + " jours.");
        }
    }

    private static Instant start(LocalDate d) {
        return d.atStartOfDay(TUNIS).toInstant();
    }

    private static Instant end(LocalDate d) {
        return d.plusDays(1).atStartOfDay(TUNIS).toInstant();
    }

    private static BigDecimal dec(Object o) {
        return Money.ofAny(o);
    }

    private static long num(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    // ------------------------------------------------------------------ ventes

    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ReportDto.Sales sales(LocalDate from, LocalDate to) {
        checkRange(from, to);
        Instant f = start(from);
        Instant t = end(to);

        TreeMap<LocalDate, long[]> counts = new TreeMap<>();
        TreeMap<LocalDate, BigDecimal> totals = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            counts.put(d, new long[]{0});
            totals.put(d, Money.zero());
        }
        BigDecimal ca = Money.zero();
        long n = 0;
        for (Object[] r : sales.amountsBetween(f, t)) {
            LocalDate day = ((Instant) r[0]).atZone(TUNIS).toLocalDate();
            BigDecimal amount = dec(r[1]);
            counts.get(day)[0]++;
            totals.merge(day, amount, BigDecimal::add);
            ca = ca.add(amount);
            n++;
        }
        List<ReportDto.DayAmount> byDay = counts.keySet().stream()
                .map(d -> new ReportDto.DayAmount(d, counts.get(d)[0], totals.get(d))).toList();
        BigDecimal basket = n == 0 ? Money.zero() : ca.divide(BigDecimal.valueOf(n), Money.SCALE, RoundingMode.HALF_UP);

        List<ReportDto.Row> byArticle = sales.byArticle(f, t).stream()
                .map(r -> new ReportDto.Row((String) r[0], (String) r[1], dec(r[2]), num(r[4]), dec(r[3]), null)).toList();
        List<ReportDto.Row> byClient = sales.byClient(f, t).stream()
                .map(r -> new ReportDto.Row(r[0] == null ? "Client comptoir" : (String) r[0], null, null, num(r[1]),
                        dec(r[2]), dec(r[3]))).toList();
        List<ReportDto.Row> byCashier = sales.byCashier(f, t).stream()
                .map(r -> new ReportDto.Row(r[0] + " " + r[1], null, null, num(r[2]), dec(r[3]), null)).toList();

        return new ReportDto.Sales(from, to, ca, n, basket, Money.of(sales.discountsBetween(f, t)),
                sales.cancelledBetween(f, t), byDay, byArticle, byClient, byCashier);
    }

    // ------------------------------------------------------------------ achats

    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ReportDto.Purchases purchases(LocalDate from, LocalDate to) {
        checkRange(from, to);
        List<ReportDto.Row> rows = purchases.bySupplier(from, to).stream()
                .map(r -> new ReportDto.Row((String) r[0], null, dec(r[2]), num(r[1]), dec(r[3]), dec(r[4]))).toList();
        return new ReportDto.Purchases(from, to, Money.of(purchases.totalBetween(from, to)), rows,
                Money.of(purchases.totalPayable()));
    }

    // ------------------------------------------------------------------ stock valorisé

    /** Valorisation au dernier prix d'achat saisi sur l'article (méthode documentée dans docs/decisions.md). */
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ReportDto.Stock stock() {
        List<ReportDto.StockValue> lines = new ArrayList<>();
        BigDecimal total = Money.zero();
        long alerts = 0;
        long noPrice = 0;
        int page = 0;
        while (true) {
            var p = stock.stockStateUnchecked(null, null, false, page, 100);
            Map<Long, Article> byId = new HashMap<>();
            articles.findAllById(p.content().stream().map(StockDto.ArticleStock::articleId).toList())
                    .forEach(a -> byId.put(a.getId(), a));
            for (StockDto.ArticleStock s : p.content()) {
                Article a = byId.get(s.articleId());
                BigDecimal price = a == null ? null : a.getPrixAchat();
                BigDecimal value = price == null ? null : s.disponible().multiply(price).setScale(Money.SCALE, RoundingMode.HALF_UP);
                if (value != null) {
                    total = total.add(value);
                } else if (s.disponible().signum() > 0) {
                    noPrice++;
                }
                if (s.alerte()) {
                    alerts++;
                }
                lines.add(new ReportDto.StockValue(s.reference(), s.nom(), s.type(), s.unite(), s.disponible(), price, value, s.alerte()));
            }
            if (page + 1 >= p.totalPages()) {
                break;
            }
            page++;
        }
        return new ReportDto.Stock(total, lines.size(), alerts, noPrice, lines);
    }

    // ------------------------------------------------------------------ finance

    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ReportDto.Finance finance(LocalDate from, LocalDate to) {
        checkRange(from, to);
        Instant f = start(from);
        Instant t = end(to);
        Map<PaymentMode, BigDecimal[]> byMode = new EnumMap<>(PaymentMode.class);
        for (PaymentMode m : PaymentMode.values()) {
            byMode.put(m, new BigDecimal[]{Money.zero(), Money.zero()});
        }
        for (Object[] r : cash.byModeAndType(f, t)) {
            BigDecimal[] slot = byMode.get((PaymentMode) r[0]);
            slot[r[1] == CashMovementType.ENCAISSEMENT ? 0 : 1] = dec(r[2]);
        }
        List<ReportDto.Row> modes = byMode.entrySet().stream()
                .map(e -> new ReportDto.Row(e.getKey().name(), null, null, 0, e.getValue()[0], e.getValue()[1])).toList();
        BigDecimal in = Money.of(cash.total(CashMovementType.ENCAISSEMENT, f, t, null));
        BigDecimal out = Money.of(cash.total(CashMovementType.DECAISSEMENT, f, t, null));
        List<ReportDto.Row> cats = expenses.byCategory(from, to).stream()
                .map(r -> new ReportDto.Row((String) r[0], null, null, num(r[1]), dec(r[2]), null)).toList();
        return new ReportDto.Finance(from, to, in, out, in.subtract(out), modes, Money.of(expenses.totalBetween(from, to)),
                cats, Money.of(sales.totalOutstanding()), Money.of(purchases.totalPayable()));
    }

    // ------------------------------------------------------------------ production

    private static BigDecimal rendement(BigDecimal oil, BigDecimal kg) {
        return kg.signum() == 0 ? Money.zero() : oil.multiply(BigDecimal.valueOf(100)).divide(kg, 2, RoundingMode.HALF_UP);
    }

    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Transactional(readOnly = true)
    public ReportDto.Production production(LocalDate from, LocalDate to) {
        checkRange(from, to);
        Object[] prod = productions.stats(from, to).get(0);
        BigDecimal olives = dec(prod[1]);
        BigDecimal huile = dec(prod[2]);
        List<ReportDto.Row> cats = productions.byCategory(from, to).stream()
                .map(r -> new ReportDto.Row(r[0] == null ? "NON_CLASSEE" : r[0].toString(), "L", dec(r[2]), num(r[1]), null, null))
                .toList();

        Object[] pr = pressings.stats(start(from), end(to)).get(0);
        BigDecimal pKg = dec(pr[1]);
        BigDecimal pOil = dec(pr[2]);
        List<ReportDto.Row> varieties = pressings.yieldByVariety(start(from), end(to)).stream()
                .map(r -> new ReportDto.Row((String) r[0], "L/100 kg", rendement(dec(r[3]), dec(r[2])), num(r[1]), dec(r[2]), dec(r[3])))
                .toList();
        List<ReportDto.Row> pack = packagings.byProduct(start(from), end(to)).stream()
                .map(r -> new ReportDto.Row((String) r[0], null, dec(r[2]), num(r[1]), dec(r[3]), null)).toList();

        return new ReportDto.Production(from, to, num(prod[0]), olives, huile, rendement(huile, olives), cats,
                num(pr[0]), pKg, pOil, rendement(pOil, pKg), dec(pr[3]), varieties, pack);
    }
}
