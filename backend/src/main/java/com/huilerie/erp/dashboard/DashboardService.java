package com.huilerie.erp.dashboard;

import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.clients.ClientRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.dashboard.dto.DashboardResponse;
import com.huilerie.erp.finance.CashMovementRepository;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.ExpenseRepository;
import com.huilerie.erp.finance.TreasuryService;
import com.huilerie.erp.purchases.PurchaseRepository;
import com.huilerie.erp.purchases.PurchaseStatus;
import com.huilerie.erp.inventory.LotStatus;
import com.huilerie.erp.inventory.StockLotRepository;
import com.huilerie.erp.inventory.StockService;
import com.huilerie.erp.sales.SaleRepository;
import com.huilerie.erp.sales.dto.SaleDto;
import com.huilerie.erp.suppliers.SupplierRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Tableau de bord : chaque bloc n'est calculé que si l'utilisateur peut voir ce module. */
@Service
public class DashboardService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final SaleRepository sales;
    private final CashMovementRepository cash;
    private final ClientRepository clients;
    private final SupplierRepository suppliers;
    private final ArticleRepository articles;
    private final StockLotRepository lots;
    private final StockService stock;
    private final PurchaseRepository purchases;
    private final ExpenseRepository expenses;
    private final com.huilerie.erp.finance.CashSessionRepository cashSessions;
    private com.huilerie.erp.pressing.PressingRepository pressings;
    private com.huilerie.erp.production.ProductionOrderRepository productions;

    @org.springframework.beans.factory.annotation.Autowired
    void setOperations(com.huilerie.erp.pressing.PressingRepository pressings,
                       com.huilerie.erp.production.ProductionOrderRepository productions) {
        this.pressings = pressings;
        this.productions = productions;
    }

    public DashboardService(SaleRepository sales, CashMovementRepository cash, ClientRepository clients,
                            SupplierRepository suppliers, ArticleRepository articles, StockLotRepository lots,
                            StockService stock, PurchaseRepository purchases, ExpenseRepository expenses,
                            com.huilerie.erp.finance.CashSessionRepository cashSessions) {
        this.purchases = purchases;
        this.expenses = expenses;
        this.cashSessions = cashSessions;
        this.sales = sales;
        this.cash = cash;
        this.clients = clients;
        this.suppliers = suppliers;
        this.articles = articles;
        this.lots = lots;
        this.stock = stock;
    }

    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    @Transactional(readOnly = true)
    public DashboardResponse build() {
        LocalDate today = LocalDate.now(TUNIS);
        Instant dayStart = today.atStartOfDay(TUNIS).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(TUNIS).toInstant();
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(TUNIS).toInstant();

        boolean salesView = SecurityUtils.has("SALES_VIEW");
        boolean financeView = SecurityUtils.has("FINANCE_VIEW");
        boolean stockView = SecurityUtils.has("STOCK_VIEW");
        boolean purchaseView = SecurityUtils.has("PURCHASE_VIEW");
        boolean cashView = SecurityUtils.has("CASH_SESSION") || financeView;
        boolean pressingView = SecurityUtils.has("PRESSING_VIEW");
        boolean productionView = SecurityUtils.has("PRODUCTION_VIEW");
        Object[] monthProd = productionView ? productions.stats(today.withDayOfMonth(1), today).get(0) : null;

        List<DashboardResponse.DayTotal> week = null;
        if (salesView) {
            week = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                week.add(new DashboardResponse.DayTotal(d.toString(), Money.of(sales.totalBetween(
                        d.atStartOfDay(TUNIS).toInstant(), d.plusDays(1).atStartOfDay(TUNIS).toInstant()))));
            }
        }

        return new DashboardResponse(
                salesView ? Money.of(sales.totalBetween(dayStart, dayEnd)) : null,
                salesView ? sales.countBetween(dayStart, dayEnd) : null,
                salesView ? Money.of(sales.totalBetween(monthStart, dayEnd)) : null,
                financeView ? net(dayStart, dayEnd) : null,
                salesView ? Money.of(sales.totalOutstanding()) : null,
                SecurityUtils.has("CLIENT_VIEW") ? clients.countByActifTrue() : null,
                SecurityUtils.has("SUPPLIER_VIEW") ? suppliers.countByActifTrue() : null,
                SecurityUtils.has("CATALOG_VIEW") ? articles.countByActifTrue() : null,
                stockView ? lots.countByStatut(LotStatus.BLOQUE) : null,
                stockView ? stock.lowStock() : null,
                salesView ? sales.findTop6ByOrderByIdDesc().stream().map(SaleDto.Summary::from).toList() : null,
                week,
                purchaseView ? Money.of(purchases.totalBetween(today.withDayOfMonth(1), today)) : null,
                purchaseView || financeView ? Money.of(purchases.totalPayable()) : null,
                purchaseView ? purchases.countByStatutIn(List.of(PurchaseStatus.VALIDEE, PurchaseStatus.PARTIELLEMENT_RECUE)) : null,
                financeView ? Money.of(expenses.totalBetween(today.withDayOfMonth(1), today)) : null,
                cashView ? cashSessions.findOpen().isPresent() : null,
                cashView ? cashInDrawer() : null,
                pressingView ? pressings.countByStatutIn(List.of(com.huilerie.erp.pressing.Pressing.Status.RECU,
                        com.huilerie.erp.pressing.Pressing.Status.EN_COURS)) : null,
                pressingView ? Money.of(pressings.oilToReturn()) : null,
                pressingView ? Money.of(pressings.unpaid()) : null,
                productionView ? productions.countByStatutIn(List.of(com.huilerie.erp.production.ProductionOrder.Status.PLANIFIE,
                        com.huilerie.erp.production.ProductionOrder.Status.EN_COURS)) : null,
                productionView ? Money.ofAny(monthProd[2]) : null,
                productionView ? monthYield(monthProd) : null);
    }

    /** Espèces théoriques de la caisse ouverte (fond + entrées − sorties en espèces). */
    private BigDecimal cashInDrawer() {
        return cashSessions.findOpen().map(s -> {
            BigDecimal in = Money.of(cash.total(CashMovementType.ENCAISSEMENT, s.getOuverteLe(), Instant.now(),
                    com.huilerie.erp.finance.PaymentMode.ESPECES));
            BigDecimal out = Money.of(cash.total(CashMovementType.DECAISSEMENT, s.getOuverteLe(), Instant.now(),
                    com.huilerie.erp.finance.PaymentMode.ESPECES));
            return s.getFondInitial().add(in).subtract(out);
        }).orElse(null);
    }

    private static BigDecimal monthYield(Object[] r) {
        BigDecimal kg = Money.ofAny(r[1]);
        BigDecimal oil = Money.ofAny(r[2]);
        return kg.signum() == 0 ? null : oil.multiply(BigDecimal.valueOf(100)).divide(kg, 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal net(Instant from, Instant to) {
        BigDecimal in = Money.of(cash.total(CashMovementType.ENCAISSEMENT, from, to, null));
        BigDecimal out = Money.of(cash.total(CashMovementType.DECAISSEMENT, from, to, null));
        return in.subtract(out);
    }
}
