package com.huilerie.erp.catalog;

import com.huilerie.erp.catalog.dto.ArticleDto;
import com.huilerie.erp.catalog.dto.CategoryDto;
import com.huilerie.erp.catalog.dto.StatusRequest;
import com.huilerie.erp.catalog.dto.UnitDto;
import com.huilerie.erp.common.response.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @GetMapping("/units")
    public List<UnitDto.Response> units() {
        return service.listUnits();
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitDto.Response createUnit(@Valid @RequestBody UnitDto.Request r) {
        return service.saveUnit(null, r);
    }

    @PutMapping("/units/{id}")
    public UnitDto.Response updateUnit(@PathVariable Long id, @Valid @RequestBody UnitDto.Request r) {
        return service.saveUnit(id, r);
    }

    @PatchMapping("/units/{id}/status")
    public UnitDto.Response unitStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setUnitStatus(id, r.actif());
    }

    @GetMapping("/categories")
    public List<CategoryDto.Response> categories() {
        return service.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto.Response createCategory(@Valid @RequestBody CategoryDto.Request r) {
        return service.saveCategory(null, r);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto.Response updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDto.Request r) {
        return service.saveCategory(id, r);
    }

    @PatchMapping("/categories/{id}/status")
    public CategoryDto.Response categoryStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setCategoryStatus(id, r.actif());
    }

    @GetMapping("/articles")
    public PageResponse<ArticleDto.Response> articles(@RequestParam(required = false) String q,
                                                      @RequestParam(required = false) ArticleType type,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) Boolean actif,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return service.searchArticles(q, type, categoryId, actif, page, size);
    }

    @GetMapping("/articles/{id}")
    public ArticleDto.Response article(@PathVariable Long id) {
        return service.getArticle(id);
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleDto.Response createArticle(@Valid @RequestBody ArticleDto.Request r) {
        return service.saveArticle(null, r);
    }

    @PutMapping("/articles/{id}")
    public ArticleDto.Response updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleDto.Request r) {
        return service.saveArticle(id, r);
    }

    @PatchMapping("/articles/{id}/status")
    public ArticleDto.Response articleStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return service.setArticleStatus(id, r.actif());
    }
}
