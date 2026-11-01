package com.huilerie.erp.inventory;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.Article;
import com.huilerie.erp.catalog.ArticleRepository;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.inventory.dto.TankDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puits et silos de stockage d'huile. Chaque puits porte un lot ENTREPRISE (même numéro que le code) :
 * les entrées s'y ajoutent, les ventes en caisse en sortent, la capacité n'est jamais dépassée.
 */
@Service
public class TankService {

    private final StorageTankRepository tanks;
    private final StockLotRepository lots;
    private final StockBalanceRepository balances;
    private final ArticleRepository articles;
    private final StockService stock;
    private final AuditService audit;

    public TankService(StorageTankRepository tanks, StockLotRepository lots, StockBalanceRepository balances,
                       ArticleRepository articles, StockService stock, AuditService audit) {
        this.tanks = tanks;
        this.lots = lots;
        this.balances = balances;
        this.articles = articles;
        this.stock = stock;
        this.audit = audit;
    }

    @PreAuthorize("hasAuthority('STOCK_VIEW') or hasAuthority('PRESSING_VIEW')")
    @Transactional(readOnly = true)
    public List<TankDto.Response> list() {
        return tanks.findAllByOrderByCodeAsc().stream().map(this::toDto).toList();
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public TankDto.Response create(TankDto.Request r) {
        if (r.code() == null || r.code().isBlank()) {
            throw new BusinessException("Le code du puits est obligatoire.");
        }
        if (r.articleId() == null) {
            throw new BusinessException("Choisissez l'huile stockée dans ce puits.");
        }
        String code = r.code().trim().replaceAll("\\s+", " ").toUpperCase();
        if (tanks.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Un puits porte déjà le code " + code + ".");
        }
        Article article = articles.findById(r.articleId()).orElseThrow(() -> new NotFoundException("Article introuvable."));
        if (!article.isActif()) {
            throw new BusinessException("Cet article est désactivé.");
        }
        if (!article.isSuiviLot()) {
            throw new BusinessException("L'huile d'un puits doit être un article suivi par lot.");
        }
        if (lots.existsByArticleIdAndNumeroIgnoreCase(article.getId(), code)) {
            throw new BusinessException("Un lot " + code + " existe déjà pour cet article : choisissez un autre code.");
        }
        StockLot lot = lots.save(new StockLot(article, code, LotOwner.ENTREPRISE, null));
        StorageTank t = tanks.save(new StorageTank(code, r.nom().trim(), r.type(), article, lot,
                Money.strict(r.capacite(), "Capacité")));
        audit.log(SecurityUtils.username(), "TANK_CREATE", "StorageTank", String.valueOf(t.getId()),
                code + " " + t.getCapacite().toPlainString());
        return toDto(t);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public TankDto.Response update(Long id, TankDto.Request r) {
        StorageTank t = tanks.lockById(id).orElseThrow(() -> new NotFoundException("Puits introuvable."));
        BigDecimal capacite = Money.strict(r.capacite(), "Capacité");
        BigDecimal quantite = quantity(t);
        if (capacite.compareTo(quantite) < 0) {
            throw new BusinessException("Le puits contient déjà " + quantite.toPlainString() + " : la capacité ne peut pas être inférieure.");
        }
        t.update(r.nom().trim(), r.type(), capacite);
        audit.log(SecurityUtils.username(), "TANK_UPDATE", "StorageTank", String.valueOf(id),
                t.getCode() + " " + capacite.toPlainString());
        return toDto(t);
    }

    @PreAuthorize("hasAuthority('STOCK_ADJUST')")
    @Transactional
    public TankDto.Response setStatus(Long id, boolean actif) {
        StorageTank t = tanks.findById(id).orElseThrow(() -> new NotFoundException("Puits introuvable."));
        t.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "ACTIVATE" : "DEACTIVATE", "StorageTank", String.valueOf(id), t.getCode());
        return toDto(t);
    }

    /**
     * Verse de l'huile dans un puits, dans la transaction de l'appelant. Le puits est verrouillé :
     * refus s'il est inactif ou si la quantité dépasse la place restante.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StorageTank fill(Long tankId, BigDecimal quantite, MovementType type, String motif, String documentType,
                            Long documentId) {
        StorageTank t = tanks.lockById(tankId).orElseThrow(() -> new NotFoundException("Puits introuvable."));
        if (!t.isActif()) {
            throw new BusinessException("Le puits " + t.getCode() + " est désactivé.");
        }
        BigDecimal place = t.getCapacite().subtract(quantity(t));
        if (quantite.compareTo(place) > 0) {
            throw new BusinessException("Le puits " + t.getCode() + " est plein : place restante "
                    + place.toPlainString() + " " + t.getArticle().getUnite().getCode() + ".");
        }
        stock.apply(t.getArticle(), t.getLot(), type, quantite, motif, documentType, documentId);
        return t;
    }

    private BigDecimal quantity(StorageTank t) {
        return Money.of(balances.quantityOfLot(t.getLot().getId()));
    }

    private TankDto.Response toDto(StorageTank t) {
        return TankDto.Response.from(t, quantity(t));
    }
}
