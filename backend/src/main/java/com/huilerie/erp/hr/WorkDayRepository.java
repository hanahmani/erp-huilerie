package com.huilerie.erp.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkDayRepository extends JpaRepository<WorkDay, Long> {

    boolean existsByEmployeeIdAndJour(Long employeeId, LocalDate jour);

    boolean existsByEmployeeIdAndJourAndIdNot(Long employeeId, LocalDate jour, Long id);

    Optional<WorkDay> findFirstByEmployeeIdAndHeureFinIsNullOrderByJourDesc(Long employeeId);

    List<WorkDay> findByEmployeeIdInAndJour(Collection<Long> employeeIds, LocalDate jour);

    /** Journées restées ouvertes un jour précédent (Stop oublié). */
    List<WorkDay> findByHeureFinIsNullAndJourBeforeOrderByJourAsc(LocalDate jour);

    long countByJourAndHeureFinIsNull(LocalDate jour);

    @Query("""
            select w from WorkDay w join w.employee e
            where (:employeeId is null or e.id = :employeeId)
              and (:from is null or w.jour >= :from) and (:to is null or w.jour <= :to)
            """)
    Page<WorkDay> search(@Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                         Pageable pageable);

    /** Par employé : nombre de journées et total des TJM gagnés. */
    @Query("""
            select w.employee.id, count(w), coalesce(sum(w.tjm), 0) from WorkDay w
            where w.employee.id in :ids group by w.employee.id
            """)
    List<Object[]> earnedBy(@Param("ids") Collection<Long> ids);
}
