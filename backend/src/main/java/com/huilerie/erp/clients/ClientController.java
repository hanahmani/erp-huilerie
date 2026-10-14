package com.huilerie.erp.clients;

import com.huilerie.erp.catalog.dto.StatusRequest;
import com.huilerie.erp.clients.dto.ClientDto;
import com.huilerie.erp.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ClientDto.Response> search(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) ClientType type,
                                                   @RequestParam(required = false) Boolean actif,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.search(q, type, actif, page, size);
    }

    @GetMapping("/{id}")
    public ClientDto.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDto.Response create(@Valid @RequestBody ClientDto.Request r) {
        return service.save(null, r);
    }

    @PutMapping("/{id}")
    public ClientDto.Response update(@PathVariable Long id, @Valid @RequestBody ClientDto.Request r) {
        return service.save(id, r);
    }

    @PatchMapping("/{id}/status")
    public ClientDto.Response status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setStatus(id, r.actif());
    }
}
