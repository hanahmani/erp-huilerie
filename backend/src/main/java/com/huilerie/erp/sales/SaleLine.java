package com.huilerie.erp.sales;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.inventory.StockLot;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_lines")
public class SaleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @Column(nullable = false, length = 200)
    private String designation;

    @Column(nullable = false, length = 20)
    private String unite;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    /** Quantité sortie du stock, dans l'unité de l'article (kg pour 10 L d'huile : 9,160). NULL = quantite. */
    @Column(name = "quantite_stock", precision = 15, scale = 3)
    private BigDecimal quantiteStock;

    @Column(name = "prix_unitaire", nullable = false, precision = 12, scale = 3)
    private BigDecimal prixUnitaire;

    @Column(name = "total_ligne", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalLigne;

    protected SaleLine() {
    }

    /** unite : celle de l'article, ou « L » pour une huile vendue au litre (quantiteStock en kg). */
    SaleLine(Sale sale, Article article, StockLot lot, String unite, BigDecimal quantite, BigDecimal quantiteStock,
             BigDecimal prixUnitaire, BigDecimal totalLigne) {
        this.sale = sale;
        this.article = article;
        this.lot = lot;
        this.designation = article.getNom();
        this.unite = unite;
        this.quantite = quantite;
        this.quantiteStock = unite.equals(article.getUnite().getCode()) ? null : quantiteStock;
        this.prixUnitaire = prixUnitaire;
        this.totalLigne = totalLigne;
    }

    public Long getId() { return id; }
    public Article getArticle() { return article; }
    public StockLot getLot() { return lot; }
    public String getDesignation() { return designation; }
    public String getUnite() { return unite; }
    public BigDecimal getQuantite() { return quantite; }
    public BigDecimal getQuantiteStock() { return quantiteStock == null ? quantite : quantiteStock; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public BigDecimal getTotalLigne() { return totalLigne; }
}
