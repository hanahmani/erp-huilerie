package com.huilerie.erp.inventory;

import com.huilerie.erp.catalog.dto.StatusRequest;
import com.huilerie.erp.inventory.dto.TankDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock/tanks")
public class TankController {

    private final TankService service;

    public TankController(TankService service) {
        this.service = service;
    }

    @GetMapping
    public List<TankDto.Response> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TankDto.Response create(@Valid @RequestBody TankDto.Request r) {
        return service.create(r);
    }

    @PutMapping("/{id}")
    public TankDto.Response update(@PathVariable Long id, @Valid @RequestBody TankDto.Request r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/transfer")
    public List<TankDto.Response> transfer(@PathVariable Long id, @Valid @RequestBody TankDto.TransferRequest r) {
        return service.transfer(id, r.versId(), r.quantite());
    }

    @PatchMapping("/{id}/status")
    public TankDto.Response status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setStatus(id, r.actif());
    }
}
