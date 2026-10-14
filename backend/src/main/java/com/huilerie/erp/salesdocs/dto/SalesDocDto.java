package com.huilerie.erp.salesdocs.dto;

import com.huilerie.erp.finance.PaymentMode;
import com.huilerie.erp.salesdocs.SalesDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SalesDocDto {

    private SalesDocDto() {
    }

    public record LineRequest(@NotNull Long articleId, Long lotId,
                              @NotNull @DecimalMin(value = "0.001", message = "Quantité positive.") BigDecimal quantite,
                              @DecimalMin(value = "0", message = "Prix positif ou nul.") BigDecimal prixUnitaire,
                              @DecimalMin("0") @DecimalMax("100") BigDecimal remisePct,
                              Long taxId) {
    }

    public record Request(@NotNull(message = "Le type est obligatoire.") SalesDocument.Type type,
                          @NotNull(message = "Le client est obligatoire.") Long clientId,
                          @NotNull(message = "La date est obligatoire.") LocalDate dateDoc,
                          LocalDate echeance,
                          @Size(max = 60) String refClient,
                          @Size(max = 500) String notes,
                          @NotEmpty(message = "Ajoutez au moins une ligne.") @Size(max = 200) List<@Valid LineRequest> lignes) {
    }

    public record TransformRequest(@NotNull SalesDocument.Type cible) {
    }

    public record PaymentRequest(@NotNull PaymentMode mode,
                                 @NotNull @DecimalMin(value = "0.001", message = "Montant positif.") BigDecimal montant,
                                 @Size(max = 80) String reference) {
    }

    public record CreditNoteRequest(@NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif,
                                    boolean retourStock, PaymentMode modeRemboursement) {
    }

    public record CancelRequest(@NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif) {
    }

    public record Line(Long id, Long articleId, Long lotId, String lotNumero, String designation, String unite,
                       BigDecimal quantite, BigDecimal prixUnitaire, BigDecimal remisePct, BigDecimal tauxTva,
                       BigDecimal totalHt, BigDecimal totalTva) {
    }

    public record Payment(String numero, Instant date, PaymentMode mode, BigDecimal montant, String reference, String username) {
    }

    public record Related(Long id, SalesDocument.Type type, String numero, SalesDocument.Status statut) {
    }

    public record Detail(Long id, SalesDocument.Type type, String numero, SalesDocument.Status statut, Long clientId,
                         String clientNom, String clientCode, String clientAdresse, String clientMatriculeFiscal,
                         LocalDate dateDoc, LocalDate echeance, String refClient, String notes,
                         BigDecimal totalHt, BigDecimal totalTva, BigDecimal timbre, BigDecimal totalTtc,
                         BigDecimal montantPaye, BigDecimal reste, String creePar, Instant createdAt, String validePar,
                         Instant valideLe, String motifAnnulation, Related source, List<Related> suites,
                         List<Line> lignes, List<Payment> paiements) {

        public static Detail from(SalesDocument d, List<SalesDocument> children) {
            var c = d.getClient();
            return new Detail(d.getId(), d.getType(), d.getNumero(), d.getStatut(), c.getId(), c.getNom(), c.getCode(),
                    c.getAdresse(), c.getMatriculeFiscal(), d.getDateDoc(), d.getEcheance(), d.getRefClient(),
                    d.getNotes(), d.getTotalHt(), d.getTotalTva(), d.getTimbre(), d.getTotalTtc(), d.getMontantPaye(),
                    d.reste(), d.getCreePar(), d.getCreatedAt(), d.getValidePar(), d.getValideLe(), d.getMotifAnnulation(),
                    d.getSource() == null ? null : rel(d.getSource()),
                    children.stream().map(Detail::rel).toList(),
                    d.getLignes().stream().map(l -> new Line(l.getId(), l.getArticle().getId(),
                            l.getLot() == null ? null : l.getLot().getId(), l.getLot() == null ? null : l.getLot().getNumero(),
                            l.getDesignation(), l.getUnite(), l.getQuantite(), l.getPrixUnitaire(), l.getRemisePct(),
                            l.getTauxTva(), l.getTotalHt(), l.getTotalTva())).toList(),
                    d.getPaiements().stream().map(p -> new Payment(p.getNumero(), p.getDate(), p.getMode(), p.getMontant(),
                            p.getReference(), p.getUsername())).toList());
        }

        static Related rel(SalesDocument d) {
            return new Related(d.getId(), d.getType(), d.getNumero(), d.getStatut());
        }
    }

    public record Summary(Long id, SalesDocument.Type type, String numero, SalesDocument.Status statut, String clientNom,
                          LocalDate dateDoc, LocalDate echeance, BigDecimal totalTtc, BigDecimal montantPaye, BigDecimal reste) {
        public static Summary from(SalesDocument d) {
            return new Summary(d.getId(), d.getType(), d.getNumero(), d.getStatut(), d.getClient().getNom(), d.getDateDoc(),
                    d.getEcheance(), d.getTotalTtc(), d.getMontantPaye(), d.reste());
        }
    }
}
