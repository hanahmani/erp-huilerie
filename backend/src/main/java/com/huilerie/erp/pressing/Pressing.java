package com.huilerie.erp.pressing;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StockLot;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pressage à façon : le client apporte ses olives, l'huilerie les presse et lui remet son huile.
 * Cycle : RECU → EN_COURS → TERMINE → REMIS ; ANNULE possible avant la fin du pressage.
 * L'huile obtenue est placée dans un lot dont le propriétaire est le client : elle n'est jamais vendable.
 */
@Entity
@Table(name = "pressings")
public class Pressing {

    public enum Status { RECU, EN_COURS, TERMINE, REMIS, ANNULE }

    public enum OliveState { SAINES, TOMBEES, ABIMEES }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status statut = Status.RECU;

    @Column(name = "date_reception", nullable = false)
    private Instant dateReception;

    @Column(length = 80)
    private String variete;

    @Column(length = 120)
    private String provenance;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "etat_olives", nullable = false, length = 20)
    private OliveState etatOlives;

    @Column(name = "poids_brut", nullable = false, precision = 15, scale = 3)
    private BigDecimal poidsBrut;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal tare;

    @Column(name = "poids_net", nullable = false, precision = 15, scale = 3)
    private BigDecimal poidsNet;

    @Column(name = "tarif_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal tarifKg;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal montant;

    @Column(name = "montant_paye", nullable = false, precision = 12, scale = 3)
    private BigDecimal montantPaye = BigDecimal.ZERO.setScale(3);

    @Column(name = "huile_obtenue", precision = 15, scale = 3)
    private BigDecimal huileObtenue;

    @Column(name = "huile_remise", nullable = false, precision = 15, scale = 3)
    private BigDecimal huileRemise = BigDecimal.ZERO.setScale(3);

    @Column(name = "pertes_kg", precision = 15, scale = 3)
    private BigDecimal pertesKg;

    @Column(name = "date_debut")
    private Instant dateDebut;

    @Column(name = "date_fin")
    private Instant dateFin;

    private String notes;

    @Column(name = "recu_par", nullable = false, length = 64)
    private String recuPar;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @OneToMany(mappedBy = "pressing", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Return> remises = new ArrayList<>();

    @OneToMany(mappedBy = "pressing", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Payment> paiements = new ArrayList<>();

    protected Pressing() {
    }

    Pressing(String numero, Client client, Article article, String variete, String provenance, OliveState etat,
             BigDecimal brut, BigDecimal tare, BigDecimal tarifKg, BigDecimal montant, String notes, String recuPar) {
        this.numero = numero;
        this.client = client;
        this.article = article;
        this.variete = variete;
        this.provenance = provenance;
        this.etatOlives = etat;
        this.poidsBrut = brut;
        this.tare = tare;
        this.poidsNet = brut.subtract(tare);
        this.tarifKg = tarifKg;
        this.montant = montant;
        this.notes = notes;
        this.recuPar = recuPar;
        this.dateReception = Instant.now();
    }

    void start() {
        statut = Status.EN_COURS;
        dateDebut = Instant.now();
    }

    void finish(StockLot lot, BigDecimal huile, BigDecimal pertes) {
        this.lot = lot;
        this.huileObtenue = huile;
        this.pertesKg = pertes;
        this.statut = Status.TERMINE;
        this.dateFin = Instant.now();
    }

    Return addReturn(String numero, BigDecimal q, String remisA, String user) {
        Return r = new Return(this, numero, q, remisA, user);
        remises.add(r);
        huileRemise = huileRemise.add(q);
        if (huileRemise.compareTo(huileObtenue) == 0) {
            statut = Status.REMIS;
        }
        return r;
    }

    Payment addPayment(String numero, PaymentMode mode, BigDecimal amount, String reference, String user) {
        Payment p = new Payment(this, numero, mode, amount, reference, user);
        paiements.add(p);
        montantPaye = montantPaye.add(amount);
        return p;
    }

    void cancel(String motif) {
        statut = Status.ANNULE;
        motifAnnulation = motif;
    }

    public BigDecimal reste() {
        return montant.subtract(montantPaye);
    }

    public BigDecimal huileARemettre() {
        return huileObtenue == null ? BigDecimal.ZERO.setScale(3) : huileObtenue.subtract(huileRemise);
    }

    /** Litres d'huile pour 100 kg d'olives (indicateur, pas une garantie). */
    public BigDecimal rendement() {
        if (huileObtenue == null || poidsNet.signum() == 0) {
            return null;
        }
        return huileObtenue.multiply(BigDecimal.valueOf(100)).divide(poidsNet, 2, java.math.RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Client getClient() { return client; }
    public Article getArticle() { return article; }
    public StockLot getLot() { return lot; }
    public Status getStatut() { return statut; }
    public Instant getDateReception() { return dateReception; }
    public String getVariete() { return variete; }
    public String getProvenance() { return provenance; }
    public OliveState getEtatOlives() { return etatOlives; }
    public BigDecimal getPoidsBrut() { return poidsBrut; }
    public BigDecimal getTare() { return tare; }
    public BigDecimal getPoidsNet() { return poidsNet; }
    public BigDecimal getTarifKg() { return tarifKg; }
    public BigDecimal getMontant() { return montant; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public BigDecimal getHuileObtenue() { return huileObtenue; }
    public BigDecimal getHuileRemise() { return huileRemise; }
    public BigDecimal getPertesKg() { return pertesKg; }
    public Instant getDateDebut() { return dateDebut; }
    public Instant getDateFin() { return dateFin; }
    public String getNotes() { return notes; }
    public String getRecuPar() { return recuPar; }
    public String getMotifAnnulation() { return motifAnnulation; }
    public List<Return> getRemises() { return remises; }
    public List<Payment> getPaiements() { return paiements; }

    @Entity(name = "PressingReturn")
    @Immutable
    @Table(name = "pressing_returns")
    public static class Return {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "pressing_id", nullable = false)
        private Pressing pressing;

        @Column(nullable = false, unique = true, length = 30)
        private String numero;

        @Column(nullable = false, precision = 15, scale = 3)
        private BigDecimal quantite;

        @Column(name = "remis_a", nullable = false, length = 150)
        private String remisA;

        @Column(name = "date_remise", nullable = false)
        private Instant date;

        @Column(nullable = false, length = 64)
        private String username;

        protected Return() {
        }

        Return(Pressing pressing, String numero, BigDecimal quantite, String remisA, String username) {
            this.pressing = pressing;
            this.numero = numero;
            this.quantite = quantite;
            this.remisA = remisA;
            this.username = username;
            this.date = Instant.now();
        }

        public Long getId() { return id; }
        public String getNumero() { return numero; }
        public BigDecimal getQuantite() { return quantite; }
        public String getRemisA() { return remisA; }
        public Instant getDate() { return date; }
        public String getUsername() { return username; }
    }

    @Entity(name = "PressingPayment")
    @Immutable
    @Table(name = "pressing_payments")
    public static class Payment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "pressing_id", nullable = false)
        private Pressing pressing;

        @Column(nullable = false, unique = true, length = 30)
        private String numero;

        @Enumerated(EnumType.STRING)
        @JdbcTypeCode(SqlTypes.VARCHAR)
        @Column(nullable = false, length = 20)
        private PaymentMode mode;

        @Column(nullable = false, precision = 12, scale = 3)
        private BigDecimal montant;

        @Column(length = 80)
        private String reference;

        @Column(name = "date_paie", nullable = false)
        private Instant date;

        @Column(nullable = false, length = 64)
        private String username;

        protected Payment() {
        }

        Payment(Pressing pressing, String numero, PaymentMode mode, BigDecimal montant, String reference, String username) {
            this.pressing = pressing;
            this.numero = numero;
            this.mode = mode;
            this.montant = montant;
            this.reference = reference;
            this.username = username;
            this.date = Instant.now();
        }

        public String getNumero() { return numero; }
        public PaymentMode getMode() { return mode; }
        public BigDecimal getMontant() { return montant; }
        public String getReference() { return reference; }
        public Instant getDate() { return date; }
        public String getUsername() { return username; }
    }
}
