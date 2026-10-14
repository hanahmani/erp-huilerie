package com.huilerie.erp.finance;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.finance.dto.CashDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/finance")
public class TreasuryController {

    private final TreasuryService service;

    public TreasuryController(TreasuryService service) {
        this.service = service;
    }

    @GetMapping("/expenses")
    public PageResponse<CashDto.ExpenseLine> expenses(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) Long categoryId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(required = false) String statut,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.searchExpenses(q, categoryId, from, to, statut, page, size);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public CashDto.ExpenseLine createExpense(@Valid @RequestBody CashDto.ExpenseRequest r) {
        return service.createExpense(r);
    }

    @PostMapping("/expenses/{id}/cancel")
    public CashDto.ExpenseLine cancelExpense(@PathVariable Long id, @Valid @RequestBody CashDto.CancelRequest r) {
        return service.cancelExpense(id, r.motif());
    }

    /** 204 quand aucune caisse n'est ouverte. */
    @GetMapping("/session")
    public ResponseEntity<CashDto.Session> current() {
        CashDto.Session s = service.current();
        return s == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(s);
    }

    @GetMapping("/sessions")
    public PageResponse<CashDto.Session> sessions(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.history(page, size);
    }

    @PostMapping("/session/open")
    public CashDto.Session open(@Valid @RequestBody CashDto.OpenSessionRequest r) {
        return service.open(r.fondInitial());
    }

    @PostMapping("/session/close")
    public CashDto.Session close(@Valid @RequestBody CashDto.CloseSessionRequest r) {
        return service.close(r.especesComptees(), r.justification());
    }
}
