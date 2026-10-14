package com.huilerie.erp.production;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.production.dto.ProductionDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/production")
public class ProductionController {

    private final ProductionService service;

    public ProductionController(ProductionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ProductionDto.Summary> search(@RequestParam(required = false) String q,
                                                      @RequestParam(required = false) ProductionOrder.Status statut,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return service.search(q, statut, from, to, page, size);
    }

    @GetMapping("/{id}")
    public ProductionDto.Detail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductionDto.Detail create(@Valid @RequestBody ProductionDto.PlanRequest r) {
        return service.plan(null, r);
    }

    @PutMapping("/{id}")
    public ProductionDto.Detail update(@PathVariable Long id, @Valid @RequestBody ProductionDto.PlanRequest r) {
        return service.plan(id, r);
    }

    @PostMapping("/{id}/start")
    public ProductionDto.Detail start(@PathVariable Long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/finish")
    public ProductionDto.Detail finish(@PathVariable Long id, @Valid @RequestBody ProductionDto.FinishRequest r) {
        return service.finish(id, r);
    }

    @PostMapping("/{id}/cancel")
    public ProductionDto.Detail cancel(@PathVariable Long id, @Valid @RequestBody ProductionDto.CancelRequest r) {
        return service.cancel(id, r.motif());
    }
}
