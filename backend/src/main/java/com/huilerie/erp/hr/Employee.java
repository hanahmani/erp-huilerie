package com.huilerie.erp.hr;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Employé : identité, taux journalier (TJM) et statut actif / inactif. */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(length = 20)
    private String cin;

    @Column(length = 50)
    private String telephone;

    private String adresse;

    /** Taux journalier en DT ; copié sur chaque journée pointée au moment du Play. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal tjm;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Employee() {
    }

    Employee(String matricule) {
        this.matricule = matricule;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    void update(String nom, String prenom, String cin, String telephone, String adresse, BigDecimal tjm) {
        this.nom = nom;
        this.prenom = prenom;
        this.cin = cin;
        this.telephone = telephone;
        this.adresse = adresse;
        this.tjm = tjm;
    }

    void setActif(boolean actif) {
        this.actif = actif;
    }

    public String fullName() {
        return prenom + " " + nom;
    }

    public Long getId() { return id; }
    public String getMatricule() { return matricule; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getCin() { return cin; }
    public String getTelephone() { return telephone; }
    public String getAdresse() { return adresse; }
    public BigDecimal getTjm() { return tjm; }
    public boolean isActif() { return actif; }
    public Instant getCreatedAt() { return createdAt; }
}
