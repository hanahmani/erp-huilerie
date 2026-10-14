package com.huilerie.erp.settings;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.settings.dto.SettingsRequest;
import com.huilerie.erp.settings.dto.SettingsResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SettingsService {

    private final CompanySettingsRepository repository;
    private final AuditService auditService;
    private final com.huilerie.erp.catalog.ArticleRepository articles;

    public SettingsService(CompanySettingsRepository repository, AuditService auditService,
                           com.huilerie.erp.catalog.ArticleRepository articles) {
        this.repository = repository;
        this.auditService = auditService;
        this.articles = articles;
    }

    /** Lisible par tout utilisateur connecté : nécessaire pour imprimer les tickets. */
    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return find().map(SettingsResponse::from).orElse(SettingsResponse.empty());
    }

    @Transactional(readOnly = true)
    public Optional<CompanySettings> find() {
        return repository.findById(CompanySettings.SINGLE_ID);
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
        s.setVenteCreditAutorisee(r.venteCreditAutorisee());
        s.setTimbreFiscal(com.huilerie.erp.common.Money.strict(r.timbreFiscal(), "Timbre fiscal"));
        s.setInactiviteMinutes(r.inactiviteMinutes());
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
        return SettingsResponse.from(s);
    }
}
