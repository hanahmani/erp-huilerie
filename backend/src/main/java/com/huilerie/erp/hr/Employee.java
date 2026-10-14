package com.huilerie.erp.hr;

import com.huilerie.erp.users.AppUser;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Dossier employé. Données personnelles : accès réservé aux permissions RH. */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 20)
    private String cin;

    @Column(length = 50)
    private String telephone;

    @Column(length = 120)
    private String email;

    private String adresse;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private HrRef.Service service;

    @ManyToOne
    @JoinColumn(name = "poste_id")
    private HrRef.Position poste;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Column(name = "type_contrat", nullable = false, length = 20)
    private String typeContrat;

    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    @Column(nullable = false, length = 20)
    private String statut = "ACTIF";

    @Column(name = "solde_conges", nullable = false, precision = 5, scale = 1)
    private BigDecimal soldeConges = BigDecimal.ZERO.setScale(1);

    @OneToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

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

    void debitLeave(BigDecimal days) {
        soldeConges = soldeConges.subtract(days);
    }

    public Long getId() { return id; }
    public String getMatricule() { return matricule; }
    public void setMatricule(String v) { matricule = v; }
    public String getNom() { return nom; }
    public void setNom(String v) { nom = v; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String v) { prenom = v; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate v) { dateNaissance = v; }
    public String getCin() { return cin; }
    public void setCin(String v) { cin = v; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { telephone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { adresse = v; }
    public HrRef.Service getService() { return service; }
    public void setService(HrRef.Service v) { service = v; }
    public HrRef.Position getPoste() { return poste; }
    public void setPoste(HrRef.Position v) { poste = v; }
    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate v) { dateEmbauche = v; }
    public String getTypeContrat() { return typeContrat; }
    public void setTypeContrat(String v) { typeContrat = v; }
    public LocalDate getDateFinContrat() { return dateFinContrat; }
    public void setDateFinContrat(LocalDate v) { dateFinContrat = v; }
    public String getStatut() { return statut; }
    public void setStatut(String v) { statut = v; }
    public BigDecimal getSoldeConges() { return soldeConges; }
    public void setSoldeConges(BigDecimal v) { soldeConges = v; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser v) { user = v; }
    public String fullName() { return prenom + " " + nom; }
}
