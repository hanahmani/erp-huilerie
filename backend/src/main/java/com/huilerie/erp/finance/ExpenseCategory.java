package com.huilerie.erp.finance;

import jakarta.persistence.*;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(nullable = false)
    private boolean actif = true;

    protected ExpenseCategory() {
    }

    public ExpenseCategory(String nom) {
        this.nom = nom;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public boolean isActif() { return actif; }
    public void setActif(boolean v) { actif = v; }
}
