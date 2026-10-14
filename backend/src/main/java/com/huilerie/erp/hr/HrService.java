package com.huilerie.erp.hr;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.numbering.DocumentNumberService;
import com.huilerie.erp.common.response.PageResponse;
import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ressources humaines : employés, contrats, congés et absences.
 * Jours ouvrés de congé = lundi à vendredi, hors jours fériés saisis.
 * Processus : employé → demande → responsable → validation / refus → mise à jour du solde.
 */
@Service
public class HrService {

    private final HrServiceRepository services;
    private final HrPositionRepository positions;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final AbsenceRepository absences;
    private final PublicHolidayRepository holidays;
    private final UserRepository users;
    private final DocumentNumberService numbers;
    private final AuditService audit;

    public HrService(HrServiceRepository services, HrPositionRepository positions, EmployeeRepository employees,
                     LeaveRequestRepository leaves, AbsenceRepository absences, PublicHolidayRepository holidays,
                     UserRepository users, DocumentNumberService numbers, AuditService audit) {
        this.services = services;
        this.positions = positions;
        this.employees = employees;
        this.leaves = leaves;
        this.absences = absences;
        this.holidays = holidays;
        this.users = users;
        this.numbers = numbers;
        this.audit = audit;
    }

    private static PageRequest page(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
    }

    // ---------------------------------------------------------------- services et postes
    @PreAuthorize("hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public List<HrDto.Ref> listServices() {
        return services.findAllByOrderByNomAsc().stream().map(s -> new HrDto.Ref(s.getId(), s.getNom(), s.isActif())).toList();
    }

    @PreAuthorize("hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public List<HrDto.Ref> listPositions() {
        return positions.findAllByOrderByNomAsc().stream().map(s -> new HrDto.Ref(s.getId(), s.getNom(), s.isActif())).toList();
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.Ref saveRef(boolean service, Long id, String nom) {
        String n = nom.trim();
        HrRef.Base ref;
        if (service) {
            if (id == null && services.existsByNomIgnoreCase(n)) throw new BusinessException("Ce service existe déjà.");
            ref = id == null ? new HrRef.Service() : services.findById(id).orElseThrow(() -> new NotFoundException("Service introuvable."));
            ref.setNom(n);
            ref = services.save((HrRef.Service) ref);
        } else {
            if (id == null && positions.existsByNomIgnoreCase(n)) throw new BusinessException("Ce poste existe déjà.");
            ref = id == null ? new HrRef.Position() : positions.findById(id).orElseThrow(() -> new NotFoundException("Poste introuvable."));
            ref.setNom(n);
            ref = positions.save((HrRef.Position) ref);
        }
        return new HrDto.Ref(ref.getId(), ref.getNom(), ref.isActif());
    }

    // ---------------------------------------------------------------- employés
    @PreAuthorize("hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<HrDto.EmployeeResponse> searchEmployees(String q, Long serviceId, String statut, String contrat,
                                                               int p, int size) {
        return PageResponse.of(employees.search(Text.search(q), serviceId, Text.clean(statut), Text.clean(contrat), page(p, size)),
                HrDto.EmployeeResponse::from);
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.EmployeeResponse saveEmployee(Long id, HrDto.EmployeeRequest r) {
        if (r.typeContrat().matches("CDD|SAISONNIER|STAGE") && r.dateFinContrat() == null) {
            throw new BusinessException("Indiquez la date de fin pour un contrat " + r.typeContrat() + ".");
        }
        if (r.dateFinContrat() != null && r.dateFinContrat().isBefore(r.dateEmbauche())) {
            throw new BusinessException("La fin de contrat doit suivre la date d'embauche.");
        }
        Employee e;
        if (id == null) {
            e = new Employee();
            e.setMatricule(numbers.nextCode("EMPLOYE", "EMP"));
        } else {
            e = employees.findById(id).orElseThrow(() -> new NotFoundException("Employé introuvable."));
        }
        e.setNom(r.nom().trim());
        e.setPrenom(r.prenom().trim());
        e.setDateNaissance(r.dateNaissance());
        e.setCin(Text.clean(r.cin()));
        e.setTelephone(Text.clean(r.telephone()));
        e.setEmail(Text.clean(r.email()));
        e.setAdresse(Text.clean(r.adresse()));
        e.setService(r.serviceId() == null ? null : services.findById(r.serviceId()).orElseThrow(() -> new NotFoundException("Service introuvable.")));
        e.setPoste(r.posteId() == null ? null : positions.findById(r.posteId()).orElseThrow(() -> new NotFoundException("Poste introuvable.")));
        e.setDateEmbauche(r.dateEmbauche());
        e.setTypeContrat(r.typeContrat());
        e.setDateFinContrat(r.dateFinContrat());
        e.setStatut(r.statut());
        e.setSoldeConges(r.soldeConges().setScale(1, RoundingMode.HALF_UP));
        if (r.userId() == null) {
            e.setUser(null);
        } else if (e.getUser() == null || !e.getUser().getId().equals(r.userId())) {
            if (employees.existsByUserId(r.userId())) {
                throw new BusinessException("Ce compte utilisateur est déjà lié à un autre employé.");
            }
            AppUser u = users.findById(r.userId()).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
            e.setUser(u);
        }
        e = employees.save(e);
        audit.log(SecurityUtils.username(), id == null ? "EMPLOYEE_CREATE" : "EMPLOYEE_UPDATE", "Employee",
                String.valueOf(e.getId()), e.getMatricule());
        return HrDto.EmployeeResponse.from(e);
    }

    /** Fiche de l'employé lié au compte connecté (pour « Mes congés »). */
    @PreAuthorize("hasAuthority('LEAVE_REQUEST')")
    @Transactional(readOnly = true)
    public HrDto.EmployeeResponse me() {
        return employees.findByUserUsername(SecurityUtils.username()).map(HrDto.EmployeeResponse::from).orElse(null);
    }

    // ---------------------------------------------------------------- congés

    /** Jours ouvrés entre deux dates incluses : hors samedi, dimanche et jours fériés. */
    public BigDecimal workingDays(LocalDate from, LocalDate to) {
        Set<LocalDate> off = new HashSet<>();
        holidays.findByJourBetween(from, to).forEach(h -> off.add(h.getJour()));
        int n = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY && !off.contains(d)) {
                n++;
            }
        }
        return BigDecimal.valueOf(n).setScale(1);
    }

