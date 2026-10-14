package com.huilerie.erp.salesdocs;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Document commercial : devis, commande, bon de livraison, facture ou avoir.
 * Modifiable en brouillon uniquement ; le numéro définitif est attribué à la validation.
 */
@Entity
@Table(name = "sales_documents")
public class SalesDocument {

    public enum Type { DEVIS, COMMANDE, LIVRAISON, FACTURE, AVOIR }

    public enum Status { BROUILLON, VALIDE, TRANSFORME, ANNULE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "source_id")
    private SalesDocument source;

    @Column(name = "date_doc", nullable = false)
    private LocalDate dateDoc;

    private LocalDate echeance;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status statut = Status.BROUILLON;

    @Column(name = "ref_client", length = 60)
    private String refClient;

    @Column(length = 500)
    private String notes;

    @Column(name = "total_ht", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalHt;

    @Column(name = "total_tva", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalTva;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal timbre = BigDecimal.ZERO.setScale(3);

    @Column(name = "total_ttc", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalTtc;

    @Column(name = "montant_paye", nullable = false, precision = 12, scale = 3)
    private BigDecimal montantPaye = BigDecimal.ZERO.setScale(3);

    @Column(name = "cree_par", nullable = false, length = 64)
    private String creePar;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "valide_par", length = 64)
    private String validePar;

    @Column(name = "valide_le")
    private Instant valideLe;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Line> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Payment> paiements = new ArrayList<>();

    protected SalesDocument() {
    }

    SalesDocument(Type type, Client client, SalesDocument source, String user) {
        this.type = type;
        this.client = client;
        this.source = source;
        this.creePar = user;
        this.createdAt = Instant.now();
    }

    void header(Client client, LocalDate date, LocalDate echeance, String refClient, String notes) {
        this.client = client;
        this.dateDoc = date;
        this.echeance = echeance;
        this.refClient = refClient;
        this.notes = notes;
    }

    void clearLines() {
        lignes.clear();
    }

    void addLine(Article a, StockLot lot, BigDecimal q, BigDecimal pu, BigDecimal remise, BigDecimal tva,
                 BigDecimal ht, BigDecimal mtva) {
        lignes.add(new Line(this, a, lot, q, pu, remise, tva, ht, mtva));
    }

    void totals(BigDecimal ht, BigDecimal tva, BigDecimal timbre) {
        this.totalHt = ht;
        this.totalTva = tva;
        this.timbre = timbre;
        this.totalTtc = ht.add(tva).add(timbre);
    }

    void validate(String numero, String user) {
        this.numero = numero;
        this.statut = Status.VALIDE;
        this.validePar = user;
        this.valideLe = Instant.now();
    }

    void markTransformed() {
        statut = Status.TRANSFORME;
    }

    void reopen() {
        statut = Status.VALIDE;
    }

    void cancel(String motif) {
        statut = Status.ANNULE;
        motifAnnulation = motif;
    }

    void addPayment(String numero, PaymentMode mode, BigDecimal amount, String ref, String user) {
        paiements.add(new Payment(this, numero, mode, amount, ref, user));
        montantPaye = montantPaye.add(amount);
    }

    public BigDecimal reste() {
        return totalTtc.subtract(montantPaye);
    }

    public Long getId() { return id; }
    public Type getType() { return type; }
    public String getNumero() { return numero; }
    public Client getClient() { return client; }
    public SalesDocument getSource() { return source; }
    public LocalDate getDateDoc() { return dateDoc; }
    public LocalDate getEcheance() { return echeance; }
    public Status getStatut() { return statut; }
    public String getRefClient() { return refClient; }
    public String getNotes() { return notes; }
    public BigDecimal getTotalHt() { return totalHt; }
    public BigDecimal getTotalTva() { return totalTva; }
    public BigDecimal getTimbre() { return timbre; }
    public BigDecimal getTotalTtc() { return totalTtc; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public String getCreePar() { return creePar; }
    public Instant getCreatedAt() { return createdAt; }
    public String getValidePar() { return validePar; }
    public Instant getValideLe() { return valideLe; }
    public String getMotifAnnulation() { return motifAnnulation; }
    public List<Line> getLignes() { return lignes; }
    public List<Payment> getPaiements() { return paiements; }

    @Entity(name = "SalesDocumentLine")
    @Table(name = "sales_document_lines")
    public static class Line {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @ManyToOne(optional = false)
        @JoinColumn(name = "document_id", nullable = false)
        private SalesDocument document;
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

        protected Line() {
        }

        Line(SalesDocument d, Article a, StockLot lot, BigDecimal q, BigDecimal pu, BigDecimal remise, BigDecimal tva,
             BigDecimal ht, BigDecimal mtva) {
            this.document = d;
            this.article = a;
            this.lot = lot;
            this.designation = a.getNom();
            this.unite = a.getUnite().getCode();
            this.quantite = q;
            this.prixUnitaire = pu;
            this.remisePct = remise;
            this.tauxTva = tva;
            this.totalHt = ht;
            this.totalTva = mtva;
        }

        public Long getId() { return id; }
        public Article getArticle() { return article; }
        public StockLot getLot() { return lot; }
        public String getDesignation() { return designation; }
        public String getUnite() { return unite; }
        public BigDecimal getQuantite() { return quantite; }
        public BigDecimal getPrixUnitaire() { return prixUnitaire; }
        public BigDecimal getRemisePct() { return remisePct; }
        public BigDecimal getTauxTva() { return tauxTva; }
        public BigDecimal getTotalHt() { return totalHt; }
        public BigDecimal getTotalTva() { return totalTva; }
    }

    @Entity(name = "SalesDocumentPayment")
    @Immutable
    @Table(name = "sales_document_payments")
    public static class Payment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @ManyToOne(optional = false)
        @JoinColumn(name = "document_id", nullable = false)
        private SalesDocument document;
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

        Payment(SalesDocument d, String numero, PaymentMode mode, BigDecimal montant, String reference, String username) {
            this.document = d;
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
