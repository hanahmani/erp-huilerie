package com.huilerie.erp.pressing;

import com.huilerie.erp.clients.Client;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Remise d'huile à un client sur l'ensemble de ses pressages à façon (bon RMH-…).
 * La quantité est en kg ; litres est renseigné quand la remise a été saisie en litres.
 * La part prise sur chaque pressage est dans {@link Pressing.Return}.
 */
@Entity
@Immutable
@Table(name = "client_oil_returns")
public class ClientOilReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    @Column(precision = 15, scale = 3)
    private BigDecimal litres;

    @Column(name = "remis_a", nullable = false, length = 150)
    private String remisA;

    @Column(name = "date_remise", nullable = false)
    private Instant date;

    @Column(nullable = false, length = 64)
    private String username;

    protected ClientOilReturn() {
    }

    ClientOilReturn(String numero, Client client, BigDecimal quantite, BigDecimal litres, String remisA, String username) {
        this.numero = numero;
        this.client = client;
        this.quantite = quantite;
        this.litres = litres;
        this.remisA = remisA;
        this.username = username;
        this.date = Instant.now();
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Client getClient() { return client; }
    public BigDecimal getQuantite() { return quantite; }
    public BigDecimal getLitres() { return litres; }
    public String getRemisA() { return remisA; }
    public Instant getDate() { return date; }
    public String getUsername() { return username; }
}
