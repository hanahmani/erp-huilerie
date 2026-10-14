package com.huilerie.erp.settings.dto;

import com.huilerie.erp.settings.CompanySettings;

/** configured = false tant que l'administrateur n'a rien saisi : rien n'est inventé. */
public record SettingsResponse(boolean configured, String raisonSociale, String adresse, String telephone,
                               String email, String matriculeFiscal, String messageTicket,
                               boolean venteCreditAutorisee, java.math.BigDecimal tarifPressageKg,
                               Long pressageArticleId, String pressageArticleNom, java.math.BigDecimal timbreFiscal,
                               Integer inactiviteMinutes) {

    public static SettingsResponse empty() {
        return new SettingsResponse(false, null, null, null, null, null, null, false, null, null, null, null, null);
    }

    public static SettingsResponse from(CompanySettings s) {
        return new SettingsResponse(true, s.getRaisonSociale(), s.getAdresse(), s.getTelephone(), s.getEmail(),
                s.getMatriculeFiscal(), s.getMessageTicket(), s.isVenteCreditAutorisee(), s.getTarifPressageKg(),
                s.getPressageArticle() == null ? null : s.getPressageArticle().getId(),
                s.getPressageArticle() == null ? null : s.getPressageArticle().getNom(), s.getTimbreFiscal(),
                s.getInactiviteMinutes());
    }
}
