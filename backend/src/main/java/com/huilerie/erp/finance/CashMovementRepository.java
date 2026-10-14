package com.huilerie.erp.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    @Query("""
            select m from CashMovement m
            where (:from is null or m.createdAt >= :from) and (:to is null or m.createdAt < :to)
              and (:type is null or m.type = :type) and (:mode is null or m.mode = :mode)
            """)
    Page<CashMovement> search(@Param("from") Instant from, @Param("to") Instant to,
                              @Param("type") CashMovementType type, @Param("mode") PaymentMode mode, Pageable pageable);

    @Query("""
            select coalesce(sum(m.montant), 0) from CashMovement m
            where m.type = :type and (:from is null or m.createdAt >= :from) and (:to is null or m.createdAt < :to)
              and (:mode is null or m.mode = :mode)
            """)
    BigDecimal total(@Param("type") CashMovementType type, @Param("from") Instant from, @Param("to") Instant to,
                     @Param("mode") PaymentMode mode);

    List<CashMovement> findByReferenceTypeAndReferenceIdOrderByIdAsc(String referenceType, Long referenceId);

    @Query("""
            select m.mode, m.type, sum(m.montant) from CashMovement m
            where m.createdAt >= :from and m.createdAt < :to
            group by m.mode, m.type
            """)
    List<Object[]> byModeAndType(@Param("from") Instant from, @Param("to") Instant to);
}