    @PreAuthorize("hasAuthority('LEAVE_REQUEST') or hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<HrDto.Leave> searchLeaves(Long employeeId, String statut, boolean mine, int p, int size) {
        Long target = employeeId;
        if (mine || !SecurityUtils.has("HR_VIEW") && !SecurityUtils.has("LEAVE_VALIDATE")) {
            Employee me = employees.findByUserUsername(SecurityUtils.username())
                    .orElseThrow(() -> new BusinessException("Votre compte n'est lié à aucune fiche employé."));
            target = me.getId();
        }
        return PageResponse.of(leaves.search(target, Text.clean(statut), page(p, size)), HrDto.Leave::from);
    }

    @PreAuthorize("hasAuthority('LEAVE_REQUEST') or hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.Leave requestLeave(HrDto.LeaveRequestBody r) {
        Employee e;
        if (r.employeeId() != null && SecurityUtils.has("HR_MANAGE")) {
            e = employees.findById(r.employeeId()).orElseThrow(() -> new NotFoundException("Employé introuvable."));
        } else {
            e = employees.findByUserUsername(SecurityUtils.username())
                    .orElseThrow(() -> new BusinessException("Votre compte n'est lié à aucune fiche employé : contactez les RH."));
        }
        if (!"ACTIF".equals(e.getStatut())) {
            throw new BusinessException("L'employé n'est pas actif.");
        }
        if (r.dateFin().isBefore(r.dateDebut())) {
            throw new BusinessException("La date de fin doit suivre la date de début.");
        }
        BigDecimal days = workingDays(r.dateDebut(), r.dateFin());
        if (days.signum() == 0) {
            throw new BusinessException("La période ne contient aucun jour ouvré.");
        }
        if (leaves.overlapping(e.getId(), r.dateDebut(), r.dateFin()) > 0) {
            throw new BusinessException("Cette période chevauche une autre demande en attente ou validée.");
        }
        if ("CONGE_ANNUEL".equals(r.type()) && days.compareTo(e.getSoldeConges()) > 0) {
            throw new BusinessException("Solde insuffisant : " + e.getSoldeConges().toPlainString() + " jour(s) disponible(s) pour "
                    + days.toPlainString() + " demandé(s).");
        }
        LeaveRequest l = leaves.save(new LeaveRequest(e, r.type(), r.dateDebut(), r.dateFin(), days, Text.clean(r.motif()),
                SecurityUtils.username()));
        audit.log(SecurityUtils.username(), "LEAVE_REQUEST", "Employee", String.valueOf(e.getId()),
                r.type() + " " + r.dateDebut() + " → " + r.dateFin());
        return HrDto.Leave.from(l);
    }

