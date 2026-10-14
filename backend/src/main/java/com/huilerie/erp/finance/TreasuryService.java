package com.huilerie.erp.finance;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.finance.dto.CashDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Dépenses et sessions de caisse. */
@Service
public class TreasuryService {

    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final CashSessionRepository sessions;
    private final CashMovementRepository cash;
    private final FinanceService finance;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public TreasuryService(ExpenseRepository expenses, ExpenseCategoryRepository categories, CashSessionRepository sessions,
                           CashMovementRepository cash, FinanceService finance, DocumentNumberService numbers,
                           AuditService audit) {
        this.expenses = expenses;
        this.categories = categories;
        this.sessions = sessions;
        this.cash = cash;
        this.finance = finance;
        this.numbers = numbers;
        this.audit = audit;
    }

    // ----------------------------------------------------------------- dépenses

    @PreAuthorize("hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<CashDto.ExpenseLine> searchExpenses(String q, Long categoryId, LocalDate from, LocalDate to,
                                                           String statut, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(expenses.search(Text.search(q), categoryId, from, to, Text.clean(statut), pr), CashDto.ExpenseLine::from);
    }

    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public CashDto.ExpenseLine createExpense(CashDto.ExpenseRequest r) {
        ExpenseCategory c = categories.findById(r.categorieId()).orElseThrow(() -> new NotFoundException("Catégorie introuvable."));
        if (!c.isActif()) {
            throw new BusinessException("Cette catégorie est désactivée.");
        }
        if (r.dateDepense().isAfter(LocalDate.now().plusDays(1))) {
            throw new BusinessException("La date de la dépense ne peut pas être dans le futur.");
        }
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        Expense e = expenses.save(new Expense(numbers.nextYearly("DEPENSE", "DEP"), c, r.libelle().trim(), amount, r.mode(),
                Text.clean(r.reference()), r.dateDepense(), SecurityUtils.username()));
        finance.record(CashMovementType.DECAISSEMENT, r.mode(), amount, "Dépense " + e.getNumero() + " : " + e.getLibelle(),
                "DEPENSE", e.getId());
        audit.log(SecurityUtils.username(), "EXPENSE_CREATE", "Expense", String.valueOf(e.getId()),
                e.getNumero() + " " + amount.toPlainString() + " DT");
        return CashDto.ExpenseLine.from(e);
    }

    /** Annulation : contre-passation par un encaissement de même montant, jamais une suppression. */
    @PreAuthorize("hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public CashDto.ExpenseLine cancelExpense(Long id, String motif) {
        Expense e = expenses.findById(id).orElseThrow(() -> new NotFoundException("Dépense introuvable."));
        if (Expense.ANNULEE.equals(e.getStatut())) {
            throw new BusinessException("Cette dépense est déjà annulée.");
        }
        e.cancel(motif.trim());
        finance.record(CashMovementType.ENCAISSEMENT, e.getMode(), e.getMontant(), "Annulation dépense " + e.getNumero(),
                "DEPENSE", e.getId());
        audit.log(SecurityUtils.username(), "EXPENSE_CANCEL", "Expense", String.valueOf(id), motif.trim());
        return CashDto.ExpenseLine.from(e);
    }

    // ----------------------------------------------------------------- sessions de caisse

    @PreAuthorize("hasAuthority('CASH_SESSION') or hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public CashDto.Session current() {
        return sessions.findOpen().map(this::toDto).orElse(null);
    }

    @PreAuthorize("hasAuthority('CASH_SESSION') or hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<CashDto.Session> history(int page, int size) {
        return PageResponse.of(sessions.findAllByOrderByIdDesc(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))),
                this::toDto);
    }

    @PreAuthorize("hasAuthority('CASH_SESSION')")
    @Transactional
    public CashDto.Session open(BigDecimal fond) {
        if (sessions.findOpen().isPresent()) {
            throw new BusinessException("Une caisse est déjà ouverte. Fermez-la avant d'en ouvrir une autre.");
        }
        CashSession s = sessions.saveAndFlush(new CashSession(SecurityUtils.username(), Money.strict(fond, "Fond de caisse")));
        audit.log(SecurityUtils.username(), "CASH_OPEN", "CashSession", String.valueOf(s.getId()),
                "Fond " + s.getFondInitial().toPlainString() + " DT");
        return toDto(s);
    }

    @PreAuthorize("hasAuthority('CASH_SESSION')")
    @Transactional
    public CashDto.Session close(BigDecimal counted, String justification) {
        CashSession s = sessions.lockOpen().orElseThrow(() -> new BusinessException("Aucune caisse ouverte."));
        CashDto.Session live = toDto(s);
        BigDecimal comptees = Money.strict(counted, "Espèces comptées");
        BigDecimal ecart = comptees.subtract(live.especesTheoriques());
        String why = Text.clean(justification);
        if (ecart.signum() != 0 && why == null) {
            throw new BusinessException("Écart de " + ecart.toPlainString() + " DT : la justification est obligatoire.");
        }
        s.close(SecurityUtils.username(), live.especesTheoriques(), comptees, why);
        audit.log(SecurityUtils.username(), "CASH_CLOSE", "CashSession", String.valueOf(s.getId()),
                "Écart " + ecart.toPlainString() + " DT" + (why == null ? "" : " : " + why));
        return toDto(s);
    }

    private CashDto.Session toDto(CashSession s) {
        Instant end = s.isOpen() ? Instant.now() : s.getFermeeLe();
        BigDecimal in = Money.of(cash.total(CashMovementType.ENCAISSEMENT, s.getOuverteLe(), end, PaymentMode.ESPECES));
        BigDecimal out = Money.of(cash.total(CashMovementType.DECAISSEMENT, s.getOuverteLe(), end, PaymentMode.ESPECES));
        BigDecimal theo = s.isOpen() ? s.getFondInitial().add(in).subtract(out) : s.getEspecesTheoriques();
        return new CashDto.Session(s.getId(), s.isOpen(), s.getOuvertePar(), s.getOuverteLe(), s.getFondInitial(), in, out,
                theo, s.getFermeePar(), s.getFermeeLe(), s.getEspecesComptees(), s.getEcart(), s.getJustification());
    }
}
