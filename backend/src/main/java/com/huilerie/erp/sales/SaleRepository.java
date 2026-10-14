package com.huilerie.erp.sales;

import com.huilerie.erp.users.AppUser;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByIdempotencyKey(String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id")
    Optional<Sale> lockById(@Param("id") Long id);

    /** Reste dû d'un client sur ses ventes validées. */
    @Query("select coalesce(sum(s.resteAPayer), 0) from Sale s where s.client.id = :clientId and s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE")
    BigDecimal outstandingByClient(@Param("clientId") Long clientId);

    @Query("""
            select s from Sale s left join s.client c join s.caissier u
            where (:from is null or s.dateVente >= :from)
              and (:to is null or s.dateVente < :to)
              and (:numero is null or lower(s.numeroTicket) like concat('%', :numero, '%'))
              and (:clientId is null or c.id = :clientId)
              and (:cashierId is null or u.id = :cashierId)
              and (:statut is null or s.statut = :statut)
            """)
    Page<Sale> search(@Param("from") Instant from, @Param("to") Instant to, @Param("numero") String numero,
                      @Param("clientId") Long clientId, @Param("cashierId") Long cashierId,
                      @Param("statut") SaleStatus statut, Pageable pageable);

    @Query("select distinct u from Sale s join s.caissier u")
    List<AppUser> cashiers();

    @Query("""
            select coalesce(sum(s.total), 0) from Sale s
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to
            """)
    BigDecimal totalBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(s) from Sale s where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to")
    long countBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select coalesce(sum(s.resteAPayer), 0) from Sale s where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE")
    BigDecimal totalOutstanding();

    List<Sale> findTop6ByOrderByIdDesc();

    /** Créances : ventes validées avec reste dû, regroupées par client. */
    @Query("""
            select c.id, c.code, c.nom, c.telephone, count(s), sum(s.resteAPayer), min(s.dateVente)
            from Sale s join s.client c
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.resteAPayer > 0
            group by c.id, c.code, c.nom, c.telephone
            order by sum(s.resteAPayer) desc
            """)
    List<Object[]> receivables();

    @Query("""
            select s from Sale s where s.client.id = :clientId
              and s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.resteAPayer > 0
            order by s.dateVente asc
            """)
    List<Sale> openCreditsOf(@Param("clientId") Long clientId);

    /** Ventes validées de la période (statistiques calculées en Java, fuseau Tunis). */
    @Query("""
            select s from Sale s
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to
            """)
    List<Sale> validatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select s.dateVente, s.total from Sale s
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to
            """)
    List<Object[]> amountsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select l.designation, l.unite, sum(l.quantite), sum(l.totalLigne), count(distinct s.id)
            from SaleLine l join l.sale s
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to
            group by l.designation, l.unite order by sum(l.totalLigne) desc
            """)
    List<Object[]> byArticle(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select c.nom, count(s), sum(s.total), sum(s.resteAPayer)
            from Sale s left join s.client c
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to
            group by c.nom order by sum(s.total) desc
            """)
    List<Object[]> byClient(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select u.firstName, u.lastName, count(s), sum(s.total)
            from Sale s join s.caissier u
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to
            group by u.firstName, u.lastName order by sum(s.total) desc
            """)
    List<Object[]> byCashier(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select coalesce(sum(s.remise), 0) from Sale s
            where s.statut = com.huilerie.erp.sales.SaleStatus.VALIDEE and s.dateVente >= :from and s.dateVente < :to
            """)
    BigDecimal discountsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(s) from Sale s where s.statut = com.huilerie.erp.sales.SaleStatus.ANNULEE and s.annuleeLe >= :from and s.annuleeLe < :to")
    long cancelledBetween(@Param("from") Instant from, @Param("to") Instant to);
}
