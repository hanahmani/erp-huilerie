package com.huilerie.erp.hr;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal jours;

    private String motif;

    @Column(nullable = false, length = 20)
    private String statut = "EN_ATTENTE";

    @Column(name = "demande_par", nullable = false, length = 64)
    private String demandePar;

    @Column(name = "demande_le", nullable = false)
    private Instant demandeLe;

    @Column(name = "decide_par", length = 64)
    private String decidePar;

    @Column(name = "decide_le")
    private Instant decideLe;

    @Column(name = "motif_refus")
    private String motifRefus;

    protected LeaveRequest() {
    }

    LeaveRequest(Employee e, String type, LocalDate debut, LocalDate fin, BigDecimal jours, String motif, String user) {
        this.employee = e;
        this.type = type;
        this.dateDebut = debut;
        this.dateFin = fin;
        this.jours = jours;
        this.motif = motif;
        this.demandePar = user;
        this.demandeLe = Instant.now();
    }

    void decide(String statut, String user, String motifRefus) {
        this.statut = statut;
        this.decidePar = user;
        this.decideLe = Instant.now();
        this.motifRefus = motifRefus;
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public String getType() { return type; }
    public LocalDate getDateDebut() { return dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public BigDecimal getJours() { return jours; }
    public String getMotif() { return motif; }
    public String getStatut() { return statut; }
    public String getDemandePar() { return demandePar; }
    public Instant getDemandeLe() { return demandeLe; }
    public String getDecidePar() { return decidePar; }
    public Instant getDecideLe() { return decideLe; }
    public String getMotifRefus() { return motifRefus; }
}
