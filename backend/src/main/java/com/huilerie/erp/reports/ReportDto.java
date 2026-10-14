package com.huilerie.erp.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Rapports : chaque tableau est une liste de lignes simples, exportable en CSV par l'écran. */
public final class ReportDto {

    private ReportDto() {
    }

    public record DayAmount(LocalDate jour, long nombre, BigDecimal total) {
    }

    public record Row(String libelle, String unite, BigDecimal quantite, long nombre, BigDecimal montant,
                      BigDecimal montant2) {
    }

    public record Sales(LocalDate from, LocalDate to, BigDecimal chiffreAffaires, long nbVentes, BigDecimal panierMoyen,
                        BigDecimal remises, long nbAnnulations, List<DayAmount> parJour, List<Row> parArticle,
                        List<Row> parClient, List<Row> parCaissier) {
    }

    public record Purchases(LocalDate from, LocalDate to, BigDecimal totalTtc, List<Row> parFournisseur,
                            BigDecimal dettesFournisseurs) {
    }

    public record StockValue(String reference, String nom, String type, String unite, BigDecimal disponible,
                             BigDecimal prixAchat, BigDecimal valeur, boolean alerte) {
    }

    public record Stock(BigDecimal valeurTotale, long nbArticles, long nbAlertes, long nbSansPrix, List<StockValue> lignes) {
    }

    public record Finance(LocalDate from, LocalDate to, BigDecimal encaissements, BigDecimal decaissements, BigDecimal solde,
                          List<Row> parMode, BigDecimal depenses, List<Row> depensesParCategorie,
                          BigDecimal creancesClients, BigDecimal dettesFournisseurs) {
    }

    /** Production, pressage et conditionnement de la période. Rendement en litres pour 100 kg. */
    public record Production(LocalDate from, LocalDate to, long nbTriturations, BigDecimal olivesKg, BigDecimal huileLitres,
                             BigDecimal rendementMoyen, List<Row> parCategorie, long nbPressages, BigDecimal pressageKg,
                             BigDecimal pressageHuile, BigDecimal pressageRendement, BigDecimal pressageChiffre,
                             List<Row> rendementParVariete, List<Row> conditionnement) {
    }
}
