package com.huilerie.erp.inventory;

import com.huilerie.erp.catalog.ArticleType;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.inventory.dto.StockDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping("/state")
    public PageResponse<StockDto.ArticleStock> state(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) ArticleType type,
                                                     @RequestParam(defaultValue = "false") boolean alertOnly,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.stockState(q, type, alertOnly, page, size);
    }

    @GetMapping("/movements")
    public PageResponse<StockDto.MovementResponse> movements(
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return service.searchMovements(articleId, lotId, type, from, to, page, size);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public StockDto.MovementResponse create(@Valid @RequestBody StockDto.MovementRequest r) {
        return service.manualMovement(r);
    }

    @GetMapping("/lots")
    public PageResponse<StockDto.LotResponse> lots(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) Long articleId,
                                                   @RequestParam(required = false) LotOwner owner,
                                                   @RequestParam(required = false) LotStatus statut,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return service.searchLots(q, articleId, owner, statut, page, size);
    }

    @PostMapping("/lots")
    @ResponseStatus(HttpStatus.CREATED)
    public StockDto.LotResponse createLot(@Valid @RequestBody StockDto.LotRequest r) {
        return service.createLot(r);
    }

    @PatchMapping("/lots/{id}/status")
    public StockDto.LotResponse lotStatus(@PathVariable Long id, @Valid @RequestBody StockDto.LotStatusRequest r) {
        return service.setLotStatus(id, r);
    }
}
