package com.huilerie.erp.sales;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.users.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Vente validée. Ses montants ne changent plus ; seule une annulation tracée est possible. */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ticket", nullable = false, unique = true, length = 30)
    private String numeroTicket;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "caissier_id", nullable = false)
    private AppUser caissier;

    @Column(name = "date_vente", nullable = false)
    private Instant dateVente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private SaleStatus statut = SaleStatus.VALIDEE;

    @Column(name = "sous_total", nullable = false, precision = 12, scale = 3)
    private BigDecimal sousTotal;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal remise;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal total;

    @Column(name = "montant_recu", nullable = false, precision = 12, scale = 3)
    private BigDecimal montantRecu;

    @Column(name = "montant_paye", nullable = false, precision = 12, scale = 3)
    private BigDecimal montantPaye;

    @Column(name = "reste_a_payer", nullable = false, precision = 12, scale = 3)
    private BigDecimal resteAPayer;

    @Column(name = "monnaie_rendue", nullable = false, precision = 12, scale = 3)
    private BigDecimal monnaieRendue;

    @Column(name = "nb_impressions", nullable = false)
    private int nbImpressions;

    @Column(name = "annulee_par", length = 64)
    private String annuleePar;

    @Column(name = "annulee_le")
    private Instant annuleeLe;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<SaleLine> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<SalePayment> paiements = new ArrayList<>();

    protected Sale() {
    }

    Sale(String numeroTicket, String idempotencyKey, Client client, AppUser caissier) {
        this.numeroTicket = numeroTicket;
        this.idempotencyKey = idempotencyKey;
        this.client = client;
        this.caissier = caissier;
        this.dateVente = Instant.now();
    }

    void addLine(Article article, StockLot lot, BigDecimal qty, BigDecimal price, BigDecimal lineTotal) {
        lignes.add(new SaleLine(this, article, lot, qty, price, lineTotal));
    }

    void addPayment(PaymentMode mode, BigDecimal amount) {
        paiements.add(new SalePayment(this, mode, amount));
    }

    void setTotals(BigDecimal sousTotal, BigDecimal remise, BigDecimal total, BigDecimal recu, BigDecimal paye,
                   BigDecimal reste, BigDecimal monnaie) {
        this.sousTotal = sousTotal;
        this.remise = remise;
        this.total = total;
        this.montantRecu = recu;
        this.montantPaye = paye;
        this.resteAPayer = reste;
        this.monnaieRendue = monnaie;
    }

    void cancel(String username, String motif) {
        this.statut = SaleStatus.ANNULEE;
        this.annuleePar = username;
        this.annuleeLe = Instant.now();
        this.motifAnnulation = motif;
    }

    /** Règlement d'un reste dû : le total de la vente ne change jamais. */
    void applySettlement(BigDecimal amount) {
        this.montantPaye = this.montantPaye.add(amount);
        this.resteAPayer = this.resteAPayer.subtract(amount);
    }

    void markPrinted() {
        nbImpressions++;
    }

    public Long getId() { return id; }
    public String getNumeroTicket() { return numeroTicket; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Client getClient() { return client; }
    public AppUser getCaissier() { return caissier; }
    public Instant getDateVente() { return dateVente; }
    public SaleStatus getStatut() { return statut; }
    public BigDecimal getSousTotal() { return sousTotal; }
    public BigDecimal getRemise() { return remise; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getMontantRecu() { return montantRecu; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public BigDecimal getResteAPayer() { return resteAPayer; }
    public BigDecimal getMonnaieRendue() { return monnaieRendue; }
    public int getNbImpressions() { return nbImpressions; }
    public String getAnnuleePar() { return annuleePar; }
    public Instant getAnnuleeLe() { return annuleeLe; }
    public String getMotifAnnulation() { return motifAnnulation; }
    public List<SaleLine> getLignes() { return lignes; }
    public List<SalePayment> getPaiements() { return paiements; }
}
