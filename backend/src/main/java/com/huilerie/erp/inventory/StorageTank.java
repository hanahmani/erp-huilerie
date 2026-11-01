package com.huilerie.erp.inventory;

import com.huilerie.erp.catalog.Article;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Puits, silo ou cuve de stockage d'huile. Son contenu est un lot ENTREPRISE dont le numéro est le code du puits :
 * l'huile y est mélangée, se vend en caisse sous ce lot, et ne peut pas dépasser la capacité.
 */
@Entity
@Table(name = "storage_tanks")
public class StorageTank {

    public enum Type { PUITS, SILO, CUVE, AUTRE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String nom;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Type type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false, updatable = false)
    private Article article;

    @OneToOne(optional = false)
    @JoinColumn(name = "lot_id", nullable = false, unique = true, updatable = false)
    private StockLot lot;

    /** Capacité dans l'unité de l'article (litres en général). */
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal capacite;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StorageTank() {
    }

    StorageTank(String code, String nom, Type type, Article article, StockLot lot, BigDecimal capacite) {
        this.code = code;
        this.nom = nom;
        this.type = type;
        this.article = article;
        this.lot = lot;
        this.capacite = capacite;
        this.createdAt = Instant.now();
    }

    void update(String nom, Type type, BigDecimal capacite) {
        this.nom = nom;
        this.type = type;
        this.capacite = capacite;
    }

    void setActif(boolean actif) {
        this.actif = actif;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNom() { return nom; }
    public Type getType() { return type; }
    public Article getArticle() { return article; }
    public StockLot getLot() { return lot; }
    public BigDecimal getCapacite() { return capacite; }
    public boolean isActif() { return actif; }
    public Instant getCreatedAt() { return createdAt; }
}
