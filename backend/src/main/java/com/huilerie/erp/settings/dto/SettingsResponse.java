package com.huilerie.erp.settings.dto;

import com.huilerie.erp.settings.CompanySettings;

/** configured = false tant que l'administrateur n'a rien saisi : rien n'est inventé. logo = un logo est téléversé. */
public record SettingsResponse(boolean configured, String raisonSociale, String adresse, String telephone,
                               String email, String matriculeFiscal, String messageTicket,
                               boolean venteCreditAutorisee, java.math.BigDecimal tarifPressageKg,
                               Long pressageArticleId, String pressageArticleNom, java.math.BigDecimal timbreFiscal,
                               Integer inactiviteMinutes, java.math.BigDecimal densiteHuile,
                               String rne, String mentionsFacture, String mentionsDevis, String mentionExport,
                               String mentionEchantillon, boolean logo) {

    public static SettingsResponse empty() {
        return new SettingsResponse(false, null, null, null, null, null, null, false, null, null, null, null, null,
                com.huilerie.erp.catalog.OilUnits.DEFAULT_DENSITY, null, null, null, null, null, false);
    }

    public static SettingsResponse from(CompanySettings s, boolean logo) {
        return new SettingsResponse(true, s.getRaisonSociale(), s.getAdresse(), s.getTelephone(), s.getEmail(),
                s.getMatriculeFiscal(), s.getMessageTicket(), s.isVenteCreditAutorisee(), s.getTarifPressageKg(),
                s.getPressageArticle() == null ? null : s.getPressageArticle().getId(),
                s.getPressageArticle() == null ? null : s.getPressageArticle().getNom(), s.getTimbreFiscal(),
                s.getInactiviteMinutes(), s.getDensiteHuile(), s.getRne(), s.getMentionsFacture(), s.getMentionsDevis(),
                s.getMentionExport(), s.getMentionEchantillon(), logo);
    }
}
