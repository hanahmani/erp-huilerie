package com.huilerie.erp.packaging;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.packaging.dto.PackagingDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/packaging")
public class PackagingController {

    private final PackagingService service;

    public PackagingController(PackagingService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PackagingDto.Detail> search(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) String statut,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.search(q, statut, from, to, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PackagingDto.Detail create(@Valid @RequestBody PackagingDto.Request r) {
        return service.create(r);
    }

    @PostMapping("/{id}/cancel")
    public PackagingDto.Detail cancel(@PathVariable Long id, @Valid @RequestBody PackagingDto.CancelRequest r) {
        return service.cancel(id, r.motif());
    }
}
