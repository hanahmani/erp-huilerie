package com.huilerie.erp.common.numbering;

import jakarta.persistence.*;

@Entity
@Table(name = "document_sequences")
public class DocumentSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false)
    private int annee;

    @Column(nullable = false)
    private long prochain;

    protected DocumentSequence() {
    }

    public DocumentSequence(String type, int annee) {
        this.type = type;
        this.annee = annee;
        this.prochain = 1;
    }

    long take() {
        return prochain++;
    }
}
