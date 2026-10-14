package com.huilerie.erp.quality;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.inventory.LotStatus;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.inventory.StockLotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contrôle qualité : chaque résultat est comparé aux seuils du critère.
 * Avec « appliquer la décision » : un lot non conforme est bloqué et une anomalie est ouverte ;
 * un lot conforme bloqué est débloqué.
 */
@Service
public class QualityService {

    private final QualityCriterionRepository criteria;
    private final QualityControlRepository controls;
    private final QualityAnomalyRepository anomalies;
    private final StockLotRepository lots;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public QualityService(QualityCriterionRepository criteria, QualityControlRepository controls,
                          QualityAnomalyRepository anomalies, StockLotRepository lots, DocumentNumberService numbers,
                          AuditService audit) {
        this.criteria = criteria;
        this.controls = controls;
        this.anomalies = anomalies;
        this.lots = lots;
        this.numbers = numbers;
        this.audit = audit;
    }

    // ---- critères
    @PreAuthorize("hasAuthority('QUALITY_VIEW')")
    @Transactional(readOnly = true)
    public List<QualityDto.Criterion> listCriteria() {
        return criteria.findAllByOrderByNomAsc().stream().map(QualityDto.Criterion::from).toList();
    }

    @PreAuthorize("hasAuthority('QUALITY_MANAGE')")
    @Transactional
    public QualityDto.Criterion saveCriterion(Long id, QualityDto.CriterionRequest r) {
        String nom = r.nom().trim();
        if (id == null ? criteria.existsByNomIgnoreCase(nom) : criteria.existsByNomIgnoreCaseAndIdNot(nom, id)) {
            throw new BusinessException("Un critère porte déjà ce nom.");
        }
        if (r.valeurMin() == null && r.valeurMax() == null) {
            throw new BusinessException("Indiquez au moins une valeur minimale ou maximale.");
        }
        if (r.valeurMin() != null && r.valeurMax() != null && r.valeurMin().compareTo(r.valeurMax()) > 0) {
            throw new BusinessException("Le minimum doit être inférieur au maximum.");
        }
        QualityCriterion c = id == null ? new QualityCriterion(nom) : criteria.findById(id).orElseThrow(() -> new NotFoundException("Critère introuvable."));
        c.setNom(nom);
        c.setUnite(Text.clean(r.unite()));
        c.setValeurMin(r.valeurMin());
        c.setValeurMax(r.valeurMax());
        c = criteria.save(c);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "QualityCriterion", String.valueOf(c.getId()), nom);
        return QualityDto.Criterion.from(c);
    }

    @PreAuthorize("hasAuthority('QUALITY_MANAGE')")
    @Transactional
    public QualityDto.Criterion setCriterionStatus(Long id, boolean actif) {
        QualityCriterion c = criteria.findById(id).orElseThrow(() -> new NotFoundException("Critère introuvable."));
        c.setActif(actif);
        return QualityDto.Criterion.from(c);
    }

    // ---- contrôles
    @PreAuthorize("hasAuthority('QUALITY_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<QualityDto.Control> searchControls(String q, String resultat, Long lotId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(controls.search(Text.search(q), Text.clean(resultat), lotId, pr), QualityDto.Control::from);
    }

    @PreAuthorize("hasAuthority('QUALITY_MANAGE')")
    @Transactional
    public QualityDto.Control record(QualityDto.ControlRequest r) {
        StockLot lot = lots.findById(r.lotId()).orElseThrow(() -> new NotFoundException("Lot introuvable."));
        QualityControl c = new QualityControl(numbers.nextYearly("CONTROLE", "CQ"), lot, SecurityUtils.username(),
                Text.clean(r.observations()));
        Set<Long> seen = new HashSet<>();
        for (QualityDto.ResultRequest res : r.resultats()) {
            if (!seen.add(res.criterionId())) {
                throw new BusinessException("Un critère est saisi deux fois.");
            }
            QualityCriterion crit = criteria.findById(res.criterionId()).orElseThrow(() -> new NotFoundException("Critère introuvable."));
            if (!crit.isActif()) {
                throw new BusinessException("Le critère « " + crit.getNom() + " » est désactivé.");
            }
            c.addResult(crit, res.valeur());
        }
        c = controls.save(c);
        if (r.appliquerDecision()) {
            if (c.isConforme()) {
                if (lot.getStatut() == LotStatus.BLOQUE) {
                    lot.unblock();
                }
            } else {
                lot.block("Non conforme au contrôle " + c.getNumero());
                anomalies.save(new QualityAnomaly(numbers.nextYearly("ANOMALIE", "ANO"), lot, c,
                        "Contrôle " + c.getNumero() + " non conforme", "MAJEURE", SecurityUtils.username()));
            }
        }
        audit.log(SecurityUtils.username(), "QUALITY_CONTROL", "StockLot", String.valueOf(lot.getId()),
                c.getNumero() + " : " + c.getResultat());
        return QualityDto.Control.from(c);
    }

    // ---- anomalies
    @PreAuthorize("hasAuthority('QUALITY_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<QualityDto.Anomaly> searchAnomalies(String statut, String gravite, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(anomalies.search(Text.clean(statut), Text.clean(gravite), pr), QualityDto.Anomaly::from);
    }

    @PreAuthorize("hasAuthority('QUALITY_MANAGE')")
    @Transactional
    public QualityDto.Anomaly createAnomaly(QualityDto.AnomalyRequest r) {
        StockLot lot = r.lotId() == null ? null : lots.findById(r.lotId()).orElseThrow(() -> new NotFoundException("Lot introuvable."));
        QualityAnomaly a = anomalies.save(new QualityAnomaly(numbers.nextYearly("ANOMALIE", "ANO"), lot, null,
                r.description().trim(), r.gravite(), SecurityUtils.username()));
        audit.log(SecurityUtils.username(), "ANOMALY_CREATE", "QualityAnomaly", String.valueOf(a.getId()), a.getNumero());
        return QualityDto.Anomaly.from(a);
    }

    @PreAuthorize("hasAuthority('QUALITY_MANAGE')")
    @Transactional
    public QualityDto.Anomaly updateAnomaly(Long id, QualityDto.AnomalyUpdate r) {
        QualityAnomaly a = anomalies.findById(id).orElseThrow(() -> new NotFoundException("Anomalie introuvable."));
        if ("CLOTUREE".equals(r.statut()) && Text.clean(r.actionCorrective()) == null) {
            throw new BusinessException("Décrivez l'action corrective avant de clôturer.");
        }
        a.update(Text.clean(r.actionCorrective()), r.statut());
        audit.log(SecurityUtils.username(), "ANOMALY_UPDATE", "QualityAnomaly", String.valueOf(id), r.statut());
        return QualityDto.Anomaly.from(a);
    }
}
