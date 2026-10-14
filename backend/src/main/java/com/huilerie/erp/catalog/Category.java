package com.huilerie.erp.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    private String description;

    @Column(nullable = false)
    private boolean actif = true;

    protected Category() {
    }

    public Category(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public boolean isActif() { return actif; }
    public void setActif(boolean v) { actif = v; }
}
