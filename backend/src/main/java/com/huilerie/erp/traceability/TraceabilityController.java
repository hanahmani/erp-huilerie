package com.huilerie.erp.traceability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traceability")
public class TraceabilityController {

    private final TraceabilityService service;

    public TraceabilityController(TraceabilityService service) {
        this.service = service;
    }

    @GetMapping("/lots/{id}")
    public TraceabilityService.Trace lot(@PathVariable Long id) {
        return service.trace(id);
    }
}
