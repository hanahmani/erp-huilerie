package com.huilerie.erp.hr;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.finance.CashMovementType;
import com.huilerie.erp.finance.FinanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ressources humaines : employés, pointage Play / Stop et paiements.
 * Une journée pointée vaut le TJM de l'employé au moment du Play ;
 * reste dû = somme des TJM des journées − somme des paiements (salaire, journées, avances).
 */
@Service
public class HrService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");
    /** Tolérance pour une heure saisie à la main légèrement en avance sur l'horloge du serveur. */
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    private final EmployeeRepository employees;
    private final WorkDayRepository workDays;
    private final EmployeePaymentRepository payments;
    private final FinanceService finance;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public HrService(EmployeeRepository employees, WorkDayRepository workDays, EmployeePaymentRepository payments,
                     FinanceService finance, DocumentNumberService numbers, AuditService audit) {
        this.employees = employees;
        this.workDays = workDays;
        this.payments = payments;
        this.finance = finance;
        this.numbers = numbers;
        this.audit = audit;
    }

    private static PageRequest page(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
    }

    private static LocalDate today() {
        return LocalDate.now(TUNIS);
    }

    // ---------------------------------------------------------------- employés

    @PreAuthorize("hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<HrDto.EmployeeResponse> searchEmployees(String q, Boolean actif, int p, int size) {
        Page<Employee> found = employees.search(Text.search(q), actif, page(p, size, Sort.by("nom", "prenom")));
        List<HrDto.EmployeeResponse> rows = summaries(found.getContent());
        return new PageResponse<>(rows, found.getNumber(), found.getSize(), found.getTotalElements(), found.getTotalPages());
    }

    @PreAuthorize("hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public HrDto.EmployeeResponse getEmployee(Long id) {
        return summaries(List.of(employee(id))).get(0);
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.EmployeeResponse saveEmployee(Long id, HrDto.EmployeeRequest r) {
        Employee e = id == null ? new Employee(numbers.nextCode("EMPLOYE", "EMP")) : employee(id);
        e.update(r.nom().trim(), r.prenom().trim(), Text.clean(r.cin()), Text.clean(r.telephone()), Text.clean(r.adresse()),
                Money.strict(r.tjm(), "TJM"));
        e = employees.save(e);
        audit.log(SecurityUtils.username(), id == null ? "EMPLOYEE_CREATE" : "EMPLOYEE_UPDATE", "Employee",
                String.valueOf(e.getId()), e.getMatricule() + " TJM " + e.getTjm().toPlainString());
        return summaries(List.of(e)).get(0);
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.EmployeeResponse setStatus(Long id, boolean actif) {
        Employee e = employee(id);
        if (!actif && workDays.findFirstByEmployeeIdAndHeureFinIsNullOrderByJourDesc(id).isPresent()) {
            throw new BusinessException("Arrêtez d'abord le pointage en cours de " + e.fullName() + ".");
        }
        e.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "EMPLOYEE_ACTIVATE" : "EMPLOYEE_DEACTIVATE", "Employee",
                String.valueOf(id), e.getMatricule());
        return summaries(List.of(e)).get(0);
    }

    // ---------------------------------------------------------------- pointage Play / Stop

    /** Play : la journée du jour commence à l'heure actuelle, au TJM actuel de l'employé. */
    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.EmployeeResponse start(Long employeeId) {
        Employee e = employees.lockById(employeeId).orElseThrow(() -> new NotFoundException("Employé introuvable."));
        if (!e.isActif()) {
            throw new BusinessException(e.fullName() + " est inactif.");
        }
        workDays.findFirstByEmployeeIdAndHeureFinIsNullOrderByJourDesc(employeeId).ifPresent(open -> {
            throw new BusinessException("Le pointage du " + open.getJour() + " n'est pas arrêté : arrêtez-le ou corrigez-le d'abord.");
        });
        LocalDate day = today();
        if (workDays.existsByEmployeeIdAndJour(employeeId, day)) {
            throw new BusinessException(e.fullName() + " est déjà pointé aujourd'hui : corrigez les heures si besoin.");
        }
        workDays.save(new WorkDay(e, day, Instant.now(), null, SecurityUtils.username()));
        audit.log(SecurityUtils.username(), "WORK_START", "Employee", String.valueOf(employeeId), e.getMatricule() + " " + day);
        return summaries(List.of(e)).get(0);
    }

    /** Stop : la journée en cours s'arrête à l'heure actuelle. */
    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.EmployeeResponse stop(Long employeeId) {
        Employee e = employees.lockById(employeeId).orElseThrow(() -> new NotFoundException("Employé introuvable."));
        WorkDay w = workDays.findFirstByEmployeeIdAndHeureFinIsNullOrderByJourDesc(employeeId)
                .orElseThrow(() -> new BusinessException(e.fullName() + " n'a pas de pointage en cours."));
        Instant now = Instant.now();
        w.stop(now.isBefore(w.getHeureDebut()) ? w.getHeureDebut() : now);
        audit.log(SecurityUtils.username(), "WORK_STOP", "Employee", String.valueOf(employeeId), e.getMatricule() + " " + w.getJour());
        return summaries(List.of(e)).get(0);
    }

    @PreAuthorize("hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<HrDto.WorkDayLine> searchWorkDays(Long employeeId, LocalDate from, LocalDate to, int p, int size) {
        return PageResponse.of(workDays.search(employeeId, from, to,
                page(p, size, Sort.by(Sort.Direction.DESC, "jour").and(Sort.by("id")))), HrDto.WorkDayLine::from);
    }

    /** Journée oubliée (pas de Play ce jour-là) : saisie manuelle au TJM actuel. */
    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.WorkDayLine addWorkDay(HrDto.WorkDayRequest r) {
        if (r.employeeId() == null) {
            throw new BusinessException("Choisissez l'employé.");
        }
        Employee e = employees.lockById(r.employeeId()).orElseThrow(() -> new NotFoundException("Employé introuvable."));
        LocalDate day = checkTimes(r.heureDebut(), r.heureFin());
        if (workDays.existsByEmployeeIdAndJour(e.getId(), day)) {
            throw new BusinessException(e.fullName() + " a déjà une journée le " + day + " : corrigez-la au lieu d'en ajouter une.");
        }
        if (r.heureFin() == null && workDays.findFirstByEmployeeIdAndHeureFinIsNullOrderByJourDesc(e.getId()).isPresent()) {
            throw new BusinessException("Un pointage est déjà en cours pour " + e.fullName() + " : indiquez l'heure de fin.");
        }
        WorkDay w = workDays.save(new WorkDay(e, day, r.heureDebut(), r.heureFin(), SecurityUtils.username()));
        audit.log(SecurityUtils.username(), "WORK_DAY_CREATE", "Employee", String.valueOf(e.getId()), e.getMatricule() + " " + day);
        return HrDto.WorkDayLine.from(w);
    }

    /** Correction manuelle des heures (le TJM de la journée ne change pas). */
    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.WorkDayLine updateWorkDay(Long id, HrDto.WorkDayRequest r) {
        WorkDay w = workDays.findById(id).orElseThrow(() -> new NotFoundException("Journée introuvable."));
        LocalDate day = checkTimes(r.heureDebut(), r.heureFin());
        if (workDays.existsByEmployeeIdAndJourAndIdNot(w.getEmployee().getId(), day, id)) {
            throw new BusinessException("Une autre journée existe déjà le " + day + " pour cet employé.");
        }
        if (r.heureFin() == null) {
            workDays.findFirstByEmployeeIdAndHeureFinIsNullOrderByJourDesc(w.getEmployee().getId())
                    .filter(open -> !open.getId().equals(id))
                    .ifPresent(open -> {
                        throw new BusinessException("Un autre pointage est en cours : indiquez l'heure de fin.");
                    });
        }
        w.correct(day, r.heureDebut(), r.heureFin(), SecurityUtils.username());
        audit.log(SecurityUtils.username(), "WORK_DAY_UPDATE", "Employee", String.valueOf(w.getEmployee().getId()),
                w.getEmployee().getMatricule() + " " + day + " " + r.heureDebut() + " → " + r.heureFin());
        return HrDto.WorkDayLine.from(w);
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public void deleteWorkDay(Long id) {
        WorkDay w = workDays.findById(id).orElseThrow(() -> new NotFoundException("Journée introuvable."));
        workDays.delete(w);
        audit.log(SecurityUtils.username(), "WORK_DAY_DELETE", "Employee", String.valueOf(w.getEmployee().getId()),
                w.getEmployee().getMatricule() + " " + w.getJour());
    }

    /** Fin après début, pas dans le futur ; renvoie le jour (fuseau de Tunis) de l'heure de début. */
    private static LocalDate checkTimes(Instant debut, Instant fin) {
        Instant limit = Instant.now().plus(FUTURE_TOLERANCE);
        if (debut.isAfter(limit) || (fin != null && fin.isAfter(limit))) {
            throw new BusinessException("Les heures ne peuvent pas être dans le futur.");
        }
        if (fin != null && fin.isBefore(debut)) {
            throw new BusinessException("L'heure de fin doit être après l'heure de début.");
        }
        return debut.atZone(TUNIS).toLocalDate();
    }

    // ---------------------------------------------------------------- paiements

    /** Salaire, journées ou avance : sortie de caisse ; le reste dû peut devenir négatif (avance à récupérer). */
    @PreAuthorize("hasAuthority('HR_MANAGE') or hasAuthority('FINANCE_MANAGE')")
    @Transactional
    public HrDto.EmployeeResponse pay(Long employeeId, HrDto.PaymentRequest r) {
        Employee e = employees.lockById(employeeId).orElseThrow(() -> new NotFoundException("Employé introuvable."));
        BigDecimal amount = Money.strict(r.montant(), "Montant");
        EmployeePayment p = payments.save(new EmployeePayment(numbers.nextYearly("PAIE_EMPLOYE", "PAI"), e, r.type(), r.mode(),
                amount, Text.clean(r.reference()), SecurityUtils.username()));
        finance.record(CashMovementType.DECAISSEMENT, r.mode(), amount,
                label(r.type()) + " " + e.fullName() + " (" + p.getNumero() + ")", "PAIE", p.getId());
        audit.log(SecurityUtils.username(), "EMPLOYEE_PAYMENT", "Employee", String.valueOf(employeeId),
                p.getNumero() + " " + r.type() + " " + amount.toPlainString() + " DT");
        return summaries(List.of(e)).get(0);
    }

    @PreAuthorize("hasAuthority('HR_VIEW') or hasAuthority('FINANCE_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<HrDto.PaymentLine> searchPayments(Long employeeId, EmployeePayment.Type type, LocalDate from,
                                                          LocalDate to, int p, int size) {
        Instant f = from == null ? null : from.atStartOfDay(TUNIS).toInstant();
        Instant t = to == null ? null : to.plusDays(1).atStartOfDay(TUNIS).toInstant();
        return PageResponse.of(payments.search(employeeId, type, f, t, page(p, size, Sort.by(Sort.Direction.DESC, "id"))),
                HrDto.PaymentLine::from);
    }

    private static String label(EmployeePayment.Type type) {
        return switch (type) {
            case SALAIRE -> "Salaire";
            case TJM -> "Journées";
            case AVANCE -> "Avance";
        };
    }

    // ---------------------------------------------------------------- calcul du reste dû

    private List<HrDto.EmployeeResponse> summaries(List<Employee> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(Employee::getId).toList();
        Map<Long, Object[]> earned = new HashMap<>();
        workDays.earnedBy(ids).forEach(r -> earned.put((Long) r[0], r));
        Map<Long, BigDecimal> paid = new HashMap<>();
        payments.paidBy(ids).forEach(r -> paid.put((Long) r[0], Money.ofAny(r[1])));
        Map<Long, WorkDay> todays = new HashMap<>();
        workDays.findByEmployeeIdInAndJour(ids, today()).forEach(w -> todays.put(w.getEmployee().getId(), w));
        return list.stream().map(e -> {
            Object[] er = earned.get(e.getId());
            long jours = er == null ? 0 : ((Number) er[1]).longValue();
            BigDecimal gagne = er == null ? Money.zero() : Money.ofAny(er[2]);
            BigDecimal paye = paid.getOrDefault(e.getId(), Money.zero());
            WorkDay w = todays.get(e.getId());
            return new HrDto.EmployeeResponse(e.getId(), e.getMatricule(), e.getPrenom(), e.getNom(), e.getCin(),
                    e.getTelephone(), e.getAdresse(), e.getTjm(), e.isActif(), jours, gagne, paye, gagne.subtract(paye),
                    w == null ? null : HrDto.WorkDayLine.from(w), w != null && w.isRunning());
        }).toList();
    }

    private Employee employee(Long id) {
        return employees.findById(id).orElseThrow(() -> new NotFoundException("Employé introuvable."));
    }
}
