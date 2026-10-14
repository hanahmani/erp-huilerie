package com.huilerie.erp.search;

import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Recherche globale par code, nom, référence ou numéro ; résultats filtrés selon les permissions. */
@Service
public class SearchService {

    public record Hit(String type, String titre, String detail, String lien) {
    }

    private static final int LIMIT = 6;
    private final EntityManager em;

    public SearchService(EntityManager em) {
        this.em = em;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<Hit> search(String raw) {
        String q = Text.search(raw);
        List<Hit> out = new ArrayList<>();
        if (q == null || q.length() < 2) {
            return out;
        }
        if (SecurityUtils.has("CLIENT_VIEW")) {
            rows("select c.code, c.nom, c.telephone from Client c where lower(c.nom) like :q or lower(c.code) like :q or c.telephone like :q", q)
                    .forEach(r -> out.add(new Hit("Client", (String) r[1], r[0] + (r[2] == null ? "" : " · " + r[2]), "/clients")));
        }
        if (SecurityUtils.has("SUPPLIER_VIEW")) {
            rows("select s.code, s.nom, s.telephone from Supplier s where lower(s.nom) like :q or lower(s.code) like :q", q)
                    .forEach(r -> out.add(new Hit("Fournisseur", (String) r[1], (String) r[0], "/fournisseurs")));
        }
        if (SecurityUtils.has("CATALOG_VIEW")) {
            rows("select a.reference, a.nom, a.codeBarres from Article a where lower(a.nom) like :q or lower(a.reference) like :q or a.codeBarres like :q", q)
                    .forEach(r -> out.add(new Hit("Article", (String) r[1], (String) r[0], "/catalogue")));
        }
        if (SecurityUtils.has("STOCK_VIEW")) {
            rows("select l.numero, a.nom, l.proprietaire from StockLot l join l.article a where lower(l.numero) like :q", q)
                    .forEach(r -> out.add(new Hit("Lot", (String) r[0], r[1] + " · " + r[2], "/stock")));
        }
        if (SecurityUtils.has("SALES_VIEW")) {
            rows("select s.numeroTicket, s.total, s.dateVente from Sale s where lower(s.numeroTicket) like :q", q)
                    .forEach(r -> out.add(new Hit("Ticket", (String) r[0], r[1] + " DT", "/ventes/historique")));
            rows("select d.numero, c.nom, d.type from SalesDocument d join d.client c where lower(d.numero) like :q", q)
                    .forEach(r -> out.add(new Hit("Document de vente", (String) r[0], r[2] + " · " + r[1], "/ventes/" + linkOf(String.valueOf(r[2])))));
        }
        if (SecurityUtils.has("PURCHASE_VIEW")) {
            rows("select p.numero, f.nom, p.refFacture from Purchase p join p.fournisseur f where lower(p.numero) like :q or lower(p.refFacture) like :q", q)
                    .forEach(r -> out.add(new Hit("Achat", (String) r[0], (String) r[1], "/achats")));
        }
        if (SecurityUtils.has("PRESSING_VIEW")) {
            rows("select p.numero, c.nom, p.statut from Pressing p join p.client c where lower(p.numero) like :q or lower(c.nom) like :q", q)
                    .forEach(r -> out.add(new Hit("Pressage", (String) r[0], r[1] + " · " + r[2], "/pressage")));
        }
        if (SecurityUtils.has("PRODUCTION_VIEW")) {
            rows("select o.numero, o.ligne, o.statut from ProductionOrder o where lower(o.numero) like :q", q)
                    .forEach(r -> out.add(new Hit("Production", (String) r[0], String.valueOf(r[2]), "/production")));
        }
        if (SecurityUtils.has("HR_VIEW")) {
            rows("select e.matricule, e.prenom, e.nom from Employee e where lower(e.nom) like :q or lower(e.prenom) like :q or lower(e.matricule) like :q", q)
                    .forEach(r -> out.add(new Hit("Employé", r[1] + " " + r[2], (String) r[0], "/rh")));
        }
        return out;
    }

    private static String linkOf(String type) {
        return switch (type) {
            case "DEVIS" -> "devis";
            case "COMMANDE" -> "commandes";
            case "LIVRAISON" -> "livraisons";
            default -> "factures";
        };
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String jpql, String q) {
        return em.createQuery(jpql).setParameter("q", "%" + q + "%").setMaxResults(LIMIT).getResultList();
    }
}
