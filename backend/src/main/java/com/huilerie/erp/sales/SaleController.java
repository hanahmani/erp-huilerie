package com.huilerie.erp.sales;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.sales.dto.SaleDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    public List<SaleDto.SellableArticle> catalog() {
        return service.sellableCatalog();
    }

    /** Renvoie 200 (et non 201) : un nouvel envoi avec la même clé renvoie la vente existante. */
    @PostMapping("/quick")
    public SaleDto.Detail quick(@Valid @RequestBody SaleDto.QuickSaleRequest r) {
        return service.createQuickSale(r);
    }

    @GetMapping
    public PageResponse<SaleDto.Summary> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) SaleStatus statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(from, to, numero, clientId, cashierId, statut, page, size);
    }

    @GetMapping("/cashiers")
    public List<SaleDto.Cashier> cashiers() {
        return service.cashiers();
    }

    @GetMapping("/receivables")
    public List<SaleDto.Receivable> receivables() {
        return service.receivables();
    }

    @GetMapping("/receivables/{clientId}")
    public List<SaleDto.Summary> openCredits(@PathVariable Long clientId) {
        return service.openCredits(clientId);
    }

    @PostMapping("/{id}/settle")
    public SaleDto.Detail settle(@PathVariable Long id, @Valid @RequestBody SaleDto.SettlementRequest r) {
        return service.settle(id, r);
    }

    @GetMapping("/{id}")
    public SaleDto.Detail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/print")
    public SaleDto.Detail print(@PathVariable Long id) {
        return service.recordPrint(id);
    }

    @PostMapping("/{id}/cancel")
    public SaleDto.Detail cancel(@PathVariable Long id, @Valid @RequestBody SaleDto.CancelRequest r) {
        return service.cancel(id, r.motif());
    }
}
