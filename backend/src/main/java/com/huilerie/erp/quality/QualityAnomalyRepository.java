package com.huilerie.erp.quality;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QualityAnomalyRepository extends JpaRepository<QualityAnomaly, Long> {
    @Query("select a from QualityAnomaly a where (:statut is null or a.statut = :statut) and (:gravite is null or a.gravite = :gravite)")
    Page<QualityAnomaly> search(@Param("statut") String statut, @Param("gravite") String gravite, Pageable pageable);

    List<QualityAnomaly> findByStatutNotOrderByIdDesc(String statut);
}
