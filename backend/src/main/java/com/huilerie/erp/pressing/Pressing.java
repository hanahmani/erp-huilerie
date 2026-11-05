package com.huilerie.erp.pressing;

import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.clients.Client;
import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.inventory.StorageTank;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulletin de pressage. Deux opérations :
 * <ul>
 *   <li>SERVICE (pressage à façon) : l'huile est placée dans un lot dont le propriétaire est le client,
 *       jamais vendable ; cycle RECU → EN_COURS → TERMINE → REMIS.</li>
 *   <li>ACHAT : l'huilerie achète les olives au kg ; l'huile obtenue entre dans un puits (lot ENTREPRISE vendable) ;
 *       cycle RECU → EN_COURS → TERMINE ; le vendeur est payé librement jusqu'au montant d'achat.</li>
 * </ul>
 * ANNULE possible avant la fin du pressage, tant que rien n'a été payé.
 */
@Entity
@Table(name = "pressings")
public class Pressing {

    public enum Status { RECU, EN_COURS, TERMINE, REMIS, ANNULE }

    public enum OliveState { SAINES, TOMBEES, ABIMEES }

    /** SERVICE : le client reprend son huile. ACHAT : l'huilerie achète les olives. */
    public enum Mode { SERVICE, ACHAT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    /** Numéro du bulletin de pressage (000001…), attribué à la réception et jamais modifié. */
    @Column(name = "numero_bulletin", nullable = false, unique = true, length = 6, updatable = false)
    private String numeroBulletin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private Mode mode = Mode.SERVICE;

    /** Huile des clients (SERVICE) ou huile du puits (ACHAT, connue à la fin du pressage). */
    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

    /** ACHAT : puits de l'huilerie où l'huile a été versée ; SERVICE : puits du client (facultatif). */
    @ManyToOne
    @JoinColumn(name = "tank_id")
    private StorageTank tank;

    /** ACHAT : prix payé au vendeur par kg d'olives net. */
    @Column(name = "prix_olives_kg", precision = 12, scale = 3)
    private BigDecimal prixOlivesKg;

    /** ACHAT : poids net × prix au kg, dû au vendeur. */
    @Column(name = "montant_achat", precision = 12, scale = 3)
    private BigDecimal montantAchat;

    /** ACHAT : total déjà payé au vendeur. */
    @Column(name = "montant_regle", nullable = false, precision = 12, scale = 3)
    private BigDecimal montantRegle = BigDecimal.ZERO.setScale(3);

    @ManyToOne
    @JoinColumn(name = "lot_id")
    private StockLot lot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status statut = Status.RECU;

    @Column(name = "date_reception", nullable = false)
    private Instant dateReception;

    @Column(length = 80)
    private String variete;

    @Column(length = 120)
    private String provenance;

    /** N° du bon de livraison apporté par le client. */
    @Column(name = "bl_numero", length = 50)
    private String blNumero;

    @Column(length = 120)
    private String transporteur;

    /** Immatriculation du véhicule, en majuscules. */
    @Column(length = 30)
    private String vehicule;

    /** Adresse de provenance (par défaut celle du client). */
    private String adresse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "etat_olives", nullable = false, length = 20)
    private OliveState etatOlives;

    @Column(name = "poids_brut", nullable = false, precision = 15, scale = 3)
    private BigDecimal poidsBrut;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal tare;

    @Column(name = "poids_net", nullable = false, precision = 15, scale = 3)
    private BigDecimal poidsNet;

