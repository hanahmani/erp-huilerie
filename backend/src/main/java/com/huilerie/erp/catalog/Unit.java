package com.huilerie.erp.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String libelle;

    @Column(nullable = false)
    private boolean actif = true;

    protected Unit() {
    }

    public Unit(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String v) { libelle = v; }
    public boolean isActif() { return actif; }
    public void setActif(boolean v) { actif = v; }
}
