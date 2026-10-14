package com.huilerie.erp.quality;

import com.huilerie.erp.catalog.dto.StatusRequest;
import com.huilerie.erp.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
public class QualityController {

    private final QualityService service;

    public QualityController(QualityService service) {
        this.service = service;
    }

    @GetMapping("/criteria")
    public List<QualityDto.Criterion> criteria() {
        return service.listCriteria();
    }

    @PostMapping("/criteria")
    @ResponseStatus(HttpStatus.CREATED)
    public QualityDto.Criterion createCriterion(@Valid @RequestBody QualityDto.CriterionRequest r) {
        return service.saveCriterion(null, r);
    }

    @PutMapping("/criteria/{id}")
    public QualityDto.Criterion updateCriterion(@PathVariable Long id, @Valid @RequestBody QualityDto.CriterionRequest r) {
        return service.saveCriterion(id, r);
    }

    @PatchMapping("/criteria/{id}/status")
    public QualityDto.Criterion criterionStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setCriterionStatus(id, r.actif());
    }

    @GetMapping("/controls")
    public PageResponse<QualityDto.Control> controls(@RequestParam(required = false) String q,
                                                     @RequestParam(required = false) String resultat,
                                                     @RequestParam(required = false) Long lotId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return service.searchControls(q, resultat, lotId, page, size);
    }

    @PostMapping("/controls")
    @ResponseStatus(HttpStatus.CREATED)
    public QualityDto.Control record(@Valid @RequestBody QualityDto.ControlRequest r) {
        return service.record(r);
    }

    @GetMapping("/anomalies")
    public PageResponse<QualityDto.Anomaly> anomalies(@RequestParam(required = false) String statut,
                                                      @RequestParam(required = false) String gravite,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return service.searchAnomalies(statut, gravite, page, size);
    }

    @PostMapping("/anomalies")
    @ResponseStatus(HttpStatus.CREATED)
    public QualityDto.Anomaly createAnomaly(@Valid @RequestBody QualityDto.AnomalyRequest r) {
        return service.createAnomaly(r);
    }

    @PutMapping("/anomalies/{id}")
    public QualityDto.Anomaly updateAnomaly(@PathVariable Long id, @Valid @RequestBody QualityDto.AnomalyUpdate r) {
        return service.updateAnomaly(id, r);
    }
}
