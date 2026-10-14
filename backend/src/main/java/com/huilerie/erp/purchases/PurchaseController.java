package com.huilerie.erp.purchases;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.purchases.dto.PurchaseDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PurchaseDto.Summary> search(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Long supplierId,
                                                    @RequestParam(required = false) PurchaseStatus statut,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(defaultValue = "false") boolean unpaid,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.search(q, supplierId, statut, from, to, unpaid, page, size);
    }

    @GetMapping("/payables")
    public List<PurchaseDto.Payable> payables() {
        return service.payables();
    }

    @GetMapping("/{id}")
    public PurchaseDto.Detail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseDto.Detail create(@Valid @RequestBody PurchaseDto.Request r) {
        return service.create(r);
    }

    @PutMapping("/{id}")
    public PurchaseDto.Detail update(@PathVariable Long id, @Valid @RequestBody PurchaseDto.Request r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/validate")
    public PurchaseDto.Detail validate(@PathVariable Long id) {
        return service.validate(id);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseDto.Detail cancel(@PathVariable Long id, @Valid @RequestBody PurchaseDto.CancelRequest r) {
        return service.cancel(id, r.motif());
    }

    @PatchMapping("/{id}/invoice-ref")
    public PurchaseDto.Detail invoiceRef(@PathVariable Long id, @Valid @RequestBody PurchaseDto.InvoiceRefRequest r) {
        return service.setInvoiceRef(id, r.refFacture());
    }

    @PostMapping("/{id}/receive")
    public PurchaseDto.Detail receive(@PathVariable Long id, @Valid @RequestBody PurchaseDto.ReceiveRequest r) {
        return service.receive(id, r);
    }

    @PostMapping("/{id}/pay")
    public PurchaseDto.Detail pay(@PathVariable Long id, @Valid @RequestBody PurchaseDto.PaymentRequest r) {
        return service.pay(id, r);
    }
}
