package com.huilerie.erp.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, Long> {

    @Query("""
            select p from EmployeePayment p join p.employee e
            where (:employeeId is null or e.id = :employeeId)
              and (:type is null or p.type = :type)
              and (:from is null or p.date >= :from) and (:to is null or p.date < :to)
            """)
    Page<EmployeePayment> search(@Param("employeeId") Long employeeId, @Param("type") EmployeePayment.Type type,
                                 @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    /** Par employé : total déjà payé (salaire, journées et avances). */
    @Query("""
            select p.employee.id, coalesce(sum(p.montant), 0) from EmployeePayment p
            where p.employee.id in :ids group by p.employee.id
            """)
    List<Object[]> paidBy(@Param("ids") Collection<Long> ids);
}
