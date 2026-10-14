package com.huilerie.erp.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("""
            select m from StockMovement m join m.article a left join m.lot l
            where (:articleId is null or a.id = :articleId)
              and (:lotId is null or l.id = :lotId)
              and (:type is null or m.type = :type)
              and (:from is null or m.createdAt >= :from)
              and (:to is null or m.createdAt < :to)
            """)
    Page<StockMovement> search(@Param("articleId") Long articleId, @Param("lotId") Long lotId,
                               @Param("type") MovementType type, @Param("from") Instant from,
                               @Param("to") Instant to, Pageable pageable);

    List<StockMovement> findByDocumentTypeAndDocumentIdOrderByIdAsc(String documentType, Long documentId);
}
