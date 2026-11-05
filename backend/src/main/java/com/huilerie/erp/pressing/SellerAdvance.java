package com.huilerie.erp.pressing;

import com.huilerie.erp.clients.Client;
import com.huilerie.erp.finance.PaymentMode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Avance payée à un vendeur d'olives avant ses achats (sortie de caisse au versement).
 * Elle est déduite automatiquement de ses achats suivants : montantImpute en garde la part déjà utilisée.
 */
@Entity
@Table(name = "seller_advances")
public class SellerAdvance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30, updatable = false)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private PaymentMode mode;

    @Column(nullable = false, precision = 12, scale = 3, updatable = false)
    private BigDecimal montant;

    @Column(name = "montant_impute", nullable = false, precision = 12, scale = 3)
    private BigDecimal montantImpute = BigDecimal.ZERO.setScale(3);

    @Column(length = 80, updatable = false)
    private String reference;

    @Column(name = "date_avance", nullable = false, updatable = false)
    private Instant date;

    @Column(nullable = false, length = 64, updatable = false)
    private String username;

    protected SellerAdvance() {
    }

    SellerAdvance(String numero, Client client, PaymentMode mode, BigDecimal montant, String reference, String username) {
        this.numero = numero;
        this.client = client;
        this.mode = mode;
        this.montant = montant;
        this.reference = reference;
        this.username = username;
        this.date = Instant.now();
    }

    /** Part de l'avance pas encore déduite d'un achat. */
    public BigDecimal reste() {
        return montant.subtract(montantImpute);
    }

    void impute(BigDecimal amount) {
        montantImpute = montantImpute.add(amount);
    }

    /** Achat annulé : la part déduite redevient disponible. */
    void release(BigDecimal amount) {
        montantImpute = montantImpute.subtract(amount);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Client getClient() { return client; }
    public PaymentMode getMode() { return mode; }
    public BigDecimal getMontant() { return montant; }
    public BigDecimal getMontantImpute() { return montantImpute; }
    public String getReference() { return reference; }
    public Instant getDate() { return date; }
    public String getUsername() { return username; }
}
