package com.huilerie.erp.settings;

import com.huilerie.erp.catalog.dto.StatusRequest;
import com.huilerie.erp.settings.dto.RefDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class ReferenceController {

    private final ReferenceService service;

    public ReferenceController(ReferenceService service) {
        this.service = service;
    }

    @GetMapping("/taxes")
    public List<RefDto.TaxResponse> taxes() {
        return service.listTaxes();
    }

    @PostMapping("/taxes")
    @ResponseStatus(HttpStatus.CREATED)
    public RefDto.TaxResponse createTax(@Valid @RequestBody RefDto.TaxRequest r) {
        return service.saveTax(null, r);
    }

    @PutMapping("/taxes/{id}")
    public RefDto.TaxResponse updateTax(@PathVariable Long id, @Valid @RequestBody RefDto.TaxRequest r) {
        return service.saveTax(id, r);
    }

    @PatchMapping("/taxes/{id}/status")
    public RefDto.TaxResponse taxStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setTaxStatus(id, r.actif());
    }

    @GetMapping("/expense-categories")
    public List<RefDto.ExpenseCategoryResponse> expenseCategories() {
        return service.listExpenseCategories();
    }

    @PostMapping("/expense-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public RefDto.ExpenseCategoryResponse createCategory(@Valid @RequestBody RefDto.ExpenseCategoryRequest r) {
        return service.saveExpenseCategory(null, r);
    }

    @PutMapping("/expense-categories/{id}")
    public RefDto.ExpenseCategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody RefDto.ExpenseCategoryRequest r) {
        return service.saveExpenseCategory(id, r);
    }

    @PatchMapping("/expense-categories/{id}/status")
    public RefDto.ExpenseCategoryResponse categoryStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setExpenseCategoryStatus(id, r.actif());
    }
}
