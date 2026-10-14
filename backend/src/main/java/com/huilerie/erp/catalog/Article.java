package com.huilerie.erp.catalog;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ArticleType type;

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private Category categorie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unite_id", nullable = false)
    private Unit unite;

    @Column(precision = 12, scale = 3)
    private BigDecimal contenance;

    @Column(name = "prix_achat", precision = 12, scale = 3)
    private BigDecimal prixAchat;

    @Column(name = "prix_vente", precision = 12, scale = 3)
    private BigDecimal prixVente;

    @Column(name = "seuil_min", precision = 15, scale = 3)
    private BigDecimal seuilMin;

    @Column(name = "code_barres", unique = true, length = 64)
    private String codeBarres;

    @Column(nullable = false)
    private boolean vendable;

    @Column(name = "suivi_lot", nullable = false)
    private boolean suiviLot;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String label() {
        return nom;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public void setReference(String v) { reference = v; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public ArticleType getType() { return type; }
    public void setType(ArticleType v) { type = v; }
    public Category getCategorie() { return categorie; }
    public void setCategorie(Category v) { categorie = v; }
    public Unit getUnite() { return unite; }
    public void setUnite(Unit v) { unite = v; }
    public BigDecimal getContenance() { return contenance; }
    public void setContenance(BigDecimal v) { contenance = v; }
    public BigDecimal getPrixAchat() { return prixAchat; }
    public void setPrixAchat(BigDecimal v) { prixAchat = v; }
    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal v) { prixVente = v; }
    public BigDecimal getSeuilMin() { return seuilMin; }
    public void setSeuilMin(BigDecimal v) { seuilMin = v; }
    public String getCodeBarres() { return codeBarres; }
    public void setCodeBarres(String v) { codeBarres = v; }
    public boolean isVendable() { return vendable; }
    public void setVendable(boolean v) { vendable = v; }
    public boolean isSuiviLot() { return suiviLot; }
    public void setSuiviLot(boolean v) { suiviLot = v; }
    public boolean isActif() { return actif; }
    public void setActif(boolean v) { actif = v; }
    public Instant getCreatedAt() { return createdAt; }
}
