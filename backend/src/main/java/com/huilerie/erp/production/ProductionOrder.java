package com.huilerie.erp.production;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.inventory.StockLot;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ordre de production (trituration des olives de l'huilerie).
 * PLANIFIE → EN_COURS (olives consommées) → TERMINE (huile et sous-produits en stock) ; ANNULE avant démarrage.
 */
@Entity
@Table(name = "production_orders")
public class ProductionOrder {

    public enum Status { PLANIFIE, EN_COURS, TERMINE, ANNULE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "date_prevue", nullable = false)
    private LocalDate datePrevue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status statut = Status.PLANIFIE;

    @Column(length = 60)
    private String ligne;

    @ManyToOne(optional = false)
    @JoinColumn(name = "huile_article_id", nullable = false)
    private Article huileArticle;

    @ManyToOne
    @JoinColumn(name = "huile_lot_id")
    private StockLot huileLot;

    @Column(name = "olives_kg", nullable = false, precision = 15, scale = 3)
    private BigDecimal olivesKg;

    @Column(name = "huile_litres", precision = 15, scale = 3)
    private BigDecimal huileLitres;

    @Column(name = "pertes_kg", precision = 15, scale = 3)
    private BigDecimal pertesKg;

    @Column(precision = 7, scale = 3)
    private BigDecimal rendement;

    @Column(precision = 5, scale = 2)
    private BigDecimal acidite;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private OilCategory categorie;

    private String notes;

    @Column(name = "cree_par", nullable = false, length = 64)
    private String creePar;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "demarre_le")
    private Instant demarreLe;

    @Column(name = "termine_le")
    private Instant termineLe;

    @Column(name = "termine_par", length = 64)
    private String terminePar;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Input> entrees = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<ByProduct> sousProduits = new ArrayList<>();

    protected ProductionOrder() {
    }

    ProductionOrder(String numero, String creePar) {
        this.numero = numero;
        this.creePar = creePar;
        this.createdAt = Instant.now();
    }

    void plan(LocalDate date, String ligne, Article huile, String notes) {
        this.datePrevue = date;
        this.ligne = ligne;
        this.huileArticle = huile;
        this.notes = notes;
    }

    void clearInputs() {
        entrees.clear();
    }

    void addInput(Article a, StockLot lot, BigDecimal q) {
        entrees.add(new Input(this, a, lot, q));
    }

    void setOlivesKg(BigDecimal kg) {
        this.olivesKg = kg;
    }

    void start() {
        statut = Status.EN_COURS;
        demarreLe = Instant.now();
    }

    void addByProduct(Article a, BigDecimal q) {
        sousProduits.add(new ByProduct(this, a, q));
    }

    void finish(StockLot lot, BigDecimal huile, BigDecimal pertes, BigDecimal acidite, String user) {
        this.huileLot = lot;
        this.huileLitres = huile;
        this.pertesKg = pertes;
        this.acidite = acidite;
        this.categorie = OilCategory.fromAcidity(acidite);
        this.rendement = huile.multiply(BigDecimal.valueOf(100)).divide(olivesKg, 3, RoundingMode.HALF_UP);
        this.statut = Status.TERMINE;
        this.termineLe = Instant.now();
        this.terminePar = user;
    }

    void cancel(String motif) {
        statut = Status.ANNULE;
        motifAnnulation = motif;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public LocalDate getDatePrevue() { return datePrevue; }
    public Status getStatut() { return statut; }
    public String getLigne() { return ligne; }
    public Article getHuileArticle() { return huileArticle; }
    public StockLot getHuileLot() { return huileLot; }
    public BigDecimal getOlivesKg() { return olivesKg; }
    public BigDecimal getHuileLitres() { return huileLitres; }
    public BigDecimal getPertesKg() { return pertesKg; }
    public BigDecimal getRendement() { return rendement; }
    public BigDecimal getAcidite() { return acidite; }
    public OilCategory getCategorie() { return categorie; }
    public String getNotes() { return notes; }
    public String getCreePar() { return creePar; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDemarreLe() { return demarreLe; }
    public Instant getTermineLe() { return termineLe; }
    public String getTerminePar() { return terminePar; }
    public String getMotifAnnulation() { return motifAnnulation; }
    public List<Input> getEntrees() { return entrees; }
    public List<ByProduct> getSousProduits() { return sousProduits; }

    @Entity(name = "ProductionInput")
    @Table(name = "production_inputs")
    public static class Input {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "order_id", nullable = false)
        private ProductionOrder order;

        @ManyToOne(optional = false)
        @JoinColumn(name = "article_id", nullable = false)
        private Article article;

        @ManyToOne
        @JoinColumn(name = "lot_id")
        private StockLot lot;

        @Column(nullable = false, precision = 15, scale = 3)
        private BigDecimal quantite;

        protected Input() {
        }

        Input(ProductionOrder order, Article article, StockLot lot, BigDecimal quantite) {
            this.order = order;
            this.article = article;
            this.lot = lot;
            this.quantite = quantite;
        }

        public Article getArticle() { return article; }
        public StockLot getLot() { return lot; }
        public BigDecimal getQuantite() { return quantite; }
    }

    @Entity(name = "ProductionByProduct")
    @Table(name = "production_byproducts")
    public static class ByProduct {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "order_id", nullable = false)
        private ProductionOrder order;

        @ManyToOne(optional = false)
        @JoinColumn(name = "article_id", nullable = false)
        private Article article;

        @Column(nullable = false, precision = 15, scale = 3)
        private BigDecimal quantite;

        protected ByProduct() {
        }

        ByProduct(ProductionOrder order, Article article, BigDecimal quantite) {
            this.order = order;
            this.article = article;
            this.quantite = quantite;
        }

        public Article getArticle() { return article; }
        public BigDecimal getQuantite() { return quantite; }
    }
}
