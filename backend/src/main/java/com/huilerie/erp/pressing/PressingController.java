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
    private final ClientAccountService accounts;

    public PressingController(PressingService service, ClientAccountService accounts) {
        this.service = service;
        this.accounts = accounts;
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

    @PutMapping("/{id}/transport")
    public PressingDto.Detail updateTransport(@PathVariable Long id, @Valid @RequestBody PressingDto.TransportRequest r) {
        return service.updateTransport(id, r);
    }

    @PutMapping("/{id}/price")
    public PressingDto.Detail updatePrice(@PathVariable Long id, @Valid @RequestBody PressingDto.PriceRequest r) {
        return service.updatePrice(id, r.prixKg());
    }

    @PutMapping("/{id}/date")
    public PressingDto.Detail updateDate(@PathVariable Long id, @Valid @RequestBody PressingDto.DateRequest r) {
        return service.updateDate(id, r.dateReception());
    }

    @PostMapping("/{id}/start")
    public PressingDto.Detail start(@PathVariable Long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/finish")
    public PressingDto.Detail finish(@PathVariable Long id, @Valid @RequestBody PressingDto.FinishRequest r) {
        return service.finish(id, r);
    }

    @PostMapping("/finish-group")
    public java.util.List<PressingDto.Detail> finishGroup(@Valid @RequestBody PressingDto.GroupFinishRequest r) {
        return service.finishGroup(r);
    }

    @PostMapping("/{id}/return")
    public PressingDto.Detail returnOil(@PathVariable Long id, @Valid @RequestBody PressingDto.ReturnRequest r) {
        return service.returnOil(id, r);
    }

    @GetMapping("/accounts")
    public java.util.List<PressingDto.AccountRow> accounts() {
        return accounts.accounts();
    }

    @GetMapping("/clients/{clientId}/oil")
    public PressingDto.OilAccount oilAccount(@PathVariable Long clientId) {
        return accounts.oilAccount(clientId);
    }

    @PostMapping("/clients/{clientId}/return")
    public PressingDto.OilAccount returnToClient(@PathVariable Long clientId,
                                                 @Valid @RequestBody PressingDto.ClientReturnRequest r) {
        return accounts.returnToClient(clientId, r);
    }

    @PostMapping("/clients/{clientId}/pay")
    public PressingDto.OilAccount payService(@PathVariable Long clientId, @Valid @RequestBody PressingDto.PaymentRequest r) {
        return accounts.payService(clientId, r);
    }

    @GetMapping("/clients/{clientId}/seller")
    public PressingDto.SellerAccount sellerAccount(@PathVariable Long clientId) {
        return accounts.sellerAccount(clientId);
    }

    @PostMapping("/clients/{clientId}/advances")
    public PressingDto.SellerAccount giveAdvance(@PathVariable Long clientId,
                                                 @Valid @RequestBody PressingDto.PaymentRequest r) {
        return accounts.giveAdvance(clientId, r);
    }

    @PostMapping("/{id}/pay")
    public PressingDto.Detail pay(@PathVariable Long id, @Valid @RequestBody PressingDto.PaymentRequest r) {
        return service.pay(id, r);
    }

    @PostMapping("/{id}/pay-seller")
    public PressingDto.Detail paySeller(@PathVariable Long id, @Valid @RequestBody PressingDto.PaymentRequest r) {
        return service.paySeller(id, r);
    }

    @PostMapping("/{id}/payments/{numero}/cancel")
    public PressingDto.Detail cancelPayment(@PathVariable Long id, @PathVariable String numero,
                                            @Valid @RequestBody PressingDto.CancelRequest r) {
        return service.cancelPayment(id, numero, r.motif());
    }

    @PostMapping("/{id}/cancel")
    public PressingDto.Detail cancel(@PathVariable Long id, @Valid @RequestBody PressingDto.CancelRequest r) {
        return service.cancel(id, r.motif());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
