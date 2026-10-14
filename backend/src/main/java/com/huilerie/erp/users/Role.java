package com.huilerie.erp.users;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role {

    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "nom", nullable = false, length = 100)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean systeme;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    protected Role() {
    }

    /** Utilisé uniquement pour créer des rôles techniques (ex. dans les tests). */
    public Role(String code, String name, String description, Set<Permission> permissions) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>(permissions);
    }

    public void update(String name, String description, Set<Permission> perms) {
        this.name = name;
        this.description = description;
        this.permissions.clear();
        this.permissions.addAll(perms);
    }

    public boolean isSysteme() { return systeme; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<Permission> getPermissions() { return permissions; }
}
