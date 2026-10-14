package com.huilerie.erp.suppliers;

import com.huilerie.erp.catalog.dto.StatusRequest;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.suppliers.dto.SupplierDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<SupplierDto.Response> search(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) SupplierType type,
                                                     @RequestParam(required = false) Boolean actif,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.search(q, type, actif, page, size);
    }

    @GetMapping("/{id}")
    public SupplierDto.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto.Response create(@Valid @RequestBody SupplierDto.Request r) {
        return service.save(null, r);
    }

    @PutMapping("/{id}")
    public SupplierDto.Response update(@PathVariable Long id, @Valid @RequestBody SupplierDto.Request r) {
        return service.save(id, r);
    }

    @PatchMapping("/{id}/status")
    public SupplierDto.Response status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setStatus(id, r.actif());
    }
}
