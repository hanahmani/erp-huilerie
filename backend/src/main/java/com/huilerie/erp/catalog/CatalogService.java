package com.huilerie.erp.catalog;

import com.huilerie.erp.audit.AuditService;
import com.huilerie.erp.catalog.dto.ArticleDto;
import com.huilerie.erp.catalog.dto.CategoryDto;
import com.huilerie.erp.catalog.dto.UnitDto;
import com.huilerie.erp.common.Money;
import com.huilerie.erp.common.SecurityUtils;
import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.exception.BusinessException;
import com.huilerie.erp.common.exception.NotFoundException;
import com.huilerie.erp.common.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final UnitRepository units;
    private final CategoryRepository categories;
    private final ArticleRepository articles;
    private final AuditService audit;

    public CatalogService(UnitRepository units, CategoryRepository categories, ArticleRepository articles,
                          AuditService audit) {
        this.units = units;
        this.categories = categories;
        this.articles = articles;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ unités

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<UnitDto.Response> listUnits() {
        return units.findAllByOrderByLibelleAsc().stream().map(UnitDto.Response::from).toList();
    }

    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    @Transactional
    public UnitDto.Response saveUnit(Long id, UnitDto.Request r) {
        String code = r.code().trim();
        boolean duplicate = id == null ? units.existsByCodeIgnoreCase(code) : units.existsByCodeIgnoreCaseAndIdNot(code, id);
        if (duplicate) {
            throw new BusinessException("Une unité porte déjà ce code.");
        }
        Unit u = id == null ? new Unit(code, r.libelle().trim()) : unit(id);
        u.setCode(code);
        u.setLibelle(r.libelle().trim());
        u = units.save(u);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "Unit", String.valueOf(u.getId()), code);
        return UnitDto.Response.from(u);
    }

    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    @Transactional
    public UnitDto.Response setUnitStatus(Long id, boolean actif) {
        Unit u = unit(id);
        u.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "ACTIVATE" : "DEACTIVATE", "Unit", String.valueOf(id), null);
        return UnitDto.Response.from(u);
    }

    // ------------------------------------------------------------------ catégories

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<CategoryDto.Response> listCategories() {
        return categories.findAllByOrderByNomAsc().stream().map(CategoryDto.Response::from).toList();
    }

    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    @Transactional
    public CategoryDto.Response saveCategory(Long id, CategoryDto.Request r) {
        String nom = r.nom().trim();
        boolean duplicate = id == null ? categories.existsByNomIgnoreCase(nom) : categories.existsByNomIgnoreCaseAndIdNot(nom, id);
        if (duplicate) {
            throw new BusinessException("Une catégorie porte déjà ce nom.");
        }
        Category c = id == null ? new Category(nom, Text.clean(r.description())) : category(id);
        c.setNom(nom);
        c.setDescription(Text.clean(r.description()));
        c = categories.save(c);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "Category", String.valueOf(c.getId()), nom);
        return CategoryDto.Response.from(c);
    }

    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    @Transactional
    public CategoryDto.Response setCategoryStatus(Long id, boolean actif) {
        Category c = category(id);
        c.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "ACTIVATE" : "DEACTIVATE", "Category", String.valueOf(id), null);
        return CategoryDto.Response.from(c);
    }

    // ------------------------------------------------------------------ articles

    @PreAuthorize("hasAuthority('CATALOG_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<ArticleDto.Response> searchArticles(String q, ArticleType type, Long categoryId, Boolean actif,
                                                           int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("nom"));
        return PageResponse.of(articles.search(Text.search(q), type, categoryId, actif, pr), ArticleDto.Response::from);
    }

    @PreAuthorize("hasAuthority('CATALOG_VIEW')")
    @Transactional(readOnly = true)
    public ArticleDto.Response getArticle(Long id) {
        return ArticleDto.Response.from(article(id));
    }

    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    @Transactional
    public ArticleDto.Response saveArticle(Long id, ArticleDto.Request r) {
        String reference = r.reference().trim().toUpperCase();
        String barcode = Text.clean(r.codeBarres());
        if (id == null ? articles.existsByReferenceIgnoreCase(reference)
                : articles.existsByReferenceIgnoreCaseAndIdNot(reference, id)) {
            throw new BusinessException("Un article porte déjà cette référence.");
        }
        if (barcode != null && (id == null ? articles.existsByCodeBarres(barcode)
                : articles.existsByCodeBarresAndIdNot(barcode, id))) {
            throw new BusinessException("Ce code-barres est déjà attribué à un autre article.");
        }
        if (r.vendable() && r.prixVente() == null) {
            throw new BusinessException("Un article vendable doit avoir un prix de vente.");
        }
        Unit unit = unit(r.uniteId());
        if (!unit.isActif()) {
            throw new BusinessException("Cette unité est désactivée.");
        }
        Category cat = r.categorieId() == null ? null : category(r.categorieId());

        Article a = id == null ? new Article() : article(id);
        if (id != null && a.isSuiviLot() != r.suiviLot()) {
            throw new BusinessException("Le suivi par lot ne peut pas être modifié après la création de l'article.");
        }
        a.setReference(reference);
        a.setNom(r.nom().trim());
        a.setDescription(Text.clean(r.description()));
        a.setType(r.type());
        a.setCategorie(cat);
        a.setUnite(unit);
        a.setContenance(Money.strict(r.contenance(), "Contenance"));
        a.setPrixAchat(Money.strict(r.prixAchat(), "Prix d'achat"));
        a.setPrixVente(Money.strict(r.prixVente(), "Prix de vente"));
        a.setSeuilMin(Money.strict(r.seuilMin(), "Seuil minimal"));
        a.setStockMax(Money.strict(r.stockMax(), "Stock maximum"));
        if (a.getSeuilMin() != null && a.getStockMax() != null && a.getStockMax().compareTo(a.getSeuilMin()) < 0) {
            throw new BusinessException("Le stock maximum doit être supérieur au seuil minimal.");
        }
        a.setCodeBarres(barcode);
        a.setVendable(r.vendable());
        a.setSuiviLot(r.suiviLot());
        a = articles.save(a);
        audit.log(SecurityUtils.username(), id == null ? "CREATE" : "UPDATE", "Article", String.valueOf(a.getId()), reference);
        return ArticleDto.Response.from(a);
    }

    @PreAuthorize("hasAuthority('CATALOG_MANAGE')")
    @Transactional
    public ArticleDto.Response setArticleStatus(Long id, boolean actif) {
        Article a = article(id);
        a.setActif(actif);
        audit.log(SecurityUtils.username(), actif ? "ACTIVATE" : "DEACTIVATE", "Article", String.valueOf(id), null);
        return ArticleDto.Response.from(a);
    }

    // ------------------------------------------------------------------ utilitaires

    public Article article(Long id) {
        return articles.findById(id).orElseThrow(() -> new NotFoundException("Article introuvable."));
    }

    private Unit unit(Long id) {
        return units.findById(id).orElseThrow(() -> new NotFoundException("Unité introuvable."));
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Catégorie introuvable."));
    }
}
