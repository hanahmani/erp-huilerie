package com.huilerie.erp.sales;

import com.huilerie.erp.finance.PaymentMode;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** Règlement ultérieur d'une vente à crédit. */
@Entity
@Immutable
@Table(name = "client_payments")
public class ClientPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

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

    protected ClientPayment() {
    }

    ClientPayment(String numero, Sale sale, PaymentMode mode, BigDecimal montant, String reference, String username) {
        this.numero = numero;
        this.sale = sale;
        this.mode = mode;
        this.montant = montant;
        this.reference = reference;
        this.username = username;
        this.date = Instant.now();
    }

    public String getNumero() { return numero; }
    public Sale getSale() { return sale; }
    public PaymentMode getMode() { return mode; }
    public BigDecimal getMontant() { return montant; }
    public String getReference() { return reference; }
    public Instant getDate() { return date; }
    public String getUsername() { return username; }
}
