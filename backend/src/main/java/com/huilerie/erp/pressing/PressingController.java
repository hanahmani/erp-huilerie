package com.huilerie.erp.pressing;

import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.pressing.dto.PressingDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/pressings")
public class PressingController {

    private final PressingService service;

    public PressingController(PressingService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<PressingDto.Summary> search(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Pressing.Status statut,
                                                    @RequestParam(required = false) Long clientId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.search(q, statut, clientId, from, to, page, size);
    }

    @GetMapping("/{id}")
    public PressingDto.Detail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PressingDto.Detail receive(@Valid @RequestBody PressingDto.ReceiveRequest r) {
        return service.receive(r);
    }

    @PostMapping("/{id}/start")
    public PressingDto.Detail start(@PathVariable Long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/finish")
    public PressingDto.Detail finish(@PathVariable Long id, @Valid @RequestBody PressingDto.FinishRequest r) {
        return service.finish(id, r);
    }

    @PostMapping("/{id}/return")
    public PressingDto.Detail returnOil(@PathVariable Long id, @Valid @RequestBody PressingDto.ReturnRequest r) {
        return service.returnOil(id, r);
    }

    @PostMapping("/{id}/pay")
    public PressingDto.Detail pay(@PathVariable Long id, @Valid @RequestBody PressingDto.PaymentRequest r) {
        return service.pay(id, r);
    }

    @PostMapping("/{id}/cancel")
    public PressingDto.Detail cancel(@PathVariable Long id, @Valid @RequestBody PressingDto.CancelRequest r) {
        return service.cancel(id, r.motif());
    }
}
