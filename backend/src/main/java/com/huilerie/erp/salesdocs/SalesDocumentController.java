package com.huilerie.erp.salesdocs;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.salesdocs.dto.SalesDocDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/sales-documents")
public class SalesDocumentController {

    private final SalesDocumentService service;
    private final SalesDocumentPdfService pdf;

    public SalesDocumentController(SalesDocumentService service, SalesDocumentPdfService pdf) {
        this.service = service;
        this.pdf = pdf;
    }

    /** PDF A4 ; inline pour l'aperçu, le nom de fichier sert au téléchargement. */
    // Pas de « produces » : une erreur (paramètre manquant…) doit pouvoir repartir en JSON.
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        SalesDocumentPdfService.Pdf p = pdf.generate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(p.fileName()).build().toString())
                .body(p.content());
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
