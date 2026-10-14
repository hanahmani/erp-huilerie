package com.huilerie.erp.hr;

import jakarta.persistence.*;

/** Services et postes : deux petits référentiels de même forme. */
public final class HrRef {

    private HrRef() {
    }

    @MappedSuperclass
    public abstract static class Base {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true, length = 100)
        private String nom;

        @Column(nullable = false)
        private boolean actif = true;

        public Long getId() { return id; }
        public String getNom() { return nom; }
        public void setNom(String v) { nom = v; }
        public boolean isActif() { return actif; }
        public void setActif(boolean v) { actif = v; }
    }

    @Entity(name = "HrService")
    @Table(name = "hr_services")
    public static class Service extends Base {
    }

    @Entity(name = "HrPosition")
    @Table(name = "hr_positions")
    public static class Position extends Base {
    }
}
