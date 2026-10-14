package com.huilerie.erp.inventory.dto;

import com.huilerie.erp.inventory.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class StockDto {

    private StockDto() {
    }

    /** Mouvement saisi manuellement : le motif est toujours obligatoire. */
    public record MovementRequest(
            @NotNull(message = "L'article est obligatoire.") Long articleId,
            Long lotId,
            @NotNull(message = "Le type est obligatoire.") MovementType type,
            @NotNull(message = "La quantité est obligatoire.")
            @DecimalMin(value = "0.001", message = "La quantité doit être positive.") BigDecimal quantite,
            @NotBlank(message = "Le motif est obligatoire.") @Size(max = 255) String motif) {
    }

    public record LotRequest(
            @NotNull(message = "L'article est obligatoire.") Long articleId,
            @NotBlank(message = "Le numéro de lot est obligatoire.") @Size(max = 50) String numero,
            @NotNull(message = "Le propriétaire est obligatoire.") LotOwner proprietaire,
            Long clientId) {
    }

    public record LotStatusRequest(@NotNull LotStatus statut, @Size(max = 255) String motif) {
    }

    public record MovementResponse(Long id, Instant date, Long articleId, String articleNom, String unite,
                                   Long lotId, String lotNumero, MovementType type, int sens, BigDecimal quantite,
                                   String motif, String documentType, Long documentId, String username) {
        public static MovementResponse from(StockMovement m) {
            return new MovementResponse(m.getId(), m.getCreatedAt(), m.getArticle().getId(), m.getArticle().getNom(),
                    m.getArticle().getUnite().getCode(),
                    m.getLot() == null ? null : m.getLot().getId(), m.getLot() == null ? null : m.getLot().getNumero(),
                    m.getType(), m.getSens(), m.getQuantite(), m.getMotif(), m.getDocumentType(), m.getDocumentId(),
                    m.getUsername());
        }
    }

    public record LotResponse(Long id, Long articleId, String articleNom, String numero, LotOwner proprietaire,
                              Long clientId, String clientNom, LotStatus statut, String motifBlocage,
                              BigDecimal quantite, Instant createdAt) {
        public static LotResponse from(StockLot l, BigDecimal quantite) {
            return new LotResponse(l.getId(), l.getArticle().getId(), l.getArticle().getNom(), l.getNumero(),
                    l.getProprietaire(), l.getClient() == null ? null : l.getClient().getId(),
                    l.getClient() == null ? null : l.getClient().getNom(), l.getStatut(), l.getMotifBlocage(),
                    quantite, l.getCreatedAt());
        }
    }

    /** État du stock d'un article, ventilé par propriétaire et par statut. */
    public record ArticleStock(Long articleId, String reference, String nom, String unite, String type,
                               BigDecimal disponible, BigDecimal bloque, BigDecimal clients, BigDecimal seuilMin,
                               boolean alerte, List<LotLine> lots) {
    }

    public record LotLine(Long lotId, String numero, LotOwner proprietaire, LotStatus statut, BigDecimal quantite) {
    }
}
