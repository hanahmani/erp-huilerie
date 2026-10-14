package com.huilerie.erp.settings;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.finance.ExpenseCategory;
import com.huilerie.erp.finance.ExpenseCategoryRepository;
import com.huilerie.erp.settings.dto.RefDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

/** Taxes et catégories de dépenses. */
@Service
public class ReferenceService {

    private final TaxRepository taxes;
    private final ExpenseCategoryRepository categories;
    private final AuditService audit;

    public ReferenceService(TaxRepository taxes, ExpenseCategoryRepository categories, AuditService audit) {
        this.taxes = taxes;
        this.categories = categories;
        this.audit = audit;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<RefDto.TaxResponse> listTaxes() {
        return taxes.findAllByOrderByTauxDesc().stream().map(ReferenceService::map).toList();
    }

    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @Transactional
    public RefDto.TaxResponse saveTax(Long id, RefDto.TaxRequest r) {
        String nom = r.nom().trim();
        if (id == null ? taxes.existsByNomIgnoreCase(nom) : taxes.existsByNomIgnoreCaseAndIdNot(nom, id)) {
            throw new BusinessException("Une taxe porte déjà ce nom.");
        }
        Tax t = id == null ? new Tax(nom, r.taux()) : taxes.findById(id).orElseThrow(() -> new NotFoundException("Taxe introuvable."));
        t.setNom(nom);
        t.setTaux(r.taux().setScale(2, RoundingMode.HALF_UP));
        t = taxes.save(t);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "Tax", String.valueOf(t.getId()), nom + " " + t.getTaux());
        return map(t);
    }

    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @Transactional
    public RefDto.TaxResponse setTaxStatus(Long id, boolean actif) {
        Tax t = taxes.findById(id).orElseThrow(() -> new NotFoundException("Taxe introuvable."));
        t.setActif(actif);
        return map(t);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<RefDto.ExpenseCategoryResponse> listExpenseCategories() {
        return categories.findAllByOrderByNomAsc().stream()
                .map(c -> new RefDto.ExpenseCategoryResponse(c.getId(), c.getNom(), c.isActif())).toList();
    }

    @PreAuthorize("hasAuthority('SETTINGS_MANAGE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public RefDto.ExpenseCategoryResponse saveExpenseCategory(Long id, RefDto.ExpenseCategoryRequest r) {
        String nom = r.nom().trim();
        if (id == null ? categories.existsByNomIgnoreCase(nom) : categories.existsByNomIgnoreCaseAndIdNot(nom, id)) {
            throw new BusinessException("Une catégorie de dépense porte déjà ce nom.");
        }
        ExpenseCategory c = id == null ? new ExpenseCategory(nom)
                : categories.findById(id).orElseThrow(() -> new NotFoundException("Catégorie introuvable."));
        c.setNom(nom);
        c = categories.save(c);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "ExpenseCategory", String.valueOf(c.getId()), nom);
        return new RefDto.ExpenseCategoryResponse(c.getId(), c.getNom(), c.isActif());
    }

    @PreAuthorize("hasAuthority('SETTINGS_MANAGE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public RefDto.ExpenseCategoryResponse setExpenseCategoryStatus(Long id, boolean actif) {
        ExpenseCategory c = categories.findById(id).orElseThrow(() -> new NotFoundException("Catégorie introuvable."));
        c.setActif(actif);
        return new RefDto.ExpenseCategoryResponse(c.getId(), c.getNom(), c.isActif());
    }

    private static RefDto.TaxResponse map(Tax t) {
        return new RefDto.TaxResponse(t.getId(), t.getNom(), t.getTaux(), t.isActif());
    }
}
