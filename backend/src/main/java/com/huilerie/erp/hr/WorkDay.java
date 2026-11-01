package com.huilerie.erp.hr;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Journée de travail pointée (Play / Stop). Une seule par employé et par jour ;
 * elle compte pour un TJM, figé au moment du pointage. Heures modifiables à la main.
 */
@Entity
@Table(name = "work_days")
public class WorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate jour;

    @Column(name = "heure_debut", nullable = false)
    private Instant heureDebut;

    @Column(name = "heure_fin")
    private Instant heureFin;

    @Column(nullable = false, precision = 12, scale = 3, updatable = false)
    private BigDecimal tjm;

    @Column(name = "saisi_par", nullable = false, length = 64, updatable = false)
    private String saisiPar;

    @Column(name = "modifie_par", length = 64)
    private String modifiePar;

    protected WorkDay() {
    }

    WorkDay(Employee employee, LocalDate jour, Instant heureDebut, Instant heureFin, String user) {
        this.employee = employee;
        this.jour = jour;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.tjm = employee.getTjm();
        this.saisiPar = user;
    }

    void stop(Instant fin) {
        this.heureFin = fin;
    }

    void correct(LocalDate jour, Instant debut, Instant fin, String user) {
        this.jour = jour;
        this.heureDebut = debut;
        this.heureFin = fin;
        this.modifiePar = user;
    }

    public boolean isRunning() {
        return heureFin == null;
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LocalDate getJour() { return jour; }
    public Instant getHeureDebut() { return heureDebut; }
    public Instant getHeureFin() { return heureFin; }
    public BigDecimal getTjm() { return tjm; }
    public String getSaisiPar() { return saisiPar; }
    public String getModifiePar() { return modifiePar; }
}
