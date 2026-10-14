package com.huilerie.erp.production;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ProductionOrder o where o.id = :id")
    Optional<ProductionOrder> lockById(@Param("id") Long id);

    @Query("""
            select o from ProductionOrder o
            where (:q is null or lower(o.numero) like concat('%', :q, '%') or lower(o.ligne) like concat('%', :q, '%'))
              and (:statut is null or o.statut = :statut)
              and (:from is null or o.datePrevue >= :from)
              and (:to is null or o.datePrevue <= :to)
            """)
    Page<ProductionOrder> search(@Param("q") String q, @Param("statut") ProductionOrder.Status statut,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    /** Production terminée de la période : nombre, olives, huile. */
    @Query("""
            select count(o), coalesce(sum(o.olivesKg), 0), coalesce(sum(o.huileLitres), 0)
            from ProductionOrder o
            where o.statut = com.huilerie.erp.production.ProductionOrder.Status.TERMINE
              and o.datePrevue >= :from and o.datePrevue <= :to
            """)
    List<Object[]> stats(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select o.categorie, count(o), sum(o.huileLitres) from ProductionOrder o
            where o.statut = com.huilerie.erp.production.ProductionOrder.Status.TERMINE
              and o.datePrevue >= :from and o.datePrevue <= :to
            group by o.categorie
            """)
    List<Object[]> byCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByStatutIn(List<ProductionOrder.Status> statuts);

    List<ProductionOrder> findByStatutAndDatePrevueBetweenOrderByDatePrevueAsc(ProductionOrder.Status statut,
                                                                              LocalDate from, LocalDate to);
}
