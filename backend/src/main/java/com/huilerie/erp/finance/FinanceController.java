package com.huilerie.erp.finance;

import com.huilerie.erp.finance.dto.CashDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/cash")
    public CashDto.Journal cash(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                @RequestParam(required = false) CashMovementType type,
                                @RequestParam(required = false) PaymentMode mode,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "30") int size) {
        return service.journal(from, to, type, mode, page, size);
    }
}
