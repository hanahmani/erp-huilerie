package com.huilerie.erp.inventory;

import com.huilerie.erp.catalog.Article;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Projection du stock (somme des mouvements). Modifiée uniquement par StockService. */
@Entity
@Table(name = "stock_balances")
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @Column(name = "lot_ref", nullable = false)
    private long lotRef;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockBalance() {
    }

    public StockBalance(Article article, StockLot lot) {
        this.article = article;
        this.lot = lot;
        this.lotRef = refOf(lot);
        this.quantite = BigDecimal.ZERO.setScale(3);
    }

    public static long refOf(StockLot lot) {
        return lot == null ? 0L : lot.getId();
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    void setQuantite(BigDecimal q) {
        this.quantite = q;
    }

    public Long getId() { return id; }
    public Article getArticle() { return article; }
    public StockLot getLot() { return lot; }
    public BigDecimal getQuantite() { return quantite; }
}
