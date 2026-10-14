package com.huilerie.erp.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByReferenceIgnoreCase(String reference);

    boolean existsByReferenceIgnoreCaseAndIdNot(String reference, Long id);

    boolean existsByCodeBarres(String codeBarres);

    boolean existsByCodeBarresAndIdNot(String codeBarres, Long id);

    long countByActifTrue();

    @Query("""
            select a from Article a left join a.categorie c
            where (:q is null or lower(a.nom) like concat('%', :q, '%')
                   or lower(a.reference) like concat('%', :q, '%')
                   or a.codeBarres = :q)
              and (:type is null or a.type = :type)
              and (:categoryId is null or c.id = :categoryId)
              and (:actif is null or a.actif = :actif)
            """)
    Page<Article> search(@Param("q") String q, @Param("type") ArticleType type,
                         @Param("categoryId") Long categoryId, @Param("actif") Boolean actif, Pageable pageable);

    List<Article> findByVendableTrueAndActifTrueOrderByNomAsc();

    List<Article> findByActifTrueAndSeuilMinIsNotNull();
}
