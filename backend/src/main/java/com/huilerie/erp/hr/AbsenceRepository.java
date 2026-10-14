package com.huilerie.erp.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    @Query("""
            select a from Absence a join a.employee e
            where (:employeeId is null or e.id = :employeeId)
              and (:from is null or a.jour >= :from) and (:to is null or a.jour <= :to)
            """)
    Page<Absence> search(@Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                         Pageable pageable);

    boolean existsByEmployeeIdAndJour(Long employeeId, LocalDate jour);
}
