package com.huilerie.erp.quality;

import com.huilerie.erp.inventory.StockLot;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "quality_anomalies")
public class QualityAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @ManyToOne
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @ManyToOne
    @JoinColumn(name = "control_id")
    private QualityControl control;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String gravite;

    @Column(name = "action_corrective", length = 500)
    private String actionCorrective;

    @Column(nullable = false, length = 20)
    private String statut = "OUVERTE";

    @Column(name = "cree_par", nullable = false, length = 64)
    private String creePar;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cloture_le")
    private Instant clotureLe;

    protected QualityAnomaly() {
    }

    QualityAnomaly(String numero, StockLot lot, QualityControl control, String description, String gravite, String user) {
        this.numero = numero;
        this.lot = lot;
        this.control = control;
        this.description = description;
        this.gravite = gravite;
        this.creePar = user;
        this.createdAt = Instant.now();
    }

    void update(String action, String statut) {
        this.actionCorrective = action;
        this.statut = statut;
        this.clotureLe = "CLOTUREE".equals(statut) ? Instant.now() : null;
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public StockLot getLot() { return lot; }
    public QualityControl getControl() { return control; }
    public String getDescription() { return description; }
    public String getGravite() { return gravite; }
    public String getActionCorrective() { return actionCorrective; }
    public String getStatut() { return statut; }
    public String getCreePar() { return creePar; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClotureLe() { return clotureLe; }
}
