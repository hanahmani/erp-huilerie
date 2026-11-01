package com.huilerie.erp.hr;

import com.huilerie.erp.catalog.dto.StatusRequest;
import com.huilerie.erp.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final HrService service;

    public HrController(HrService service) {
        this.service = service;
    }

    // ---- employés
    @GetMapping("/employees")
    public PageResponse<HrDto.EmployeeResponse> employees(@RequestParam(required = false) String q,
                                                         @RequestParam(required = false) Boolean actif,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "50") int size) {
        return service.searchEmployees(q, actif, page, size);
    }

    @GetMapping("/employees/{id}")
    public HrDto.EmployeeResponse employee(@PathVariable Long id) { return service.getEmployee(id); }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public HrDto.EmployeeResponse createEmployee(@Valid @RequestBody HrDto.EmployeeRequest r) { return service.saveEmployee(null, r); }

    @PutMapping("/employees/{id}")
    public HrDto.EmployeeResponse updateEmployee(@PathVariable Long id, @Valid @RequestBody HrDto.EmployeeRequest r) {
        return service.saveEmployee(id, r);
    }

    @PatchMapping("/employees/{id}/status")
    public HrDto.EmployeeResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setStatus(id, r.actif());
    }

    // ---- pointage
    @PostMapping("/employees/{id}/start")
    public HrDto.EmployeeResponse start(@PathVariable Long id) { return service.start(id); }

    @PostMapping("/employees/{id}/stop")
    public HrDto.EmployeeResponse stop(@PathVariable Long id) { return service.stop(id); }

    @GetMapping("/work-days")
    public PageResponse<HrDto.WorkDayLine> workDays(@RequestParam(required = false) Long employeeId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "30") int size) {
        return service.searchWorkDays(employeeId, from, to, page, size);
    }

    @PostMapping("/work-days")
    @ResponseStatus(HttpStatus.CREATED)
    public HrDto.WorkDayLine addWorkDay(@Valid @RequestBody HrDto.WorkDayRequest r) { return service.addWorkDay(r); }

    @PutMapping("/work-days/{id}")
    public HrDto.WorkDayLine updateWorkDay(@PathVariable Long id, @Valid @RequestBody HrDto.WorkDayRequest r) {
        return service.updateWorkDay(id, r);
    }

    @DeleteMapping("/work-days/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkDay(@PathVariable Long id) { service.deleteWorkDay(id); }

    // ---- paiements
    @PostMapping("/employees/{id}/payments")
    public HrDto.EmployeeResponse pay(@PathVariable Long id, @Valid @RequestBody HrDto.PaymentRequest r) {
        return service.pay(id, r);
    }

    @GetMapping("/payments")
    public PageResponse<HrDto.PaymentLine> payments(@RequestParam(required = false) Long employeeId,
                                                   @RequestParam(required = false) EmployeePayment.Type type,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "30") int size) {
        return service.searchPayments(employeeId, type, from, to, page, size);
    }
}
