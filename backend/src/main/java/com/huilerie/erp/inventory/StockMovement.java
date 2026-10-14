package com.huilerie.erp.inventory;

import com.huilerie.erp.catalog.Article;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** Mouvement de stock : écrit une fois, jamais modifié (trigger MySQL en plus). */
@Entity
@Immutable
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private MovementType type;

    @Column(nullable = false)
    private int sens;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    private String motif;

    @Column(name = "document_type", length = 30)
    private String documentType;

    @Column(name = "document_id")
    private Long documentId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(Article article, StockLot lot, MovementType type, BigDecimal quantite, String motif,
                         String documentType, Long documentId, String username) {
        this.article = article;
        this.lot = lot;
        this.type = type;
        this.sens = type.sens();
        this.quantite = quantite;
        this.motif = motif;
        this.documentType = documentType;
        this.documentId = documentId;
        this.username = username;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Article getArticle() { return article; }
    public StockLot getLot() { return lot; }
    public MovementType getType() { return type; }
    public int getSens() { return sens; }
    public BigDecimal getQuantite() { return quantite; }
    public String getMotif() { return motif; }
    public String getDocumentType() { return documentType; }
    public Long getDocumentId() { return documentId; }
    public String getUsername() { return username; }
    public Instant getCreatedAt() { return createdAt; }
}