    @Column(name = "tarif_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal tarifKg;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal montant;

    @Column(name = "montant_paye", nullable = false, precision = 12, scale = 3)
    private BigDecimal montantPaye = BigDecimal.ZERO.setScale(3);

    @Column(name = "huile_obtenue", precision = 15, scale = 3)
    private BigDecimal huileObtenue;

    @Column(name = "huile_remise", nullable = false, precision = 15, scale = 3)
    private BigDecimal huileRemise = BigDecimal.ZERO.setScale(3);

    @Column(name = "pertes_kg", precision = 15, scale = 3)
    private BigDecimal pertesKg;

    @Column(name = "date_debut")
    private Instant dateDebut;

    @Column(name = "date_fin")
    private Instant dateFin;

    private String notes;

    @Column(name = "recu_par", nullable = false, length = 64)
    private String recuPar;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @OneToMany(mappedBy = "pressing", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Return> remises = new ArrayList<>();

    @OneToMany(mappedBy = "pressing", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Payment> paiements = new ArrayList<>();

    @OneToMany(mappedBy = "pressing", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<SellerPayment> reglementsVendeur = new ArrayList<>();

    protected Pressing() {
    }

    Pressing(String numero, String numeroBulletin, Client client, Article article, String variete, String provenance,
             OliveState etat, BigDecimal brut, BigDecimal tare, BigDecimal tarifKg, BigDecimal montant, String notes,
             String recuPar) {
        this.numero = numero;
        this.numeroBulletin = numeroBulletin;
        this.client = client;
        this.article = article;
        this.variete = variete;
        this.provenance = provenance;
        this.etatOlives = etat;
        this.poidsBrut = brut;
        this.tare = tare;
        this.poidsNet = brut.subtract(tare);
        this.tarifKg = tarifKg;
        this.montant = montant;
        this.notes = notes;
        this.recuPar = recuPar;
        this.dateReception = Instant.now();
    }

    /** Achat d'olives : pas de frais de pressage, montant dû au vendeur. */
    void markPurchase(BigDecimal prixKg, BigDecimal montantAchat) {
        this.mode = Mode.ACHAT;
        this.prixOlivesKg = prixKg;
        this.montantAchat = montantAchat;
    }

    /** Fin d'un pressage acheté : l'huile est dans le puits, aucune remise au vendeur. */
    void finishPurchase(StorageTank tank, BigDecimal huile, BigDecimal pertes) {
        this.tank = tank;
        this.article = tank.getArticle();
        finish(tank.getLot(), huile, pertes);
    }

    /** advance renseignée : part d'une avance déduite de l'achat (pas de nouvelle sortie de caisse). */
    SellerPayment addSellerPayment(String numero, PaymentMode mode, BigDecimal amount, String reference, String user,
                                   SellerAdvance advance) {
        SellerPayment p = new SellerPayment(this, numero, mode, amount, reference, user, advance);
        reglementsVendeur.add(p);
        montantRegle = montantRegle.add(amount);
        return p;
    }

    public boolean isPurchase() {
        return mode == Mode.ACHAT;
    }

    /** ACHAT : reste à payer au vendeur. */
    public BigDecimal resteVendeur() {
        return montantAchat == null ? BigDecimal.ZERO.setScale(3) : montantAchat.subtract(montantRegle);
    }

    /** Informations de transport du bulletin ; modifiables tant que les olives sont seulement reçues. */
    void setTransport(String blNumero, String transporteur, String vehicule, String adresse) {
        this.blNumero = blNumero;
        this.transporteur = transporteur;
        this.vehicule = vehicule;
        this.adresse = adresse;
    }

    /** Fin d'un pressage à façon dont l'huile est versée dans le puits du client. */
    void finishInTank(StorageTank tank, BigDecimal huile, BigDecimal pertes) {
        this.tank = tank;
        finish(tank.getLot(), huile, pertes);
    }

    void start() {
        statut = Status.EN_COURS;
        dateDebut = Instant.now();
    }

    void finish(StockLot lot, BigDecimal huile, BigDecimal pertes) {
        this.lot = lot;
        this.huileObtenue = huile;
        this.pertesKg = pertes;
        this.statut = Status.TERMINE;
        this.dateFin = Instant.now();
    }

    Return addReturn(ClientOilReturn clientReturn, BigDecimal q) {
        Return r = new Return(this, clientReturn, q);
        remises.add(r);
        huileRemise = huileRemise.add(q);
        if (huileRemise.compareTo(huileObtenue) == 0) {
            statut = Status.REMIS;
        }
        return r;
    }

    Payment addPayment(String numero, PaymentMode mode, BigDecimal amount, String reference, String user) {
        Payment p = new Payment(this, numero, mode, amount, reference, user);
        paiements.add(p);
        montantPaye = montantPaye.add(amount);
        return p;
    }

    void cancel(String motif) {
        statut = Status.ANNULE;
        motifAnnulation = motif;
    }

    public BigDecimal reste() {
        return montant.subtract(montantPaye);
    }

    public BigDecimal huileARemettre() {
        return huileObtenue == null || isPurchase() ? BigDecimal.ZERO.setScale(3) : huileObtenue.subtract(huileRemise);
    }

    /** Litres d'huile pour 100 kg d'olives (indicateur, pas une garantie). */
    public BigDecimal rendement() {
        if (huileObtenue == null || poidsNet.signum() == 0) {
            return null;
        }
        return huileObtenue.multiply(BigDecimal.valueOf(100)).divide(poidsNet, 2, java.math.RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public String getNumeroBulletin() { return numeroBulletin; }
    public String getBlNumero() { return blNumero; }
    public String getTransporteur() { return transporteur; }
    public String getVehicule() { return vehicule; }
    public String getAdresse() { return adresse; }
    public Mode getMode() { return mode; }
    public StorageTank getTank() { return tank; }
    public BigDecimal getPrixOlivesKg() { return prixOlivesKg; }
    public BigDecimal getMontantAchat() { return montantAchat; }
    public BigDecimal getMontantRegle() { return montantRegle; }
    public List<SellerPayment> getReglementsVendeur() { return reglementsVendeur; }
    public Client getClient() { return client; }
    public Article getArticle() { return article; }
    public StockLot getLot() { return lot; }
    public Status getStatut() { return statut; }
    public Instant getDateReception() { return dateReception; }
    public String getVariete() { return variete; }
    public String getProvenance() { return provenance; }
    public OliveState getEtatOlives() { return etatOlives; }
    public BigDecimal getPoidsBrut() { return poidsBrut; }
    public BigDecimal getTare() { return tare; }
    public BigDecimal getPoidsNet() { return poidsNet; }
    public BigDecimal getTarifKg() { return tarifKg; }
    public BigDecimal getMontant() { return montant; }
    public BigDecimal getMontantPaye() { return montantPaye; }
    public BigDecimal getHuileObtenue() { return huileObtenue; }
    public BigDecimal getHuileRemise() { return huileRemise; }
    public BigDecimal getPertesKg() { return pertesKg; }
    public Instant getDateDebut() { return dateDebut; }
    public Instant getDateFin() { return dateFin; }
    public String getNotes() { return notes; }
    public String getRecuPar() { return recuPar; }
    public String getMotifAnnulation() { return motifAnnulation; }
    public List<Return> getRemises() { return remises; }
    public List<Payment> getPaiements() { return paiements; }

    @Entity(name = "PressingReturn")
    @Immutable
    @Table(name = "pressing_returns")
    public static class Return {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "pressing_id", nullable = false)
        private Pressing pressing;

        /** Remise au client dont cette ligne est la part prise sur ce pressage. */
        @ManyToOne
        @JoinColumn(name = "client_return_id")
        private ClientOilReturn clientReturn;

        /** Numéro de la remise (RMH-…) : le même pour toutes les parts d'une remise. */
        @Column(nullable = false, length = 30)
        private String numero;

        @Column(nullable = false, precision = 15, scale = 3)
        private BigDecimal quantite;

        @Column(name = "remis_a", nullable = false, length = 150)
        private String remisA;

        @Column(name = "date_remise", nullable = false)
        private Instant date;

        @Column(nullable = false, length = 64)
        private String username;

        protected Return() {
        }

        Return(Pressing pressing, ClientOilReturn clientReturn, BigDecimal quantite) {
            this.pressing = pressing;
            this.clientReturn = clientReturn;
            this.numero = clientReturn.getNumero();
            this.quantite = quantite;
            this.remisA = clientReturn.getRemisA();
            this.username = clientReturn.getUsername();
            this.date = clientReturn.getDate();
        }

        public Long getId() { return id; }
        public ClientOilReturn getClientReturn() { return clientReturn; }
        public String getNumero() { return numero; }
        public BigDecimal getQuantite() { return quantite; }
        public String getRemisA() { return remisA; }
        public Instant getDate() { return date; }
        public String getUsername() { return username; }
    }

    @Entity(name = "PressingPayment")
    @Immutable
    @Table(name = "pressing_payments")
    public static class Payment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "pressing_id", nullable = false)
        private Pressing pressing;

        @Column(nullable = false, unique = true, length = 30)
        private String numero;

        @Enumerated(EnumType.STRING)
        @JdbcTypeCode(SqlTypes.VARCHAR)
        @Column(nullable = false, length = 20)
        private PaymentMode mode;

        @Column(nullable = false, precision = 12, scale = 3)
        private BigDecimal montant;

        @Column(length = 80)
        private String reference;

        @Column(name = "date_paie", nullable = false)
        private Instant date;

        @Column(nullable = false, length = 64)
        private String username;

        protected Payment() {
        }

        Payment(Pressing pressing, String numero, PaymentMode mode, BigDecimal montant, String reference, String username) {
            this.pressing = pressing;
            this.numero = numero;
            this.mode = mode;
            this.montant = montant;
            this.reference = reference;
            this.username = username;
            this.date = Instant.now();
        }

        public String getNumero() { return numero; }
        public PaymentMode getMode() { return mode; }
        public BigDecimal getMontant() { return montant; }
        public String getReference() { return reference; }
        public Instant getDate() { return date; }
        public String getUsername() { return username; }
    }

    /** Paiement au vendeur des olives : sortie de caisse, ou part d'une avance déjà versée (advance renseignée). */
    @Entity(name = "PressingSellerPayment")
    @Immutable
    @Table(name = "pressing_seller_payments")
    public static class SellerPayment {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "pressing_id", nullable = false)
        private Pressing pressing;

        @ManyToOne
        @JoinColumn(name = "advance_id")
        private SellerAdvance advance;

        @Column(nullable = false, unique = true, length = 30)
        private String numero;

        @Enumerated(EnumType.STRING)
        @JdbcTypeCode(SqlTypes.VARCHAR)
        @Column(nullable = false, length = 20)
        private PaymentMode mode;

        @Column(nullable = false, precision = 12, scale = 3)
        private BigDecimal montant;

        @Column(length = 80)
        private String reference;

        @Column(name = "date_paie", nullable = false)
        private Instant date;

        @Column(nullable = false, length = 64)
        private String username;

        protected SellerPayment() {
        }

        SellerPayment(Pressing pressing, String numero, PaymentMode mode, BigDecimal montant, String reference, String username,
                      SellerAdvance advance) {
            this.pressing = pressing;
            this.advance = advance;
            this.numero = numero;
            this.mode = mode;
            this.montant = montant;
            this.reference = reference;
            this.username = username;
            this.date = Instant.now();
        }

        public SellerAdvance getAdvance() { return advance; }
        public String getNumero() { return numero; }
        public PaymentMode getMode() { return mode; }
        public BigDecimal getMontant() { return montant; }
        public String getReference() { return reference; }
        public Instant getDate() { return date; }
        public String getUsername() { return username; }
    }
}
