package com.huilerie.erp.clients;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ClientType type;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 50)
    private String telephone;

    private String adresse;

    @Column(length = 20)
    private String cin;

    @Column(name = "matricule_fiscal", length = 50)
    private String matriculeFiscal;

    @Column(length = 120)
    private String email;

    @Column(name = "conditions_paiement", length = 100)
    private String conditionsPaiement;

    @Column(name = "plafond_credit", precision = 12, scale = 3)
    private BigDecimal plafondCredit;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public ClientType getType() { return type; }
    public void setType(ClientType v) { type = v; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { telephone = v; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { adresse = v; }
    public String getCin() { return cin; }
    public void setCin(String v) { cin = v; }
    public String getMatriculeFiscal() { return matriculeFiscal; }
    public void setMatriculeFiscal(String v) { matriculeFiscal = v; }
    public BigDecimal getPlafondCredit() { return plafondCredit; }
    public void setPlafondCredit(BigDecimal v) { plafondCredit = v; }
    public boolean isActif() { return actif; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getConditionsPaiement() { return conditionsPaiement; }
    public void setConditionsPaiement(String v) { conditionsPaiement = v; }
    public void setActif(boolean v) { actif = v; }
    public Instant getCreatedAt() { return createdAt; }
}
