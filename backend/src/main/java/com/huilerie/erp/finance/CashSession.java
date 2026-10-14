package com.huilerie.erp.finance;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Session de caisse : ouverture avec fond initial, fermeture avec comptage et justification des écarts. */
@Entity
@Table(name = "cash_sessions")
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 1 tant que la session est ouverte, NULL ensuite : l'index unique garantit une seule caisse ouverte. */
    @Column(name = "ouverte_flag")
    private Integer ouverteFlag = 1;

    @Column(name = "ouverte_par", nullable = false, length = 64)
    private String ouvertePar;

    @Column(name = "ouverte_le", nullable = false)
    private Instant ouverteLe;

    @Column(name = "fond_initial", nullable = false, precision = 12, scale = 3)
    private BigDecimal fondInitial;

    @Column(name = "fermee_par", length = 64)
    private String fermeePar;

    @Column(name = "fermee_le")
    private Instant fermeeLe;

    @Column(name = "especes_theoriques", precision = 12, scale = 3)
    private BigDecimal especesTheoriques;

    @Column(name = "especes_comptees", precision = 12, scale = 3)
    private BigDecimal especesComptees;

    @Column(precision = 12, scale = 3)
    private BigDecimal ecart;

    private String justification;

    protected CashSession() {
    }

    public CashSession(String user, BigDecimal fond) {
        this.ouvertePar = user;
        this.fondInitial = fond;
        this.ouverteLe = Instant.now();
    }

    public void close(String user, BigDecimal theoriques, BigDecimal comptees, String justification) {
        this.ouverteFlag = null;
        this.fermeePar = user;
        this.fermeeLe = Instant.now();
        this.especesTheoriques = theoriques;
        this.especesComptees = comptees;
        this.ecart = comptees.subtract(theoriques);
        this.justification = justification;
    }

    public boolean isOpen() { return ouverteFlag != null; }
    public Long getId() { return id; }
    public String getOuvertePar() { return ouvertePar; }
    public Instant getOuverteLe() { return ouverteLe; }
    public BigDecimal getFondInitial() { return fondInitial; }
    public String getFermeePar() { return fermeePar; }
    public Instant getFermeeLe() { return fermeeLe; }
    public BigDecimal getEspecesTheoriques() { return especesTheoriques; }
    public BigDecimal getEspecesComptees() { return especesComptees; }
    public BigDecimal getEcart() { return ecart; }
    public String getJustification() { return justification; }
}
