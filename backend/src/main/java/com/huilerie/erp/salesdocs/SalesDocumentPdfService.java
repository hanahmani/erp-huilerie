package com.huilerie.erp.salesdocs;

import com.huilerie.erp.common.Devise;
import com.huilerie.erp.common.MontantEnLettres;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.salesdocs.SalesDocument.Status;
import com.huilerie.erp.salesdocs.SalesDocument.Type;
import com.huilerie.erp.settings.CompanySettings;
import com.huilerie.erp.settings.SettingsService;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PDF A4 des documents commerciaux (devis, commande, bon de livraison, facture, avoir), même gabarit pour tous :
 * seuls le titre, la phrase d'arrêté et les mentions changent. Tout vient de la base ; si une information
 * légale obligatoire manque, la génération est refusée avec un message qui dit quoi compléter.
 */
@Service
public class SalesDocumentPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FONT = "DejaVu Sans";

    private static final Map<Type, String> TITRES = Map.of(Type.DEVIS, "DEVIS", Type.COMMANDE, "BON DE COMMANDE",
            Type.LIVRAISON, "BON DE LIVRAISON", Type.FACTURE, "FACTURE", Type.AVOIR, "AVOIR");
    /** Phrase d'arrêté ; le bon de livraison n'en a pas (pas de montant). */
    private static final Map<Type, String> ARRETES = Map.of(
            Type.DEVIS, "Arrêté le présent devis à la somme de :",
            Type.COMMANDE, "Arrêté le présent bon de commande à la somme de :",
            Type.FACTURE, "Arrêtée la présente facture à la somme de :",
            Type.AVOIR, "Arrêté le présent avoir à la somme de :");
    private static final Map<Type, String> LIBELLES = Map.of(Type.DEVIS, "Devis", Type.COMMANDE, "Commande",
            Type.LIVRAISON, "Bon de livraison", Type.FACTURE, "Facture", Type.AVOIR, "Avoir");

    private final SalesDocumentRepository docs;
    private final SettingsService settings;
    private final TemplateEngine engine;
    private final String defaultLogo;

    public SalesDocumentPdfService(SalesDocumentRepository docs, SettingsService settings,
                                   @Value("${app.documents.logo-par-defaut:}") String defaultLogo) {
        this.docs = docs;
        this.settings = settings;
        this.defaultLogo = defaultLogo;
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("pdf/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        this.engine = new TemplateEngine();
        this.engine.setTemplateResolver(resolver);
    }

    /** Nom du fichier proposé au téléchargement, ex. FAC-2026-000012.pdf. */
    public record Pdf(String fileName, byte[] content) {
    }

    @PreAuthorize("hasAuthority('SALES_VIEW')")
    @Transactional(readOnly = true)
    public Pdf generate(Long id) {
        SalesDocument d = docs.findById(id).orElseThrow(() -> new NotFoundException("Document introuvable."));
        CompanySettings s = settings.find().orElseThrow(() -> new BusinessException(
                "Impossible de générer le PDF : les informations de l'huilerie ne sont pas saisies (Paramètres › Huilerie)."));
        checkRequired(d, s);
        String html = engine.process("document-vente", new Context(Locale.FRANCE, model(d, s)));
        String name = (d.getNumero() != null ? d.getNumero() : LIBELLES.get(d.getType()) + "-brouillon-" + d.getId())
                .replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        return new Pdf(name, render(html));
    }

    // ---------------------------------------------------------------- contrôles

    static void checkRequired(SalesDocument d, CompanySettings s) {
        List<String> manquants = new ArrayList<>();
        if (blank(s.getRaisonSociale())) manquants.add("la raison sociale");
        if (blank(s.getMatriculeFiscal())) manquants.add("le matricule fiscal");
        if (blank(s.getAdresse())) manquants.add("l'adresse");
        if (d.getRegime().isExport() && blank(s.getMentionExport())) manquants.add("la mention export");
        if (d.getRegime() == SalesDocument.Regime.ECHANTILLON && blank(s.getMentionEchantillon())) {
            manquants.add("la mention échantillon");
        }
        if (!manquants.isEmpty()) {
            throw new BusinessException("Impossible de générer le PDF : renseignez " + join(manquants)
                    + " dans Paramètres › Huilerie.");
        }
        if (d.getRegime().isExport() && blank(d.getClient().getPays())) {
            throw new BusinessException("Impossible de générer le PDF : document export sans pays du client. "
                    + "Renseignez le pays sur la fiche de « " + d.getClient().getNom() + " ».");
        }
    }

    private static String join(List<String> items) {
        if (items.size() == 1) return items.get(0);
        return String.join(", ", items.subList(0, items.size() - 1)) + " et " + items.get(items.size() - 1);
    }

    // ---------------------------------------------------------------- modèle

    Map<String, Object> model(SalesDocument d, CompanySettings s) {
        Devise devise = d.getDevise();
        boolean prix = d.getType() != Type.LIVRAISON;
        Map<String, Object> m = new HashMap<>();

        m.put("societe", map("raisonSociale", s.getRaisonSociale(), "rne", s.getRne(), "mf", s.getMatriculeFiscal(),
                "adresse", s.getAdresse(), "telephone", s.getTelephone(), "email", s.getEmail()));
        m.put("logo", logoDataUri());
        m.put("date", d.getDateDoc().format(DATE));
        m.put("titre", TITRES.get(d.getType()));
        m.put("sousTitre", switch (d.getRegime()) {
            case EXPORT -> "Export";
            case ECHANTILLON -> "Échantillons";
            case LOCAL -> null;
        });
        m.put("numero", d.getNumero());
        m.put("filigrane", d.getStatut() == Status.BROUILLON ? "BROUILLON" : d.getStatut() == Status.ANNULE ? "ANNULÉ" : null);

        var c = d.getClient();
        m.put("client", map("nom", c.getNom(), "adresse", c.getAdresse(), "mf", c.getMatriculeFiscal(), "pays", c.getPays()));

        List<Map<String, Object>> refs = new ArrayList<>();
        if (d.getSource() != null) {
            refs.add(map("label", LIBELLES.get(d.getSource().getType()) + " d'origine", "value",
                    Objects.requireNonNullElse(d.getSource().getNumero(), "brouillon")));
        }
        if (d.getRefClient() != null) refs.add(map("label", "Réf. client", "value", d.getRefClient()));
        if (d.getEcheance() != null && d.getType() == Type.FACTURE) {
            refs.add(map("label", "Échéance", "value", d.getEcheance().format(DATE)));
        }
        if (d.getEcheance() != null && d.getType() == Type.DEVIS) {
            refs.add(map("label", "Valable jusqu'au", "value", d.getEcheance().format(DATE)));
        }
        if (prix) refs.add(map("label", "Devise", "value", devise.name()));
        m.put("references", refs);

        if (d.getRegime().isExport()) {
            List<Map<String, Object>> exp = new ArrayList<>();
            if (d.getPaysOrigine() != null) exp.add(map("label", "Pays d'origine", "value", d.getPaysOrigine()));
            exp.add(map("label", "Pays de destination", "value", c.getPays()));
            if (d.getIncoterm() != null) exp.add(map("label", "Incoterm", "value", d.getIncoterm()));
            if (d.getPoidsNet() != null) exp.add(map("label", "Poids net", "value", qty(d.getPoidsNet()) + " kg"));
            if (d.getPoidsBrut() != null) exp.add(map("label", "Poids brut", "value", qty(d.getPoidsBrut()) + " kg"));
            m.put("export", exp);
        }

        m.put("prix", prix);
        m.put("devise", devise.name());
        m.put("lignes", d.getLignes().stream().map(l -> map(
                "designation", l.getDesignation(),
                "lot", l.getLot() == null ? null : l.getLot().getNumero(),
                "codeSh", l.getCodeSh(),
                "unite", l.getUnite(),
                "quantite", qty(l.getQuantite()),
                "pu", money(l.getPrixUnitaire(), puDecimals(l.getPrixUnitaire(), devise)),
                "remise", l.getRemisePct().signum() == 0 ? "" : qty(l.getRemisePct()) + " %",
                "totalHt", money(l.getTotalHt(), devise.decimales()))).toList());

        if (prix) {
            List<Map<String, Object>> recap = new ArrayList<>();
            recap.add(map("label", "Total HT", "value", money(d.getTotalHt(), devise.decimales())));
            if (d.getRegime().isExport()) {
                recap.add(map("label", "TVA", "value", "Exonérée"));
            } else {
                tvaParTaux(d).forEach((taux, montant) -> recap.add(map("label", "TVA " + qty(taux) + " %",
                        "value", money(montant, devise.decimales()))));
            }
            if (d.getTimbre().signum() > 0) {
                recap.add(map("label", "Timbre fiscal", "value", money(d.getTimbre(), devise.decimales())));
            }
            m.put("recap", recap);
            m.put("totalTtc", money(d.getTotalTtc(), devise.decimales()) + " " + devise.symbole());
            String lettres = MontantEnLettres.enLettres(d.getTotalTtc(), devise);
            m.put("arrete", ARRETES.get(d.getType()));
            m.put("montantLettres", Character.toUpperCase(lettres.charAt(0)) + lettres.substring(1) + ".");
        }

        m.put("mentions", mentions(d, s));
        m.put("livraison", d.getType() == Type.LIVRAISON);
        m.put("piedDePage", footer(s));
        return m;
    }

    /** TVA par taux, dans l'ordre croissant des taux. */
    static SortedMap<BigDecimal, BigDecimal> tvaParTaux(SalesDocument d) {
        return d.getLignes().stream().collect(Collectors.groupingBy(l -> l.getTauxTva().stripTrailingZeros(), TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, SalesDocument.Line::getTotalTva, BigDecimal::add)));
    }

    private static List<String> mentions(SalesDocument d, CompanySettings s) {
        List<String> out = new ArrayList<>();
        if (d.getType() == Type.FACTURE) {
            addLines(out, s.getMentionsFacture());
            if (d.getClient().getConditionsPaiement() != null) {
                out.add("Conditions de paiement : " + d.getClient().getConditionsPaiement() + ".");
            }
        }
        if (d.getType() == Type.DEVIS) {
            addLines(out, s.getMentionsDevis());
        }
        if (d.getRegime().isExport()) {
            out.add(s.getMentionExport());
        }
        if (d.getRegime() == SalesDocument.Regime.ECHANTILLON) {
            out.add(s.getMentionEchantillon());
        }
        if (d.getType() == Type.AVOIR && d.getSource() != null) {
            out.add("Avoir sur la facture " + d.getSource().getNumero() + (d.getNotes() != null ? " — motif : " + d.getNotes() : "") + ".");
        } else if (d.getNotes() != null) {
            out.add(d.getNotes());
        }
        return out;
    }

    private static void addLines(List<String> out, String text) {
        if (text != null) {
            text.lines().map(String::trim).filter(t -> !t.isEmpty()).forEach(out::add);
        }
    }

    /** Coordonnées légales sur une ligne (téléphone et e-mail sont déjà dans l'en-tête). */
    private static String footer(CompanySettings s) {
        List<String> parts = new ArrayList<>();
        parts.add(s.getRaisonSociale());
        parts.add(s.getAdresse());
        if (s.getRne() != null) parts.add("RNE : " + s.getRne());
        parts.add("MF : " + s.getMatriculeFiscal());
        return String.join(" — ", parts);
    }

    private String logoDataUri() {
        Optional<SettingsService.Logo> logo = settings.logo();
        if (logo.isPresent()) {
            return "data:" + logo.get().type() + ";base64," + Base64.getEncoder().encodeToString(logo.get().data());
        }
        if (!blank(defaultLogo)) {
            try {
                byte[] data = Files.readAllBytes(Path.of(defaultLogo));
                String type = defaultLogo.toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
                return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(data);
            } catch (IOException e) {
                return null;   // logo facultatif : le document reste valable sans lui
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- rendu

    private static byte[] render(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            b.useFont(() -> font("DejaVuSans.ttf"), FONT, 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            b.useFont(() -> font("DejaVuSans-Bold.ttf"), FONT, 700, BaseRendererBuilder.FontStyle.NORMAL, true);
            b.withHtmlContent(html, null);
            b.toStream(out);
            b.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Génération du PDF impossible", e);
        }
    }

    private static InputStream font(String file) {
        InputStream in = SalesDocumentPdfService.class.getResourceAsStream("/fonts/" + file);
        if (in == null) {
            throw new IllegalStateException("Police manquante : /fonts/" + file);
        }
        return in;
    }

    // ---------------------------------------------------------------- format français

    /** 1 234,500 (séparateur de milliers : espace insécable). */
    static String money(BigDecimal v, int decimals) {
        DecimalFormat f = new DecimalFormat(decimals == 0 ? "#,##0" : "#,##0." + "0".repeat(decimals), symbols());
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f.format(v);
    }

    /** Quantité sans zéros inutiles : 5 ; 2,5 ; 1 250,125. */
    static String qty(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("#,##0.###", symbols());
        return f.format(v);
    }

    /** Prix unitaire : précision de la devise, ou 3 décimales si le prix en a plus (prix au litre en DT). */
    private static int puDecimals(BigDecimal pu, Devise devise) {
        return Math.max(devise.decimales(), Math.min(3, pu.stripTrailingZeros().scale()));
    }

    private static DecimalFormatSymbols symbols() {
        DecimalFormatSymbols sy = new DecimalFormatSymbols(Locale.FRANCE);
        sy.setGroupingSeparator(' ');
        sy.setDecimalSeparator(',');
        return sy;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /** Map tolérant les valeurs nulles (champs facultatifs non affichés). */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
