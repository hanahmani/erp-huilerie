package com.huilerie.erp.hr;

import com.huilerie.erp.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hr")
public class HrController {

    private final HrService service;

    public HrController(HrService service) {
        this.service = service;
    }

    @GetMapping("/services")
    public List<HrDto.Ref> services() { return service.listServices(); }

    @PostMapping("/services")
    public HrDto.Ref addService(@Valid @RequestBody HrDto.RefRequest r) { return service.saveRef(true, null, r.nom()); }

    @PutMapping("/services/{id}")
    public HrDto.Ref updateService(@PathVariable Long id, @Valid @RequestBody HrDto.RefRequest r) { return service.saveRef(true, id, r.nom()); }

    @GetMapping("/positions")
    public List<HrDto.Ref> positions() { return service.listPositions(); }

    @PostMapping("/positions")
    public HrDto.Ref addPosition(@Valid @RequestBody HrDto.RefRequest r) { return service.saveRef(false, null, r.nom()); }

    @PutMapping("/positions/{id}")
    public HrDto.Ref updatePosition(@PathVariable Long id, @Valid @RequestBody HrDto.RefRequest r) { return service.saveRef(false, id, r.nom()); }

    @GetMapping("/employees")
    public PageResponse<HrDto.EmployeeResponse> employees(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) Long serviceId,
                                                          @RequestParam(required = false) String statut,
                                                          @RequestParam(required = false) String contrat,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return service.searchEmployees(q, serviceId, statut, contrat, page, size);
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public HrDto.EmployeeResponse createEmployee(@Valid @RequestBody HrDto.EmployeeRequest r) { return service.saveEmployee(null, r); }

    @PutMapping("/employees/{id}")
    public HrDto.EmployeeResponse updateEmployee(@PathVariable Long id, @Valid @RequestBody HrDto.EmployeeRequest r) { return service.saveEmployee(id, r); }

    @GetMapping("/me")
    public ResponseEntity<HrDto.EmployeeResponse> me() {
        HrDto.EmployeeResponse e = service.me();
        return e == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(e);
    }

    @GetMapping("/leaves")
    public PageResponse<HrDto.Leave> leaves(@RequestParam(required = false) Long employeeId,
                                            @RequestParam(required = false) String statut,
                                            @RequestParam(defaultValue = "false") boolean mine,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.searchLeaves(employeeId, statut, mine, page, size);
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public HrDto.Leave request(@Valid @RequestBody HrDto.LeaveRequestBody r) { return service.requestLeave(r); }

    @PostMapping("/leaves/{id}/decision")
    public HrDto.Leave decide(@PathVariable Long id, @Valid @RequestBody HrDto.DecisionRequest r) { return service.decide(id, r); }

    @PostMapping("/leaves/{id}/cancel")
    public HrDto.Leave cancel(@PathVariable Long id) { return service.cancelLeave(id); }

    @GetMapping("/leaves/working-days")
    public java.math.BigDecimal workingDays(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.workingDays(from, to);
    }

    @GetMapping("/absences")
    public PageResponse<HrDto.AbsenceResponse> absences(@RequestParam(required = false) Long employeeId,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return service.searchAbsences(employeeId, from, to, page, size);
    }

    @PostMapping("/absences")
    @ResponseStatus(HttpStatus.CREATED)
    public HrDto.AbsenceResponse absence(@Valid @RequestBody HrDto.AbsenceRequest r) { return service.recordAbsence(r); }

    @GetMapping("/holidays")
    public List<HrDto.Holiday> holidays() { return service.listHolidays(); }

    @PostMapping("/holidays")
    public HrDto.Holiday addHoliday(@Valid @RequestBody HrDto.HolidayRequest r) { return service.addHoliday(r); }

    @DeleteMapping("/holidays/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHoliday(@PathVariable Long id) { service.deleteHoliday(id); }
}
