package com.huilerie.erp.hr;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("""
            select e from Employee e
            where (:q is null or lower(e.nom) like concat('%', :q, '%') or lower(e.prenom) like concat('%', :q, '%')
                   or lower(e.matricule) like concat('%', :q, '%') or e.cin like concat('%', :q, '%')
                   or e.telephone like concat('%', :q, '%'))
              and (:actif is null or e.actif = :actif)
            """)
    Page<Employee> search(@Param("q") String q, @Param("actif") Boolean actif, Pageable pageable);

    /** Verrou de l'employé : deux clics Play simultanés ne créent pas deux journées. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> lockById(@Param("id") Long id);

    long countByActifTrue();
}
