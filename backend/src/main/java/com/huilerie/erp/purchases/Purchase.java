package com.huilerie.erp.purchases;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.suppliers.Supplier;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Commande fournisseur. Modifiable uniquement en brouillon ; le stock ne bouge qu'à la réception. */
@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Supplier fournisseur;

    @Column(name = "date_commande", nullable = false)
    private LocalDate dateCommande;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private PurchaseStatus statut = PurchaseStatus.BROUILLON;

    @Column(name = "ref_facture", length = 60)
    private String refFacture;

    @Column(length = 500)
    private String notes;

    @Column(name = "total_ht", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalHt;

    @Column(name = "total_tva", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalTva;

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

    @Column(name = "annule_par", length = 64)
    private String annulePar;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PurchaseLine> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "purchase")
    @OrderBy("id")
    private List<GoodsReceipt> receptions = new ArrayList<>();

    @OneToMany(mappedBy = "purchase")
    @OrderBy("id")
    private List<SupplierPayment> paiements = new ArrayList<>();

    protected Purchase() {
    }

    Purchase(String numero, String creePar) {
        this.numero = numero;
        this.creePar = creePar;
        this.createdAt = Instant.now();
    }

    void setHeader(Supplier fournisseur, LocalDate date, String refFacture, String notes) {
        this.fournisseur = fournisseur;
        this.dateCommande = date;
        this.refFacture = refFacture;
        this.notes = notes;
    }

    void clearLines() {
        lignes.clear();
    }

    void addLine(Article a, BigDecimal q, BigDecimal pu, BigDecimal remise, BigDecimal tva, BigDecimal ht, BigDecimal mtva) {
        lignes.add(new PurchaseLine(this, a, q, pu, remise, tva, ht, mtva));
    }

    void setTotals(BigDecimal ht, BigDecimal tva) {
        this.totalHt = ht;
        this.totalTva = tva;
        this.totalTtc = ht.add(tva);
    }

    void validate(String user) {
        statut = PurchaseStatus.VALIDEE;
        validePar = user;
        valideLe = Instant.now();
    }

    void cancel(String user, String motif) {
        statut = PurchaseStatus.ANNULEE;
        annulePar = user;
        motifAnnulation = motif;
    }

    void refreshReceptionStatus() {
        boolean all = lignes.stream().allMatch(l -> l.remaining().signum() == 0);
        boolean any = lignes.stream().anyMatch(l -> l.getQuantiteRecue().signum() > 0);
        statut = all ? PurchaseStatus.RECUE : any ? PurchaseStatus.PARTIELLEMENT_RECUE : PurchaseStatus.VALIDEE;
    }

    void addPaid(BigDecimal amount) {
        montantPaye = montantPaye.add(amount);
    }

    void setRefFacture(String ref) {
        refFacture = ref;
    }

    public BigDecimal reste() {
        return totalTtc.subtract(montantPaye);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Supplier getFournisseur() { return fournisseur; }
    public LocalDate getDateCommande() { return dateCommande; }
    public PurchaseStatus getStatut() { return statut; }
    public String getRefFacture() { return refFacture; }
    public String getNotes() { return notes; }
    public BigDecimal getTotalHt() { return totalHt; }
    public BigDecimal getTotalTva() { return totalTva; }
    public BigDecimal getTotalTtc() { return totalTtc; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public String getCreePar() { return creePar; }
    public Instant getCreatedAt() { return createdAt; }
    public String getValidePar() { return validePar; }
    public Instant getValideLe() { return valideLe; }
    public String getAnnulePar() { return annulePar; }
    public String getMotifAnnulation() { return motifAnnulation; }
    public List<PurchaseLine> getLignes() { return lignes; }
    public List<GoodsReceipt> getReceptions() { return receptions; }
    public List<SupplierPayment> getPaiements() { return paiements; }
}
