package com.huilerie.erp.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    @Query("""
            select l from LeaveRequest l join l.employee e
            where (:employeeId is null or e.id = :employeeId) and (:statut is null or l.statut = :statut)
            """)
    Page<LeaveRequest> search(@Param("employeeId") Long employeeId, @Param("statut") String statut, Pageable pageable);

    @Query("""
            select count(l) from LeaveRequest l where l.employee.id = :employeeId
              and l.statut in ('EN_ATTENTE', 'VALIDEE') and l.dateDebut <= :fin and l.dateFin >= :debut
            """)
    long overlapping(@Param("employeeId") Long employeeId, @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    long countByStatut(String statut);

    @Query("select count(l) from LeaveRequest l where l.statut = 'VALIDEE' and l.dateDebut <= :d and l.dateFin >= :d")
    long onLeave(@Param("d") LocalDate d);
}
