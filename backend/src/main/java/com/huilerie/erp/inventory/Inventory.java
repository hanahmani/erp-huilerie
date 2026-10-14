package com.huilerie.erp.inventory;

import com.huilerie.erp.catalog.Article;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Inventaire physique : comptage puis validation qui génère les mouvements d'écart. */
@Entity
@Table(name = "inventories")
public class Inventory {

    public static final String EN_COURS = "EN_COURS";
    public static final String VALIDE = "VALIDE";
    public static final String ANNULE = "ANNULE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(nullable = false, length = 20)
    private String statut = EN_COURS;

    @Column(name = "cree_par", nullable = false, length = 64)
    private String creePar;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "valide_par", length = 64)
    private String validePar;

    @Column(name = "valide_le")
    private Instant valideLe;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Line> lignes = new ArrayList<>();

    protected Inventory() {
    }

    Inventory(String numero, String libelle, String creePar) {
        this.numero = numero;
        this.libelle = libelle;
        this.creePar = creePar;
        this.createdAt = Instant.now();
    }

    void addLine(Article a, StockLot lot, BigDecimal theorique) {
        lignes.add(new Line(this, a, lot, theorique));
    }

    void validate(String user) {
        statut = VALIDE;
        validePar = user;
        valideLe = Instant.now();
    }

    void cancel() {
        statut = ANNULE;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public String getLibelle() { return libelle; }
    public String getStatut() { return statut; }
    public String getCreePar() { return creePar; }
    public Instant getCreatedAt() { return createdAt; }
    public String getValidePar() { return validePar; }
    public Instant getValideLe() { return valideLe; }
    public List<Line> getLignes() { return lignes; }

    @Entity(name = "InventoryLine")
    @Table(name = "inventory_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "inventory_id", nullable = false)
        private Inventory inventory;

        @ManyToOne(optional = false)
        @JoinColumn(name = "article_id", nullable = false)
        private Article article;

        @ManyToOne
        @JoinColumn(name = "lot_id")
        private StockLot lot;

        @Column(name = "quantite_theorique", nullable = false, precision = 15, scale = 3)
        private BigDecimal quantiteTheorique;

        @Column(name = "quantite_comptee", precision = 15, scale = 3)
        private BigDecimal quantiteComptee;

        protected Line() {
        }

        Line(Inventory inventory, Article article, StockLot lot, BigDecimal theorique) {
            this.inventory = inventory;
            this.article = article;
            this.lot = lot;
            this.quantiteTheorique = theorique;
        }

        void count(BigDecimal q) {
            quantiteComptee = q;
        }

        public Long getId() { return id; }
        public Article getArticle() { return article; }
        public StockLot getLot() { return lot; }
        public BigDecimal getQuantiteTheorique() { return quantiteTheorique; }
        public BigDecimal getQuantiteComptee() { return quantiteComptee; }
    }
}
