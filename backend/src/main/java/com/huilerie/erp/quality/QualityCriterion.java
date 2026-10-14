package com.huilerie.erp.quality;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Critère de qualité paramétrable (ex. acidité ≤ 0,8 %, peroxyde ≤ 20 meq O2/kg, K232 ≤ 2,5). */
@Entity
@Table(name = "quality_criteria")
public class QualityCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(length = 30)
    private String unite;

    @Column(name = "valeur_min", precision = 12, scale = 4)
    private BigDecimal valeurMin;

    @Column(name = "valeur_max", precision = 12, scale = 4)
    private BigDecimal valeurMax;

    @Column(nullable = false)
    private boolean actif = true;

    protected QualityCriterion() {
    }

    public QualityCriterion(String nom) {
        this.nom = nom;
    }

    public boolean accepts(BigDecimal v) {
        return (valeurMin == null || v.compareTo(valeurMin) >= 0) && (valeurMax == null || v.compareTo(valeurMax) <= 0);
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public String getUnite() { return unite; }
    public void setUnite(String v) { unite = v; }
    public BigDecimal getValeurMin() { return valeurMin; }
    public void setValeurMin(BigDecimal v) { valeurMin = v; }
    public BigDecimal getValeurMax() { return valeurMax; }
    public void setValeurMax(BigDecimal v) { valeurMax = v; }
    public boolean isActif() { return actif; }
    public void setActif(boolean v) { actif = v; }
}
