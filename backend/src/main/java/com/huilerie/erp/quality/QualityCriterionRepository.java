package com.huilerie.erp.quality;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityCriterionRepository extends JpaRepository<QualityCriterion, Long> {
    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);

    List<QualityCriterion> findAllByOrderByNomAsc();
}
