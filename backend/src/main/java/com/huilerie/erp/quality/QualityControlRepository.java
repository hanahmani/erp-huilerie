package com.huilerie.erp.quality;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QualityControlRepository extends JpaRepository<QualityControl, Long> {
    @Query("""
            select c from QualityControl c join c.lot l
            where (:q is null or lower(c.numero) like concat('%', :q, '%') or lower(l.numero) like concat('%', :q, '%'))
              and (:resultat is null or c.resultat = :resultat)
              and (:lotId is null or l.id = :lotId)
            """)
    Page<QualityControl> search(@Param("q") String q, @Param("resultat") String resultat, @Param("lotId") Long lotId,
                                Pageable pageable);
}
