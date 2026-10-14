package com.huilerie.erp.dashboard.dto;

import com.huilerie.erp.inventory.dto.StockDto;
import com.huilerie.erp.sales.dto.SaleDto;

import java.math.BigDecimal;
import java.util.List;

/** Indicateurs calculés à partir des données enregistrées. null = information non autorisée pour ce rôle. */
public record DashboardResponse(
        BigDecimal ventesJour, Long nbVentesJour, BigDecimal ventesMois,
        BigDecimal encaissementsJour, BigDecimal creancesClients,
        Long nbClients, Long nbFournisseurs, Long nbArticles, Long nbLotsBloques,
        List<StockDto.ArticleStock> alertesStock,
        List<SaleDto.Summary> dernieresVentes,
        List<DayTotal> ventes7Jours,
        BigDecimal achatsMois, BigDecimal dettesFournisseurs, Long commandesARecevoir,
        BigDecimal depensesMois, Boolean caisseOuverte, BigDecimal especesEnCaisse,
        Long pressagesEnCours, BigDecimal huileClientsARemettre, BigDecimal pressagesImpayes,
        Long triturationsEnCours, BigDecimal huileProduiteMois, BigDecimal rendementMois) {

    public record DayTotal(String jour, BigDecimal total) {
    }
}
