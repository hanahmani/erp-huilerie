package com.huilerie.erp.finance;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/** Journal de caisse : chaque encaissement ou décaissement réel, jamais modifié. */
@Entity
@Immutable
@Table(name = "cash_movements")
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CashMovementType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal montant;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CashMovement() {
    }

    public CashMovement(CashMovementType type, PaymentMode mode, BigDecimal montant, String libelle,
                        String referenceType, Long referenceId, String username) {
        this.type = type;
        this.mode = mode;
        this.montant = montant;
        this.libelle = libelle;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.username = username;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public CashMovementType getType() { return type; }
    public PaymentMode getMode() { return mode; }
    public BigDecimal getMontant() { return montant; }
    public String getLibelle() { return libelle; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
    public String getUsername() { return username; }
    public Instant getCreatedAt() { return createdAt; }
}
