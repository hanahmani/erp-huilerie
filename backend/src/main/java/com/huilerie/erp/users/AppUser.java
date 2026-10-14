package com.huilerie.erp.users;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/** Utilisateur de l'ERP. Le mot de passe n'est jamais stocké en clair (hachage BCrypt). */
@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "prenom", nullable = false, length = 100)
    private String firstName;

    @Column(name = "nom", nullable = false, length = 100)
    private String lastName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "actif", nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /** Incrémenté à chaque changement de mot de passe ou désactivation : invalide les anciens jetons. */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(length = 120)
    private String email;

    @Column(length = 50)
    private String telephone;

    @Column(length = 100)
    private String service;

    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String username, String firstName, String lastName, String passwordHash,
                   Set<Role> roles, boolean mustChangePassword) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>(roles);
        this.mustChangePassword = mustChangePassword;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
        this.mustChangePassword = false;
        this.tokenVersion++;
    }

    public void setActive(boolean active) {
        if (this.active && !active) {
            this.tokenVersion++;
        }
        this.active = active;
    }

    /** Réinitialisation par un administrateur : mot de passe provisoire, sessions coupées. */
    public void resetPassword(String newHash) {
        this.passwordHash = newHash;
        this.mustChangePassword = true;
        this.tokenVersion++;
    }

    public void updateProfile(String firstName, String lastName, String email, String telephone, String service) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.telephone = telephone;
        this.service = service;
    }

    public void replaceRoles(Set<Role> newRoles) {
        this.roles.clear();
        this.roles.addAll(newRoles);
        this.tokenVersion++;
    }

    public void markLogin() {
        this.derniereConnexion = Instant.now();
    }

    public boolean hasRole(String roleCode) {
        return roles.stream().anyMatch(r -> r.getCode().equals(roleCode));
    }

    public Set<String> roleCodes() {
        Set<String> codes = new TreeSet<>();
        roles.forEach(r -> codes.add(r.getCode()));
        return codes;
    }

    public Set<String> permissionCodes() {
        Set<String> codes = new TreeSet<>();
        roles.forEach(r -> r.getPermissions().forEach(p -> codes.add(p.getCode())));
        return codes;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public int getTokenVersion() { return tokenVersion; }
    public Set<Role> getRoles() { return roles; }
    public Instant getCreatedAt() { return createdAt; }
    public String getEmail() { return email; }
    public String getTelephone() { return telephone; }
    public String getService() { return service; }
    public Instant getDerniereConnexion() { return derniereConnexion; }
}
