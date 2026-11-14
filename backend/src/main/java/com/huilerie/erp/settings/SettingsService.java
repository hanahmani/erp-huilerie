package com.huilerie.erp.settings;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.settings.dto.SettingsRequest;
import com.huilerie.erp.settings.dto.SettingsResponse;
import com.huilerie.erp.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class SettingsService {

    private final CompanySettingsRepository repository;
    private final AuditService auditService;
    private final com.huilerie.erp.catalog.ArticleRepository articles;
    private final JdbcTemplate jdbc;

    public SettingsService(CompanySettingsRepository repository, AuditService auditService,
                           com.huilerie.erp.catalog.ArticleRepository articles, JdbcTemplate jdbc) {
        this.repository = repository;
        this.auditService = auditService;
        this.articles = articles;
        this.jdbc = jdbc;
    }

    /** Logo téléversé : contenu et type MIME (image/png ou image/jpeg). */
    public record Logo(byte[] data, String type) {
    }

    public static final int LOGO_MAX_BYTES = 500 * 1024;

    /** Lisible par tout utilisateur connecté : nécessaire pour imprimer les tickets. */
    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return find().map(s -> SettingsResponse.from(s, hasLogo())).orElse(SettingsResponse.empty());
    }

    @Transactional(readOnly = true)
    public Optional<CompanySettings> find() {
        return repository.findById(CompanySettings.SINGLE_ID);
    }

    /** kg par litre ; 0,916 tant que les paramètres n'ont pas été enregistrés. */
    public java.math.BigDecimal oilDensity() {
        return find().map(CompanySettings::getDensiteHuile).orElse(com.huilerie.erp.catalog.OilUnits.DEFAULT_DENSITY);
    }

    public boolean creditAllowed() {
        return find().map(CompanySettings::isVenteCreditAutorisee).orElse(false);
    }

    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @Transactional
    public SettingsResponse update(SettingsRequest r) {
        CompanySettings s = repository.findById(CompanySettings.SINGLE_ID).orElseGet(CompanySettings::new);
        s.setRaisonSociale(r.raisonSociale().trim());
        s.setAdresse(Text.clean(r.adresse()));
        s.setTelephone(Text.clean(r.telephone()));
        s.setEmail(Text.clean(r.email()));
        s.setMatriculeFiscal(Text.clean(r.matriculeFiscal()));
        s.setMessageTicket(Text.clean(r.messageTicket()));
        s.setRne(Text.clean(r.rne()));
        s.setMentionsFacture(Text.clean(r.mentionsFacture()));
        s.setMentionsDevis(Text.clean(r.mentionsDevis()));
        s.setMentionExport(Text.clean(r.mentionExport()));
        s.setMentionEchantillon(Text.clean(r.mentionEchantillon()));
        s.setVenteCreditAutorisee(r.venteCreditAutorisee());
        s.setTimbreFiscal(com.huilerie.erp.common.Money.strict(r.timbreFiscal(), "Timbre fiscal"));
        s.setInactiviteMinutes(r.inactiviteMinutes());
        java.math.BigDecimal density = r.densiteHuile() == null ? com.huilerie.erp.catalog.OilUnits.DEFAULT_DENSITY
                : r.densiteHuile().setScale(4, java.math.RoundingMode.HALF_UP);
        boolean densityChanged = s.getDensiteHuile() == null || s.getDensiteHuile().compareTo(density) != 0;
        s.setDensiteHuile(density);
        if (densityChanged) {
            // Les prix au kg des huiles vendues au litre suivent la nouvelle densité.
            articles.findByPrixVenteLitreIsNotNull().forEach(a ->
                    a.setPrixVente(com.huilerie.erp.catalog.OilUnits.kgPrice(a.getPrixVenteLitre(), density)));
        }
        s.setTarifPressageKg(com.huilerie.erp.common.Money.strict(r.tarifPressageKg(), "Tarif de pressage"));
        if (r.pressageArticleId() == null) {
            s.setPressageArticle(null);
        } else {
            var a = articles.findById(r.pressageArticleId())
                    .orElseThrow(() -> new com.huilerie.erp.common.exception.NotFoundException("Article introuvable."));
            if (!a.isSuiviLot()) {
                throw new com.huilerie.erp.common.exception.BusinessException(
                        "L'article « huile des clients » doit être suivi par lot.");
            }
            s.setPressageArticle(a);
        }
        s = repository.save(s);
        auditService.log(SecurityUtils.username(), "SETTINGS_UPDATE", "CompanySettings", "1", null);
        return SettingsResponse.from(s, hasLogo());
    }

    // ---------------------------------------------------------------- logo
    // Stocké dans company_settings (colonnes hors entité) : il suit la sauvegarde de la base.

    @Transactional(readOnly = true)
    public boolean hasLogo() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM company_settings WHERE id = ? AND logo_data IS NOT NULL",
                Integer.class, CompanySettings.SINGLE_ID);
        return n != null && n > 0;
    }

    @Transactional(readOnly = true)
    public Optional<Logo> logo() {
        List<Logo> rows = jdbc.query("SELECT logo_data, logo_type FROM company_settings WHERE id = ? AND logo_data IS NOT NULL",
                (rs, i) -> new Logo(rs.getBytes(1), rs.getString(2)), CompanySettings.SINGLE_ID);
        return rows.stream().findFirst();
    }

    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @Transactional
    public SettingsResponse uploadLogo(byte[] data) {
        if (find().isEmpty()) {
            throw new BusinessException("Enregistrez d'abord les informations de l'huilerie, puis ajoutez le logo.");
        }
        if (data == null || data.length == 0) {
            throw new BusinessException("Le fichier du logo est vide.");
        }
        if (data.length > LOGO_MAX_BYTES) {
            throw new BusinessException("Logo trop lourd : 500 Ko au maximum.");
        }
        String type = imageType(data);
        if (type == null) {
            throw new BusinessException("Format de logo non pris en charge : utilisez une image PNG ou JPEG.");
        }
        jdbc.update("UPDATE company_settings SET logo_data = ?, logo_type = ? WHERE id = ?", data, type, CompanySettings.SINGLE_ID);
        auditService.log(SecurityUtils.username(), "SETTINGS_LOGO", "CompanySettings", "1", type + " " + data.length + " octets");
        return get();
    }

    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @Transactional
    public SettingsResponse deleteLogo() {
        jdbc.update("UPDATE company_settings SET logo_data = NULL, logo_type = NULL WHERE id = ?", CompanySettings.SINGLE_ID);
        auditService.log(SecurityUtils.username(), "SETTINGS_LOGO", "CompanySettings", "1", "supprimé");
        return get();
    }

    /** Type réel d'après les premiers octets (on ne se fie pas à l'extension). */
    static String imageType(byte[] d) {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (d.length >= png.length && Arrays.equals(Arrays.copyOf(d, png.length), png)) {
            return "image/png";
        }
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }
}
