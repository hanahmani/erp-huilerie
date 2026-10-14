package com.huilerie.erp.inventory;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.inventory.dto.InventoryDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock/inventories")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<InventoryDto.Summary> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public InventoryDto.Detail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDto.Detail create(@Valid @RequestBody InventoryDto.CreateRequest r) {
        return service.create(r);
    }

    @PutMapping("/{id}/counts")
    public InventoryDto.Detail count(@PathVariable Long id, @Valid @RequestBody InventoryDto.CountRequest r) {
        return service.count(id, r);
    }

    @PostMapping("/{id}/validate")
    public InventoryDto.Detail validate(@PathVariable Long id) {
        return service.validate(id);
    }

    @PostMapping("/{id}/cancel")
    public InventoryDto.Detail cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
