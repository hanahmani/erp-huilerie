package com.huilerie.erp.salesdocs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huilerie.erp.SalesFixtures;
import com.huilerie.erp.TestUsers;
import com.huilerie.erp.catalog.Article;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.huilerie.erp.MockMvcSupport.bearer;
import static com.huilerie.erp.MockMvcSupport.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * PDF des documents commerciaux, de bout en bout par l'API : paramètres, client, devis de 30 lignes,
 * conversion en facture, facture export en euros, bon de livraison. Données créées dans la base H2 des tests
 * puis effacées. Les PDF et leurs pages en PNG sont écrits dans target/apercu-pdf pour contrôle visuel.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesDocumentPdfTest {

    private static final Path OUT = Path.of("target", "apercu-pdf");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TestUsers testUsers;
    @Autowired SalesFixtures fx;

    String token;
    long tva19;
    long tva7;
    List<Article> articles;
    /** Nombre de pages du dernier PDF téléchargé. */
    int pages;

    @BeforeEach
    void setUp() throws Exception {
        testUsers.reset();
        testUsers.create("admin-pdf", false, "ADMIN");
        token = login(mvc, om, "admin-pdf", TestUsers.PASSWORD);
        tva19 = call(post("/api/settings/taxes"), Map.of("nom", "TVA test 19 %", "taux", "19"), 201).get("id").asLong();
        tva7 = call(post("/api/settings/taxes"), Map.of("nom", "TVA test 7 %", "taux", "7"), 201).get("id").asLong();
        articles = List.of(fx.article("T-EXTRA", "18.500", false), fx.article("T-VIERGE", "15.750", false),
                fx.article("T-LAMPANTE", "9.125", false));
        articles.forEach(a -> fx.receive(a, null, "1000"));
        Files.createDirectories(OUT);
    }

    @AfterEach
    void cleanUp() {
        testUsers.reset();
    }

    // ---------------------------------------------------------------- refus

    @Test
    void pdfRefuseTantQueLesInformationsLegalesManquent() throws Exception {
        long client = client("Client test local", null);
        long devis = call(post("/api/sales-documents"), doc("DEVIS", client, lines(1), null), 201).get("id").asLong();

        assertThat(error(devis)).contains("informations de l'huilerie ne sont pas saisies");

        Map<String, Object> s = settings();
        s.put("matriculeFiscal", null);
        s.put("adresse", " ");
        call(put("/api/settings"), s, 200);
        assertThat(error(devis)).isEqualTo("Impossible de générer le PDF : renseignez le matricule fiscal et l'adresse dans Paramètres › Huilerie.");
    }

    // ---------------------------------------------------------------- devis → facture, 30 lignes

    @Test
    void devisDeTrenteLignesPuisFactureConvertie() throws Exception {
        call(put("/api/settings"), settings(), 200);
        uploadLogo();
        long client = client("Client test local", null);

        JsonNode devis = call(post("/api/sales-documents"), doc("DEVIS", client, lines(30), null), 201);
        devis = call(post("/api/sales-documents/" + devis.get("id").asLong() + "/validate"), null, 200);
        String numDevis = devis.get("numero").asText();
        assertThat(numDevis).matches("DEV-\\d{4}-\\d{6}");
        assertThat(devis.get("timbre").decimalValue()).isEqualByComparingTo("0");   // pas de timbre sur un devis

        String devisText = pdf(devis.get("id").asLong(), "devis-30-lignes");
        assertThat(devisText).contains("DEVIS", "N° : " + numDevis, "Arrêté le présent devis à la somme de :",
                "Devis valable 30 jours.", "TVA 7 %", "TVA 19 %");
        assertThat(pages).as("30 lignes : le tableau continue sur une 2e page").isEqualTo(2);
        assertThat(devisText).doesNotContain("Timbre fiscal");

        // « Convertir en facture » : mêmes lignes, référence du devis d'origine.
        JsonNode fac = call(post("/api/sales-documents/" + devis.get("id").asLong() + "/transform"), Map.of("cible", "FACTURE"), 200);
        assertThat(fac.get("source").get("numero").asText()).isEqualTo(numDevis);
        assertThat(fac.get("lignes")).hasSize(30);
        fac = call(post("/api/sales-documents/" + fac.get("id").asLong() + "/validate"), null, 200);
        assertThat(fac.get("timbre").decimalValue()).isEqualByComparingTo("1.000");
        assertThat(fac.get("totalTtc").decimalValue())
                .isEqualByComparingTo(fac.get("totalHt").decimalValue().add(fac.get("totalTva").decimalValue()).add(fac.get("timbre").decimalValue()));

        String facText = pdf(fac.get("id").asLong(), "facture-30-lignes");
        assertThat(facText).contains("FACTURE", "Devis d'origine", numDevis, "Arrêtée la présente facture à la somme de :",
                "Timbre fiscal", "Conditions de paiement : 30 jours fin de mois.", "Paiement par virement bancaire.",
                "Cachet et signature");
        assertThat(pages).isEqualTo(2);
        assertThat(facText).contains(capitalize(fac.get("montantEnLettres").asText()));
    }

    // ---------------------------------------------------------------- export en euros

    @Test
    void factureExportEnEurosSansTvaNiTimbre() throws Exception {
        call(put("/api/settings"), settings(), 200);
        long client = client("Client test export", "France");

        Map<String, Object> body = doc("FACTURE", client, List.of(
                line(articles.get(0), "120", "6.50", "0", tva19, "1509.20"),
                line(articles.get(1), "40", "5.25", "5", tva19, "1509.20")), "EUR");
        body.put("regime", "EXPORT");
        body.put("paysOrigine", "Tunisie");
        body.put("incoterm", "FOB");
        body.put("poidsNet", "160");
        body.put("poidsBrut", "174.5");
        JsonNode fac = call(post("/api/sales-documents"), body, 201);
        assertThat(fac.get("totalTva").decimalValue()).isEqualByComparingTo("0");   // exonéré même si une TVA est choisie
        fac = call(post("/api/sales-documents/" + fac.get("id").asLong() + "/validate"), null, 200);
        assertThat(fac.get("timbre").decimalValue()).isEqualByComparingTo("0");
        // 120 × 6,50 = 780,00 ; 40 × 5,25 × 0,95 = 199,50
        assertThat(fac.get("totalTtc").decimalValue()).isEqualByComparingTo("979.50");
        assertThat(fac.get("montantEnLettres").asText()).isEqualTo("neuf cent soixante-dix-neuf euros et cinquante centimes");

        String text = pdf(fac.get("id").asLong(), "facture-export-eur");
        assertThat(pages).isEqualTo(1);
        assertThat(text).contains("FACTURE", "Export", "INFORMATIONS EXPORT", "Incoterm", "FOB", "Pays d'origine", "Tunisie",
                "France", "Code SH : 1509.20", "Exonéré de TVA – export", "Exonérée", "979,50 €",
                "Neuf cent soixante-dix-neuf euros et cinquante centimes.");
        assertThat(text).doesNotContain("Timbre fiscal");

        // Prix à 3 décimales refusé en euros ; encaissement en devise refusé (pas de taux de change).
        Map<String, Object> bad = doc("FACTURE", client, List.of(line(articles.get(0), "1", "6.505", "0", null, null)), "EUR");
        assertThat(call(post("/api/sales-documents"), bad, 409).get("message").asText()).contains("2 décimales au maximum en EUR");
        assertThat(call(post("/api/sales-documents/" + fac.get("id").asLong() + "/pay"),
                Map.of("mode", "VIREMENT", "montant", "10"), 409).get("message").asText()).contains("taux de change");
    }

    @Test
    void echantillonsExigentLaMentionEtLePaysDuClient() throws Exception {
        Map<String, Object> s = settings();
        s.put("mentionEchantillon", null);
        call(put("/api/settings"), s, 200);
        long sansPays = client("Client test sans pays", null);
        Map<String, Object> body = doc("FACTURE", sansPays, lines(2), "EUR");
        body.put("regime", "ECHANTILLON");
        long id = call(post("/api/sales-documents"), body, 201).get("id").asLong();
        assertThat(error(id)).contains("la mention échantillon");

        call(put("/api/settings"), settings(), 200);
        assertThat(error(id)).contains("sans pays du client");
    }

    // ---------------------------------------------------------------- bon de livraison

    @Test
    void bonDeLivraisonSansPrixNiArrete() throws Exception {
        call(put("/api/settings"), settings(), 200);
        long client = client("Client test local", null);
        JsonNode bl = call(post("/api/sales-documents"), doc("LIVRAISON", client, lines(3), null), 201);
        String text = pdf(bl.get("id").asLong(), "bon-de-livraison-brouillon");
        assertThat(text).contains("BON DE LIVRAISON", "brouillon (attribué à la validation)", "Reçu conforme", "Cachet et signature");
        assertThat(text).doesNotContain("Arrêté", "Total TTC", "P.U HT");
    }

    // ---------------------------------------------------------------- outils

    private Map<String, Object> settings() {
        Map<String, Object> s = new HashMap<>();
        s.put("raisonSociale", "Huilerie Test SARL");
        s.put("adresse", "Adresse de test, 0000 Ville test");
        s.put("telephone", "00 000 000");
        s.put("email", "test@example.invalid");
        s.put("matriculeFiscal", "0000000/T/A/M/000");
        s.put("rne", "0000000T");
        s.put("mentionsFacture", "Paiement par virement bancaire.\nPénalités de retard selon la réglementation en vigueur.");
        s.put("mentionsDevis", "Devis valable 30 jours.");
        s.put("mentionExport", "Exonéré de TVA – export");
        s.put("mentionEchantillon", "Échantillons sans valeur commerciale.");
        s.put("timbreFiscal", "1.000");
        s.put("venteCreditAutorisee", false);
        return s;
    }

    private long client(String nom, String pays) throws Exception {
        Map<String, Object> c = new HashMap<>();
        c.put("type", "PROFESSIONNEL");
        c.put("nom", nom);
        c.put("adresse", "Adresse client test");
        c.put("matriculeFiscal", "1111111/T/A/M/000");
        c.put("conditionsPaiement", "30 jours fin de mois");
        c.put("pays", pays);
        return call(post("/api/clients"), c, 201).get("id").asLong();
    }

    private Map<String, Object> line(Article a, String q, String pu, String remise, Long taxId, String codeSh) {
        Map<String, Object> l = new HashMap<>();
        l.put("articleId", a.getId());
        l.put("quantite", q);
        l.put("prixUnitaire", pu);
        l.put("remisePct", remise);
        l.put("taxId", taxId);
        l.put("enLitres", false);
        l.put("codeSh", codeSh);
        return l;
    }

    /** n lignes variées : trois articles, TVA 19 % ou 7 %, une remise sur une ligne sur quatre. */
    private List<Map<String, Object>> lines(int n) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Article a = articles.get(i % 3);
            out.add(line(a, String.valueOf(1 + i % 7), a.getPrixVente().toPlainString(), i % 4 == 0 ? "10" : "0",
                    i % 2 == 0 ? tva19 : tva7, null));
        }
        return out;
    }

    private Map<String, Object> doc(String type, long client, List<Map<String, Object>> lignes, String devise) {
        Map<String, Object> d = new HashMap<>();
        d.put("type", type);
        d.put("clientId", client);
        d.put("dateDoc", LocalDate.now().toString());
        d.put("echeance", LocalDate.now().plusDays(30).toString());
        d.put("refClient", "BC-TEST-1");
        d.put("lignes", lignes);
        d.put("devise", devise);
        return d;
    }

    private JsonNode call(MockHttpServletRequestBuilder req, Object body, int expected) throws Exception {
        req.header(HttpHeaders.AUTHORIZATION, bearer(token));
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        }
        MvcResult r = mvc.perform(req).andReturn();
        String json = r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(r.getResponse().getStatus()).as(json).isEqualTo(expected);
        return om.readTree(json);
    }

    private String error(long id) throws Exception {
        return call(get("/api/sales-documents/" + id + "/pdf"), null, 409).get("message").asText();
    }

    /** Télécharge le PDF, l'écrit avec une image PNG par page, renvoie son texte. */
    private String pdf(long id, String name) throws Exception {
        MvcResult r = mvc.perform(get("/api/sales-documents/" + id + "/pdf").header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(r.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(r.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline; filename=");
        byte[] bytes = r.getResponse().getContentAsByteArray();
        Files.write(OUT.resolve(name + ".pdf"), bytes);
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            pages = pdf.getNumberOfPages();
            PDFRenderer renderer = new PDFRenderer(pdf);
            for (int p = 0; p < pdf.getNumberOfPages(); p++) {
                ImageIO.write(renderer.renderImageWithDPI(p, 110, ImageType.RGB), "png", OUT.resolve(name + "-p" + (p + 1) + ".png").toFile());
            }
            return new PDFTextStripper().getText(pdf).replace(' ', ' ');
        }
    }

    /** Logo de test dessiné à la volée (aucun fichier réel). */
    private void uploadLogo() throws Exception {
        BufferedImage img = new BufferedImage(360, 140, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(90, 110, 40));
        g.fillOval(8, 8, 124, 124);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
        g.drawString("LOGO", 150, 86);
        g.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(img, "png", png);
        MockMultipartFile file = new MockMultipartFile("fichier", "logo.png", "image/png", png.toByteArray());
        mvc.perform(multipart(HttpMethod.PUT, "/api/settings/logo").file(file).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn();
        assertThat(call(get("/api/settings"), null, 200).get("logo").asBoolean()).isTrue();
        // logo public : lisible sans jeton
        assertThat(mvc.perform(get("/api/settings/logo")).andReturn().getResponse().getContentType()).isEqualTo("image/png");
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
