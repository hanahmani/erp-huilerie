package com.huilerie.erp.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockLotRepository extends JpaRepository<StockLot, Long> {

    boolean existsByArticleIdAndNumeroIgnoreCase(Long articleId, String numero);

    long countByStatut(LotStatus statut);

    @Query("""
            select l from StockLot l join l.article a
            where (:q is null or lower(l.numero) like concat('%', :q, '%') or lower(a.nom) like concat('%', :q, '%'))
              and (:articleId is null or a.id = :articleId)
              and (:owner is null or l.proprietaire = :owner)
              and (:statut is null or l.statut = :statut)
            """)
    Page<StockLot> search(@Param("q") String q, @Param("articleId") Long articleId, @Param("owner") LotOwner owner,
                          @Param("statut") LotStatus statut, Pageable pageable);
}
