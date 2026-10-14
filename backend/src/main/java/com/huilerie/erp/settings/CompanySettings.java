package com.huilerie.erp.settings;

import jakarta.persistence.*;

import java.time.Instant;

/** Informations légales de l'huilerie, saisies par l'administrateur (ligne unique id = 1). */
@Entity
@Table(name = "company_settings")
public class CompanySettings {

    public static final long SINGLE_ID = 1L;

    @Id
    private Long id = SINGLE_ID;

    @Column(name = "raison_sociale", nullable = false, length = 150)
    private String raisonSociale;

    private String adresse;

    @Column(length = 50)
    private String telephone;

    @Column(length = 120)
    private String email;

    @Column(name = "matricule_fiscal", length = 50)
    private String matriculeFiscal;

    @Column(name = "message_ticket")
    private String messageTicket;

    @Column(name = "vente_credit_autorisee", nullable = false)
    private boolean venteCreditAutorisee;

    @Column(name = "tarif_pressage_kg", precision = 12, scale = 3)
    private java.math.BigDecimal tarifPressageKg;

    @ManyToOne
    @JoinColumn(name = "pressage_article_id")
    private com.huilerie.erp.catalog.Article pressageArticle;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String v) { raisonSociale = v; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { adresse = v; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { telephone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getMatriculeFiscal() { return matriculeFiscal; }
    public void setMatriculeFiscal(String v) { matriculeFiscal = v; }
    public String getMessageTicket() { return messageTicket; }
    public void setMessageTicket(String v) { messageTicket = v; }
    public boolean isVenteCreditAutorisee() { return venteCreditAutorisee; }
    public void setVenteCreditAutorisee(boolean v) { venteCreditAutorisee = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public java.math.BigDecimal getTarifPressageKg() { return tarifPressageKg; }
    public void setTarifPressageKg(java.math.BigDecimal v) { tarifPressageKg = v; }
    public com.huilerie.erp.catalog.Article getPressageArticle() { return pressageArticle; }
    public void setPressageArticle(com.huilerie.erp.catalog.Article v) { pressageArticle = v; }
}
