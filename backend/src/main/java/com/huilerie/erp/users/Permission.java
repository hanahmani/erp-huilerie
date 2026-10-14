package com.huilerie.erp.users;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private String description;

    protected Permission() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
}
