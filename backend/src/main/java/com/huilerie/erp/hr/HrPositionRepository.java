package com.huilerie.erp.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HrPositionRepository extends JpaRepository<HrRef.Position, Long> {
    List<HrRef.Position> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);
}
