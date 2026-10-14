package com.huilerie.erp.quality;

import com.huilerie.erp.inventory.StockLot;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Contrôle qualité d'un lot : enregistré une fois, jamais modifié. */
@Entity
@Immutable
@Table(name = "quality_controls")
public class QualityControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private StockLot lot;

    @Column(name = "date_controle", nullable = false)
    private Instant date;

    @Column(nullable = false, length = 64)
    private String controleur;

    @Column(nullable = false, length = 20)
    private String resultat;

    @Column(length = 500)
    private String observations;

    @OneToMany(mappedBy = "control", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Result> resultats = new ArrayList<>();

    protected QualityControl() {
    }

    QualityControl(String numero, StockLot lot, String controleur, String observations) {
        this.numero = numero;
        this.lot = lot;
        this.controleur = controleur;
        this.observations = observations;
        this.date = Instant.now();
    }

    void addResult(QualityCriterion c, BigDecimal v) {
        resultats.add(new Result(this, c, v, c.accepts(v)));
        this.resultat = resultats.stream().allMatch(Result::isConforme) ? "CONFORME" : "NON_CONFORME";
    }

    public boolean isConforme() {
        return "CONFORME".equals(resultat);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public StockLot getLot() { return lot; }
    public Instant getDate() { return date; }
    public String getControleur() { return controleur; }
    public String getResultat() { return resultat; }
    public String getObservations() { return observations; }
    public List<Result> getResultats() { return resultats; }

    @Entity(name = "QualityResult")
    @Immutable
    @Table(name = "quality_results")
    public static class Result {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @ManyToOne(optional = false)
        @JoinColumn(name = "control_id", nullable = false)
        private QualityControl control;
        @ManyToOne(optional = false)
        @JoinColumn(name = "criterion_id", nullable = false)
        private QualityCriterion criterion;
        @Column(nullable = false, precision = 12, scale = 4)
        private BigDecimal valeur;
        @Column(nullable = false)
        private boolean conforme;

        protected Result() {
        }

        Result(QualityControl control, QualityCriterion criterion, BigDecimal valeur, boolean conforme) {
            this.control = control;
            this.criterion = criterion;
            this.valeur = valeur;
            this.conforme = conforme;
        }

        public QualityCriterion getCriterion() { return criterion; }
        public BigDecimal getValeur() { return valeur; }
        public boolean isConforme() { return conforme; }
    }
}
