package com.huilerie.erp.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @Query("""
            select e from Employee e left join e.service s left join e.poste p
            where (:q is null or lower(e.nom) like concat('%', :q, '%') or lower(e.prenom) like concat('%', :q, '%')
                   or lower(e.matricule) like concat('%', :q, '%') or e.cin like concat('%', :q, '%'))
              and (:serviceId is null or s.id = :serviceId)
              and (:statut is null or e.statut = :statut)
              and (:contrat is null or e.typeContrat = :contrat)
            """)
    Page<Employee> search(@Param("q") String q, @Param("serviceId") Long serviceId, @Param("statut") String statut,
                          @Param("contrat") String contrat, Pageable pageable);

    Optional<Employee> findByUserUsername(String username);

    boolean existsByUserId(Long userId);

    @Query("select e from Employee e where e.statut = 'ACTIF' and e.dateFinContrat is not null and e.dateFinContrat <= :limit order by e.dateFinContrat")
    List<Employee> contractsEndingBefore(@Param("limit") LocalDate limit);

    long countByStatut(String statut);
}
