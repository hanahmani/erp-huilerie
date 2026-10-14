package com.huilerie.erp.hr;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "absences")
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate jour;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal duree;

    private String motif;

    @Column(nullable = false)
    private boolean justifiee;

    @Column(name = "saisi_par", nullable = false, length = 64)
    private String saisiPar;

    protected Absence() {
    }

    Absence(Employee e, LocalDate jour, BigDecimal duree, String motif, boolean justifiee, String user) {
        this.employee = e;
        this.jour = jour;
        this.duree = duree;
        this.motif = motif;
        this.justifiee = justifiee;
        this.saisiPar = user;
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LocalDate getJour() { return jour; }
    public BigDecimal getDuree() { return duree; }
    public String getMotif() { return motif; }
    public boolean isJustifiee() { return justifiee; }
    public String getSaisiPar() { return saisiPar; }
}
