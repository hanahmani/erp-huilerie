package com.huilerie.erp.hr;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Jours fériés saisis par l'administration (fêtes religieuses variables chaque année). */
@Entity
@Table(name = "public_holidays")
public class PublicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate jour;

    @Column(nullable = false, length = 100)
    private String libelle;

    protected PublicHoliday() {
    }

    public PublicHoliday(LocalDate jour, String libelle) {
        this.jour = jour;
        this.libelle = libelle;
    }

    public Long getId() { return id; }
    public LocalDate getJour() { return jour; }
    public String getLibelle() { return libelle; }
}
