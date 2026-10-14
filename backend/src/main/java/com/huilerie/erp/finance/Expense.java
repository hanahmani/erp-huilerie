package com.huilerie.erp.finance;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Dépense payée (électricité, carburant, main-d'œuvre…). Annulable par contre-passation tracée. */
@Entity
@Table(name = "expenses")
public class Expense {

    public static final String VALIDEE = "VALIDEE";
    public static final String ANNULEE = "ANNULEE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private ExpenseCategory categorie;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    @Column(length = 80)
    private String reference;

    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense;

    @Column(nullable = false, length = 20)
    private String statut = VALIDEE;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    protected Expense() {
    }

    public Expense(String numero, ExpenseCategory categorie, String libelle, BigDecimal montant, PaymentMode mode,
                   String reference, LocalDate dateDepense, String username) {
        this.numero = numero;
        this.categorie = categorie;
        this.libelle = libelle;
        this.montant = montant;
        this.mode = mode;
        this.reference = reference;
        this.dateDepense = dateDepense;
        this.username = username;
        this.createdAt = Instant.now();
    }

    public void cancel(String motif) {
        statut = ANNULEE;
        motifAnnulation = motif;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public ExpenseCategory getCategorie() { return categorie; }
    public String getLibelle() { return libelle; }
    public BigDecimal getMontant() { return montant; }
    public PaymentMode getMode() { return mode; }
    public String getReference() { return reference; }
    public LocalDate getDateDepense() { return dateDepense; }
    public String getStatut() { return statut; }
    public String getUsername() { return username; }
    public String getMotifAnnulation() { return motifAnnulation; }
}
