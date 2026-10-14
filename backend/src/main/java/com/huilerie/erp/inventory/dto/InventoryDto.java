package com.huilerie.erp.inventory.dto;

import com.huilerie.erp.inventory.Inventory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryDto {

    private InventoryDto() {
    }

    /** articleIds vide = tous les articles actifs. */
    public record CreateRequest(@NotBlank(message = "Le libellé est obligatoire.") @Size(max = 150) String libelle,
                                List<Long> articleIds) {
    }

    public record CountLine(@NotNull Long lineId,
                            @DecimalMin(value = "0", message = "Quantité positive ou nulle.") BigDecimal quantiteComptee) {
    }

    public record CountRequest(@NotNull List<@Valid CountLine> lignes) {
    }

    public record Line(Long id, Long articleId, String article, String reference, String unite, String lot,
                       BigDecimal theorique, BigDecimal comptee, BigDecimal ecart) {
        static Line from(Inventory.Line l) {
            BigDecimal ecart = l.getQuantiteComptee() == null ? null : l.getQuantiteComptee().subtract(l.getQuantiteTheorique());
            return new Line(l.getId(), l.getArticle().getId(), l.getArticle().getNom(), l.getArticle().getReference(),
                    l.getArticle().getUnite().getCode(), l.getLot() == null ? null : l.getLot().getNumero(),
                    l.getQuantiteTheorique(), l.getQuantiteComptee(), ecart);
        }
    }

    public record Summary(Long id, String numero, String libelle, String statut, String creePar, Instant createdAt,
                          String validePar, Instant valideLe, int nbLignes, long nbComptees, long nbEcarts) {
        public static Summary from(Inventory i) {
            long counted = i.getLignes().stream().filter(l -> l.getQuantiteComptee() != null).count();
            long gaps = i.getLignes().stream().filter(l -> l.getQuantiteComptee() != null
                    && l.getQuantiteComptee().compareTo(l.getQuantiteTheorique()) != 0).count();
            return new Summary(i.getId(), i.getNumero(), i.getLibelle(), i.getStatut(), i.getCreePar(), i.getCreatedAt(),
                    i.getValidePar(), i.getValideLe(), i.getLignes().size(), counted, gaps);
        }
    }

    public record Detail(Summary entete, List<Line> lignes) {
        public static Detail from(Inventory i) {
            return new Detail(Summary.from(i), i.getLignes().stream().map(Line::from).toList());
        }
    }
}
