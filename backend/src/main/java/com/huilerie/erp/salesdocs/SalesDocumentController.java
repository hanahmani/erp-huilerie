package com.huilerie.erp.salesdocs;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.salesdocs.dto.SalesDocDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales-documents")
public class SalesDocumentController {

    private final SalesDocumentService service;

    public SalesDocumentController(SalesDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<SalesDocDto.Summary> search(@RequestParam SalesDocument.Type type,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(required = false) SalesDocument.Status statut,
                                                    @RequestParam(required = false) Long clientId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(defaultValue = "false") boolean unpaid,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.search(type, q, statut, clientId, from, to, unpaid, page, size);
    }

    @GetMapping("/{id}")
    public SalesDocDto.Detail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesDocDto.Detail create(@Valid @RequestBody SalesDocDto.Request r) {
        return service.create(r);
    }

    @PutMapping("/{id}")
    public SalesDocDto.Detail update(@PathVariable Long id, @Valid @RequestBody SalesDocDto.Request r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/validate")
    public SalesDocDto.Detail validate(@PathVariable Long id) {
        return service.validate(id);
    }

    @PostMapping("/{id}/transform")
    public SalesDocDto.Detail transform(@PathVariable Long id, @Valid @RequestBody SalesDocDto.TransformRequest r) {
        return service.transform(id, r.cible());
    }

    @PostMapping("/{id}/pay")
    public SalesDocDto.Detail pay(@PathVariable Long id, @Valid @RequestBody SalesDocDto.PaymentRequest r) {
        return service.pay(id, r);
    }

    @PostMapping("/{id}/credit-note")
    public SalesDocDto.Detail creditNote(@PathVariable Long id, @Valid @RequestBody SalesDocDto.CreditNoteRequest r) {
        return service.creditNote(id, r);
    }

    @PostMapping("/{id}/cancel")
    public SalesDocDto.Detail cancel(@PathVariable Long id, @Valid @RequestBody SalesDocDto.CancelRequest r) {
        return service.cancel(id, r.motif());
    }
}
