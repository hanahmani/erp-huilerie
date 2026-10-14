package com.huilerie.erp.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            select e from Expense e join e.categorie c
            where (:q is null or lower(e.libelle) like concat('%', :q, '%') or lower(e.numero) like concat('%', :q, '%'))
              and (:categoryId is null or c.id = :categoryId)
              and (:from is null or e.dateDepense >= :from) and (:to is null or e.dateDepense <= :to)
              and (:statut is null or e.statut = :statut)
            """)
    Page<Expense> search(@Param("q") String q, @Param("categoryId") Long categoryId, @Param("from") LocalDate from,
                         @Param("to") LocalDate to, @Param("statut") String statut, Pageable pageable);

    @Query("""
            select coalesce(sum(e.montant), 0) from Expense e
            where e.statut = 'VALIDEE' and e.dateDepense >= :from and e.dateDepense <= :to
            """)
    BigDecimal totalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select c.nom, count(e), sum(e.montant) from Expense e join e.categorie c
            where e.statut = 'VALIDEE' and e.dateDepense >= :from and e.dateDepense <= :to
            group by c.nom order by sum(e.montant) desc
            """)
    List<Object[]> byCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
