package com.huilerie.erp.inventory;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.clients.Client;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "stock_lots")
public class StockLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 50)
    private String numero;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LotOwner proprietaire;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private LotStatus statut = LotStatus.DISPONIBLE;

    @Column(name = "motif_blocage")
    private String motifBlocage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockLot() {
    }

    public StockLot(Article article, String numero, LotOwner proprietaire, Client client) {
        this.article = article;
        this.numero = numero;
        this.proprietaire = proprietaire;
        this.client = client;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /** Vendable seulement si l'huile appartient à l'entreprise et que le lot n'est pas bloqué. */
    public boolean isSellable() {
        return proprietaire == LotOwner.ENTREPRISE && statut == LotStatus.DISPONIBLE;
    }

    public void block(String motif) {
        statut = LotStatus.BLOQUE;
        motifBlocage = motif;
    }

    public void unblock() {
        statut = LotStatus.DISPONIBLE;
        motifBlocage = null;
    }

    public Long getId() { return id; }
    public Article getArticle() { return article; }
    public String getNumero() { return numero; }
    public LotOwner getProprietaire() { return proprietaire; }
    public Client getClient() { return client; }
    public LotStatus getStatut() { return statut; }
    public String getMotifBlocage() { return motifBlocage; }
    public Instant getCreatedAt() { return createdAt; }
}