    @PreAuthorize("hasAuthority('LEAVE_VALIDATE')")
    @Transactional
    public HrDto.Leave decide(Long id, HrDto.DecisionRequest r) {
        LeaveRequest l = leaves.findById(id).orElseThrow(() -> new NotFoundException("Demande introuvable."));
        if (!"EN_ATTENTE".equals(l.getStatut())) {
            throw new BusinessException("Cette demande a déjà été traitée.");
        }
        if (l.getEmployee().getUser() != null && l.getEmployee().getUser().getUsername().equals(SecurityUtils.username())) {
            throw new AccessDeniedException("Vous ne pouvez pas valider votre propre demande.");
        }
        if (r.accepter()) {
            if ("CONGE_ANNUEL".equals(l.getType())) {
                if (l.getJours().compareTo(l.getEmployee().getSoldeConges()) > 0) {
                    throw new BusinessException("Le solde de l'employé ne couvre plus cette demande.");
                }
                l.getEmployee().debitLeave(l.getJours());
            }
            l.decide("VALIDEE", SecurityUtils.username(), null);
        } else {
            if (Text.clean(r.motifRefus()) == null) {
                throw new BusinessException("Indiquez le motif du refus.");
            }
            l.decide("REFUSEE", SecurityUtils.username(), r.motifRefus().trim());
        }
        audit.log(SecurityUtils.username(), r.accepter() ? "LEAVE_APPROVE" : "LEAVE_REJECT", "LeaveRequest", String.valueOf(id), null);
        return HrDto.Leave.from(l);
    }

    @PreAuthorize("hasAuthority('LEAVE_REQUEST') or hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.Leave cancelLeave(Long id) {
        LeaveRequest l = leaves.findById(id).orElseThrow(() -> new NotFoundException("Demande introuvable."));
        boolean own = l.getDemandePar().equals(SecurityUtils.username());
        if (!own && !SecurityUtils.has("HR_MANAGE")) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos propres demandes.");
        }
        if (!"EN_ATTENTE".equals(l.getStatut())) {
            throw new BusinessException("Seule une demande en attente peut être annulée.");
        }
        l.decide("ANNULEE", SecurityUtils.username(), null);
        return HrDto.Leave.from(l);
    }

    // ---------------------------------------------------------------- absences
    @PreAuthorize("hasAuthority('HR_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<HrDto.AbsenceResponse> searchAbsences(Long employeeId, LocalDate from, LocalDate to, int p, int size) {
        return PageResponse.of(absences.search(employeeId, from, to, page(p, size)), HrDto.AbsenceResponse::from);
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.AbsenceResponse recordAbsence(HrDto.AbsenceRequest r) {
        Employee e = employees.findById(r.employeeId()).orElseThrow(() -> new NotFoundException("Employé introuvable."));
        if (absences.existsByEmployeeIdAndJour(e.getId(), r.jour())) {
            throw new BusinessException("Une absence est déjà enregistrée ce jour-là pour cet employé.");
        }
        BigDecimal duree = r.duree().compareTo(BigDecimal.ONE) >= 0 ? new BigDecimal("1.0") : new BigDecimal("0.5");
        Absence a = absences.save(new Absence(e, r.jour(), duree, Text.clean(r.motif()), r.justifiee(), SecurityUtils.username()));
        audit.log(SecurityUtils.username(), "ABSENCE_CREATE", "Employee", String.valueOf(e.getId()), r.jour().toString());
        return HrDto.AbsenceResponse.from(a);
    }

    // ---------------------------------------------------------------- jours fériés
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<HrDto.Holiday> listHolidays() {
        return holidays.findAllByOrderByJourAsc().stream().map(h -> new HrDto.Holiday(h.getId(), h.getJour(), h.getLibelle())).toList();
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public HrDto.Holiday addHoliday(HrDto.HolidayRequest r) {
        if (holidays.existsByJour(r.jour())) {
            throw new BusinessException("Ce jour est déjà férié.");
        }
        PublicHoliday h = holidays.save(new PublicHoliday(r.jour(), r.libelle().trim()));
        return new HrDto.Holiday(h.getId(), h.getJour(), h.getLibelle());
    }

    @PreAuthorize("hasAuthority('HR_MANAGE')")
    @Transactional
    public void deleteHoliday(Long id) {
        holidays.deleteById(id);
    }
}
