package com.huilerie.erp.traceability;

import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.inventory.StockLot;
import com.huilerie.erp.inventory.StockLotRepository;
import com.huilerie.erp.inventory.StockMovementRepository;
import com.huilerie.erp.inventory.dto.StockDto;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Répond à la question : « d'où vient ce lot et où a-t-il été utilisé ou vendu ? »
 * Amont : réception d'achat, pressage, trituration (lots d'olives), conditionnement (lot de vrac).
 * Aval : trituration, conditionnement, ventes, remises au client.
 */
@Service
public class TraceabilityService {

    public record Link(String type, String document, Instant date, String description, BigDecimal quantite,
                       Long lotId, String lotNumero) {
    }

    public record Trace(Long lotId, String numero, String article, String proprietaire, String client, String statut,
                        List<Link> amont, List<Link> aval, List<StockDto.MovementResponse> mouvements) {
    }

    private final EntityManager em;
    private final StockLotRepository lots;
    private final StockMovementRepository movements;

    public TraceabilityService(EntityManager em, StockLotRepository lots, StockMovementRepository movements) {
        this.em = em;
        this.lots = lots;
        this.movements = movements;
    }

    @PreAuthorize("hasAuthority('STOCK_VIEW') or hasAuthority('PRODUCTION_VIEW')")
    @Transactional(readOnly = true)
    public Trace trace(Long lotId) {
        StockLot lot = lots.findById(lotId).orElseThrow(() -> new NotFoundException("Lot introuvable."));
        List<Link> up = new ArrayList<>();
        List<Link> down = new ArrayList<>();

        // ---- amont
        for (Object[] r : rows("""
                select g.numero, g.date, f.nom, l.quantite from GoodsReceiptLine l join l.receipt g
                join g.purchase p join p.fournisseur f where l.lot.id = :id""", lotId)) {
            up.add(new Link("Réception d'achat", (String) r[0], (Instant) r[1], "Fournisseur " + r[2], (BigDecimal) r[3], null, null));
        }
        for (Object[] r : rows("""
                select p.numero, p.dateReception, c.nom, p.poidsNet, p.variete from Pressing p join p.client c
                where p.lot.id = :id""", lotId)) {
            up.add(new Link("Pressage client", (String) r[0], (Instant) r[1],
                    "Olives de " + r[2] + (r[4] == null ? "" : " (" + r[4] + ")") + ", kg net", (BigDecimal) r[3], null, null));
        }
        for (Object[] r : rows("""
                select o.numero, o.termineLe, il.id, il.numero, i.quantite, a.nom
                from ProductionOrder o join o.entrees i join i.article a left join i.lot il where o.huileLot.id = :id""", lotId)) {
            up.add(new Link("Trituration", (String) r[0], (Instant) r[1], "Olives " + r[5] + ", kg",
                    (BigDecimal) r[4], (Long) r[2], (String) r[3]));
        }
        for (Object[] r : rows("""
                select o.numero, o.createdAt, s.id, s.numero, o.volumeUtilise from PackagingOrder o join o.sourceLot s
                where o.produitLot.id = :id and o.statut = 'VALIDE'""", lotId)) {
            up.add(new Link("Conditionnement", (String) r[0], (Instant) r[1], "Huile en vrac utilisée", (BigDecimal) r[4],
                    (Long) r[2], (String) r[3]));
        }

        // ---- aval
        for (Object[] r : rows("""
                select o.numero, o.demarreLe, h.id, h.numero, i.quantite from ProductionOrder o join o.entrees i
                left join o.huileLot h where i.lot.id = :id
                and o.statut <> com.huilerie.erp.production.ProductionOrder.Status.ANNULE""", lotId)) {
            down.add(new Link("Trituration", (String) r[0], (Instant) r[1], "Olives consommées, kg", (BigDecimal) r[4],
                    (Long) r[2], (String) r[3]));
        }
        for (Object[] r : rows("""
                select o.numero, o.createdAt, pl.id, pl.numero, o.quantiteProduite, p.nom from PackagingOrder o
                join o.produit p left join o.produitLot pl where o.sourceLot.id = :id and o.statut = 'VALIDE'""", lotId)) {
            down.add(new Link("Conditionnement", (String) r[0], (Instant) r[1], r[5] + " produits", (BigDecimal) r[4],
                    (Long) r[2], (String) r[3]));
        }
        for (Object[] r : rows("""
                select s.numeroTicket, s.dateVente, c.nom, l.quantite from SaleLine l join l.sale s left join s.client c
                where l.lot.id = :id and s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE""", lotId)) {
            down.add(new Link("Vente", (String) r[0], (Instant) r[1], r[2] == null ? "Client comptoir" : "Client " + r[2],
                    (BigDecimal) r[3], null, null));
        }
        for (Object[] r : rows("""
                select r.numero, r.date, r.remisA, r.quantite from Pressing p join p.remises r where p.lot.id = :id""", lotId)) {
            down.add(new Link("Remise au client", (String) r[0], (Instant) r[1], "Remis à " + r[2], (BigDecimal) r[3], null, null));
        }

        List<StockDto.MovementResponse> mvts = movements.search(null, lotId, null, null, null,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "id"))).map(StockDto.MovementResponse::from).getContent();

        return new Trace(lot.getId(), lot.getNumero(), lot.getArticle().getNom(), lot.getProprietaire().name(),
                lot.getClient() == null ? null : lot.getClient().getNom(), lot.getStatut().name(), up, down, mvts);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String jpql, Long id) {
        return em.createQuery(jpql).setParameter("id", id).getResultList();
    }
}
