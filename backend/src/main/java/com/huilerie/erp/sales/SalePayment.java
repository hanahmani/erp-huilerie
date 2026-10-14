package com.huilerie.erp.sales;

import com.huilerie.erp.finance.PaymentMode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** Montant réellement affecté à la vente (monnaie rendue déjà déduite). */
@Entity
@Table(name = "sale_payments")
public class SalePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal montant;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SalePayment() {
    }

    SalePayment(Sale sale, PaymentMode mode, BigDecimal montant) {
        this.sale = sale;
        this.mode = mode;
        this.montant = montant;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public PaymentMode getMode() { return mode; }
    public BigDecimal getMontant() { return montant; }
    public Instant getCreatedAt() { return createdAt; }
}
