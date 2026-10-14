package com.huilerie.erp.purchases;

import com.huilerie.erp.catalog.Article;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_lines")
public class PurchaseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 200)
    private String designation;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    @Column(name = "quantite_recue", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteRecue;

    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 3)
    private BigDecimal prixUnitaire;

    @Column(name = "remise_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal remisePct;

    @Column(name = "taux_tva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxTva;

    @Column(name = "total_ht", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalHt;

    @Column(name = "total_tva", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalTva;

    protected PurchaseLine() {
    }

    PurchaseLine(Purchase purchase, Article article, BigDecimal quantite, BigDecimal prixUnitaire, BigDecimal remisePct,
                 BigDecimal tauxTva, BigDecimal totalHt, BigDecimal totalTva) {
        this.purchase = purchase;
        this.article = article;
        this.designation = article.getNom();
        this.quantite = quantite;
        this.quantiteRecue = BigDecimal.ZERO.setScale(3);
        this.prixUnitaire = prixUnitaire;
        this.remisePct = remisePct;
        this.tauxTva = tauxTva;
        this.totalHt = totalHt;
        this.totalTva = totalTva;
    }

    public BigDecimal remaining() {
        return quantite.subtract(quantiteRecue);
    }

    void addReceived(BigDecimal q) {
        quantiteRecue = quantiteRecue.add(q);
    }

    public Long getId() { return id; }
    public Purchase getPurchase() { return purchase; }
    public Article getArticle() { return article; }
    public String getDesignation() { return designation; }
    public BigDecimal getQuantite() { return quantite; }
    public BigDecimal getQuantiteRecue() { return quantiteRecue; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public BigDecimal getRemisePct() { return remisePct; }
    public BigDecimal getTauxTva() { return tauxTva; }
    public BigDecimal getTotalHt() { return totalHt; }
    public BigDecimal getTotalTva() { return totalTva; }
}
