package com.huilerie.erp.packaging;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.inventory.StockLot;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Conditionnement : huile en vrac → produits finis (bouteilles, bidons), avec consommation d'emballages. */
@Entity
@Table(name = "packaging_orders")
public class PackagingOrder {

    public static final String VALIDE = "VALIDE";
    public static final String ANNULE = "ANNULE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Column(nullable = false, length = 20)
    private String statut = VALIDE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_article_id", nullable = false)
    private Article source;

    @ManyToOne
    @JoinColumn(name = "source_lot_id")
    private StockLot sourceLot;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_article_id", nullable = false)
    private Article produit;

    @ManyToOne
    @JoinColumn(name = "produit_lot_id")
    private StockLot produitLot;

    @Column(name = "quantite_produite", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteProduite;

    @Column(name = "volume_utilise", nullable = false, precision = 15, scale = 3)
    private BigDecimal volumeUtilise;

    private String notes;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "annule_par", length = 64)
    private String annulePar;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Consumption> consommations = new ArrayList<>();

    protected PackagingOrder() {
    }

    PackagingOrder(String numero, Article source, StockLot sourceLot, Article produit, StockLot produitLot,
                   BigDecimal quantite, BigDecimal volume, String notes, String username) {
        this.numero = numero;
        this.source = source;
        this.sourceLot = sourceLot;
        this.produit = produit;
        this.produitLot = produitLot;
        this.quantiteProduite = quantite;
        this.volumeUtilise = volume;
        this.notes = notes;
        this.username = username;
        this.createdAt = Instant.now();
    }

    void addConsumption(Article a, BigDecimal q) {
        consommations.add(new Consumption(this, a, q));
    }

    void cancel(String user, String motif) {
        statut = ANNULE;
        annulePar = user;
        motifAnnulation = motif;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public String getStatut() { return statut; }
    public Article getSource() { return source; }
    public StockLot getSourceLot() { return sourceLot; }
    public Article getProduit() { return produit; }
    public StockLot getProduitLot() { return produitLot; }
    public BigDecimal getQuantiteProduite() { return quantiteProduite; }
    public BigDecimal getVolumeUtilise() { return volumeUtilise; }
    public String getNotes() { return notes; }
    public String getUsername() { return username; }
    public Instant getCreatedAt() { return createdAt; }
    public String getAnnulePar() { return annulePar; }
    public String getMotifAnnulation() { return motifAnnulation; }
    public List<Consumption> getConsommations() { return consommations; }

    @Entity(name = "PackagingConsumption")
    @Table(name = "packaging_consumptions")
    public static class Consumption {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "order_id", nullable = false)
        private PackagingOrder order;

        @ManyToOne(optional = false)
        @JoinColumn(name = "article_id", nullable = false)
        private Article article;

        @Column(nullable = false, precision = 15, scale = 3)
        private BigDecimal quantite;

        protected Consumption() {
        }

        Consumption(PackagingOrder order, Article article, BigDecimal quantite) {
            this.order = order;
            this.article = article;
            this.quantite = quantite;
        }

        public Article getArticle() { return article; }
        public BigDecimal getQuantite() { return quantite; }
    }
}
