package com.huilerie.erp.settings;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Taux de TVA paramétrables (ex. 19 %, 13 %, 7 %, 0 %), saisis par l'administrateur. */
@Entity
@Table(name = "taxes")
public class Tax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nom;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taux;

    @Column(nullable = false)
    private boolean actif = true;

    protected Tax() {
    }

    public Tax(String nom, BigDecimal taux) {
        this.nom = nom;
        this.taux = taux;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal v) { taux = v; }
    public boolean isActif() { return actif; }
    public void setActif(boolean v) { actif = v; }
}
