package com.huilerie.erp.pressing.dto;

import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.pressing.Pressing;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class PressingDto {

    private PressingDto() {
    }

    /**
     * Bulletin de pressage / bon de pesée. mode vide : SERVICE.
     * SERVICE : tarifKg et articleId facultatifs, sinon valeurs des paramètres.
     * ACHAT : prixOlivesKg obligatoire ; articleId et tarifKg ignorés (l'huile ira dans un puits) ;
     * les avances déjà versées au vendeur sont déduites, puis avance (facultative) est payée le jour même.
     * adresse vide : adresse du client.
     */
    public record ReceiveRequest(
            @NotNull(message = "Le client est obligatoire.") Long clientId,
            Long articleId,
            @Size(max = 80) String variete,
            @Size(max = 120) String provenance,
            @NotNull(message = "L'état des olives est obligatoire.") Pressing.OliveState etatOlives,
            @NotNull @DecimalMin(value = "0.001", message = "Le poids brut doit être positif.") BigDecimal poidsBrut,
            @NotNull @DecimalMin(value = "0", message = "La tare ne peut pas être négative.") BigDecimal tare,
            @DecimalMin(value = "0", message = "Le tarif ne peut pas être négatif.") BigDecimal tarifKg,
            @Size(max = 255) String notes,
            @Size(max = 50, message = "N° de bon de livraison : 50 caractères au maximum.") String blNumero,
            @Size(max = 120, message = "Transporteur : 120 caractères au maximum.") String transporteur,
            @Size(max = 30, message = "N° de véhicule : 30 caractères au maximum.") String vehicule,
            @Size(max = 255, message = "Adresse : 255 caractères au maximum.") String adresse,
            Pressing.Mode mode,
            @DecimalMin(value = "0", message = "Le prix d'achat ne peut pas être négatif.") BigDecimal prixOlivesKg,
            @DecimalMin(value = "0", message = "L'avance ne peut pas être négative.") BigDecimal avance,
            PaymentMode modeAvance,
            /** Date de réception des olives ; maintenant si absente. */
            Instant dateReception) {

        /** Bulletin daté de maintenant. */
        public ReceiveRequest(Long clientId, Long articleId, String variete, String provenance,
                              Pressing.OliveState etatOlives, BigDecimal poidsBrut, BigDecimal tare, BigDecimal tarifKg,
                              String notes, String blNumero, String transporteur, String vehicule, String adresse,
                              Pressing.Mode mode, BigDecimal prixOlivesKg, BigDecimal avance, PaymentMode modeAvance) {
            this(clientId, articleId, variete, provenance, etatOlives, poidsBrut, tare, tarifKg, notes, blNumero,
                    transporteur, vehicule, adresse, mode, prixOlivesKg, avance, modeAvance, null);
        }

        /** Bulletin daté de maintenant, sans avance payée le jour même. */
        public ReceiveRequest(Long clientId, Long articleId, String variete, String provenance,
                              Pressing.OliveState etatOlives, BigDecimal poidsBrut, BigDecimal tare, BigDecimal tarifKg,
                              String notes, String blNumero, String transporteur, String vehicule, String adresse,
                              Pressing.Mode mode, BigDecimal prixOlivesKg) {
            this(clientId, articleId, variete, provenance, etatOlives, poidsBrut, tare, tarifKg, notes, blNumero,
                    transporteur, vehicule, adresse, mode, prixOlivesKg, null, null, null);
        }
    }

    /** Nouvelle date de réception des olives. */
    public record DateRequest(@NotNull(message = "La date de réception est obligatoire.") Instant dateReception) {
    }

    /** Correction des informations de transport du bulletin (statut RECU uniquement). */
    public record TransportRequest(
            @Size(max = 50, message = "N° de bon de livraison : 50 caractères au maximum.") String blNumero,
            @Size(max = 120, message = "Transporteur : 120 caractères au maximum.") String transporteur,
            @Size(max = 30, message = "N° de véhicule : 30 caractères au maximum.") String vehicule,
            @Size(max = 255, message = "Adresse : 255 caractères au maximum.") String adresse,
            /** Nouvelle date de réception ; inchangée si absente. */
            Instant dateReception) {

        public TransportRequest(String blNumero, String transporteur, String vehicule, String adresse) {
            this(blNumero, transporteur, vehicule, adresse, null);
        }
    }

    /**
     * tankId : obligatoire pour un achat d'olives (puits de l'huilerie où l'huile est versée) ;
     * facultatif pour un pressage à façon (citerne, fût ou puits réservé au client, sinon lot propre au pressage).
     * priseDirecte (pressage à façon) : le client emporte toute son huile tout de suite ; le reste du service est
     * encaissé en même temps (modePaiement obligatoire s'il reste à payer) ; remisA : personne qui prend l'huile.
     */
    public record FinishRequest(
            @NotNull @DecimalMin(value = "0.001", message = "La quantité d'huile doit être positive.") BigDecimal huileObtenue,
            @DecimalMin(value = "0", message = "Les pertes ne peuvent pas être négatives.") BigDecimal pertesKg,
            Long tankId,
            boolean priseDirecte,
            PaymentMode modePaiement,
            @Size(max = 80) String reference,
            @Size(max = 150, message = "Remis à : 150 caractères au maximum.") String remisA) {

        public FinishRequest(BigDecimal huileObtenue, BigDecimal pertesKg, Long tankId) {
            this(huileObtenue, pertesKg, tankId, false, null, null, null);
        }
    }

    public record ReturnRequest(
            @NotNull @DecimalMin(value = "0.001", message = "La quantité doit être positive.") BigDecimal quantite,
            @Size(max = 150) String remisA) {
    }

    /** Remise sur le compte d'huile du client : quantité en kg, ou en litres (convertie avec la densité). */
    public record ClientReturnRequest(
            @NotNull @DecimalMin(value = "0.001", message = "La quantité doit être positive.") BigDecimal quantite,
            boolean enLitres,
            @Size(max = 150) String remisA) {
    }

    public record PaymentRequest(@NotNull(message = "Mode obligatoire.") PaymentMode mode,
                                 @NotNull @DecimalMin(value = "0.001", message = "Montant positif.") BigDecimal montant,
                                 @Size(max = 80) String reference) {
    }

    public record CancelRequest(@NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif) {
    }

    public record ReturnLine(String numero, Instant date, BigDecimal quantite, String remisA, String username) {
    }

    /** avanceNumero : règlement vendeur pris sur une avance déjà versée (pas de sortie de caisse). */
    public record PaymentLine(String numero, Instant date, PaymentMode mode, BigDecimal montant, String reference, String username,
                              String avanceNumero) {
    }

    public record Detail(Long id, String numero, Pressing.Status statut, Long clientId, String clientNom, String clientCode,
                         String clientTelephone, Long articleId, String articleNom, String unite, String lotNumero,
                         Instant dateReception, String variete, String provenance, Pressing.OliveState etatOlives,
                         BigDecimal poidsBrut, BigDecimal tare, BigDecimal poidsNet, BigDecimal tarifKg,
                         BigDecimal montant, BigDecimal montantPaye, BigDecimal reste,
                         BigDecimal huileObtenue, BigDecimal huileRemise, BigDecimal huileARemettre, BigDecimal pertesKg,
                         BigDecimal rendement, Instant dateDebut, Instant dateFin, String notes, String recuPar,
                         String motifAnnulation, List<ReturnLine> remises, List<PaymentLine> paiements,
                         String numeroBulletin, String blNumero, String transporteur, String vehicule, String adresse,
                         Pressing.Mode mode, BigDecimal prixOlivesKg, BigDecimal montantAchat, BigDecimal montantRegle,
                         BigDecimal resteVendeur, Long tankId, String tankCode, String tankNom,
                         List<PaymentLine> reglementsVendeur) {

        public static Detail from(Pressing p) {
            var a = p.getArticle();
            var t = p.getTank();
            return new Detail(p.getId(), p.getNumero(), p.getStatut(), p.getClient().getId(), p.getClient().getNom(),
                    p.getClient().getCode(), p.getClient().getTelephone(), a == null ? null : a.getId(),
                    a == null ? null : a.getNom(), a == null ? null : a.getUnite().getCode(),
                    p.getLot() == null ? null : p.getLot().getNumero(),
                    p.getDateReception(), p.getVariete(), p.getProvenance(), p.getEtatOlives(), p.getPoidsBrut(),
                    p.getTare(), p.getPoidsNet(), p.getTarifKg(), p.getMontant(), p.getMontantPaye(), p.reste(),
                    p.getHuileObtenue(), p.getHuileRemise(), p.huileARemettre(), p.getPertesKg(), p.rendement(),
                    p.getDateDebut(), p.getDateFin(), p.getNotes(), p.getRecuPar(), p.getMotifAnnulation(),
                    p.getRemises().stream().map(r -> new ReturnLine(r.getNumero(), r.getDate(), r.getQuantite(),
                            r.getRemisA(), r.getUsername())).toList(),
                    p.getPaiements().stream().map(x -> new PaymentLine(x.getNumero(), x.getDate(), x.getMode(),
                            x.getMontant(), x.getReference(), x.getUsername(), null)).toList(),
                    p.getNumeroBulletin(), p.getBlNumero(), p.getTransporteur(), p.getVehicule(), p.getAdresse(),
                    p.getMode(), p.getPrixOlivesKg(), p.getMontantAchat(), p.getMontantRegle(), p.resteVendeur(),
                    t == null ? null : t.getId(), t == null ? null : t.getCode(), t == null ? null : t.getNom(),
                    p.getReglementsVendeur().stream().map(x -> new PaymentLine(x.getNumero(), x.getDate(), x.getMode(),
                            x.getMontant(), x.getReference(), x.getUsername(),
                            x.getAdvance() == null ? null : x.getAdvance().getNumero())).toList());
        }
    }

    public record Summary(Long id, String numero, Pressing.Status statut, String clientNom, Instant dateReception,
                          BigDecimal poidsNet, BigDecimal huileObtenue, BigDecimal huileARemettre, BigDecimal rendement,
                          BigDecimal montant, BigDecimal reste, String numeroBulletin, String vehicule,
                          Pressing.Mode mode, BigDecimal montantAchat, BigDecimal resteVendeur, String tankCode) {
        public static Summary from(Pressing p) {
            return new Summary(p.getId(), p.getNumero(), p.getStatut(), p.getClient().getNom(), p.getDateReception(),
                    p.getPoidsNet(), p.getHuileObtenue(), p.huileARemettre(), p.rendement(), p.getMontant(), p.reste(),
                    p.getNumeroBulletin(), p.getVehicule(), p.getMode(), p.getMontantAchat(), p.resteVendeur(),
                    p.getTank() == null ? null : p.getTank().getCode());
        }
    }

    // ------------------------------------------------------------ comptes clients

    /** Une ligne du relevé d'huile : ENTREE (fin d'un pressage) ou SORTIE (remise), avec le solde après l'opération. */
    public record OilMovement(Instant date, String type, String numero, String detail, BigDecimal entree,
                              BigDecimal sortie, BigDecimal litres, BigDecimal solde, String username) {
    }

    public record TankLine(Long id, String code, String nom, BigDecimal quantite, BigDecimal capacite) {
    }

    /**
     * Compte d'huile d'un client (pressage à façon) : solde en kg (et en litres), argent encore dû pour le service,
     * puits réservés, relevé chronologique des entrées et sorties.
     */
    public record OilAccount(Long clientId, String clientNom, String clientCode, String unite, BigDecimal solde,
                             BigDecimal soldeLitres, BigDecimal densite, BigDecimal montantDu, List<TankLine> puits,
                             List<OilMovement> mouvements, List<ServiceLine> services, List<ServicePaymentLine> paiements) {
    }

    /** Service d'un pressage à façon du client : montant, déjà payé, reste dû. */
    public record ServiceLine(Long id, String numero, String numeroBulletin, Instant date, Pressing.Status statut,
                              BigDecimal poidsNet, BigDecimal montant, BigDecimal paye, BigDecimal reste) {
    }

    /** Paiement du service, imputé sur un pressage. */
    public record ServicePaymentLine(String numero, Instant date, PaymentMode mode, BigDecimal montant, String reference,
                                     String pressingNumero, String numeroBulletin, String username) {
    }

    public record AdvanceLine(String numero, Instant date, PaymentMode mode, BigDecimal montant, BigDecimal impute,
                              BigDecimal reste, String reference, String username) {
    }

    public record PurchaseLine(Long id, String numero, String numeroBulletin, Instant date, Pressing.Status statut,
                               BigDecimal poidsNet, BigDecimal prixOlivesKg, BigDecimal montantAchat,
                               BigDecimal montantRegle, BigDecimal reste) {
    }

    /** Compte vendeur : achats d'olives, ce qui lui reste dû, avances versées et encore disponibles. */
    public record SellerAccount(Long clientId, String clientNom, String clientCode, BigDecimal resteDu,
                                BigDecimal avanceDisponible, List<AdvanceLine> avances, List<PurchaseLine> achats) {
    }

    /** Résumé par client pour la liste des comptes. */
    public record AccountRow(Long clientId, String clientNom, String clientCode, String telephone, BigDecimal huile,
                             BigDecimal montantDu, BigDecimal resteVendeur, BigDecimal avanceDisponible) {
    }
}
