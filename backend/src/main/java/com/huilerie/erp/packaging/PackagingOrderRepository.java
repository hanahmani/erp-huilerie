package com.huilerie.erp.packaging;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PackagingOrderRepository extends JpaRepository<PackagingOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PackagingOrder o where o.id = :id")
    Optional<PackagingOrder> lockById(@Param("id") Long id);

    @Query("""
            select o from PackagingOrder o join o.produit p
            where (:q is null or lower(o.numero) like concat('%', :q, '%') or lower(p.nom) like concat('%', :q, '%'))
              and (:statut is null or o.statut = :statut)
              and (:from is null or o.createdAt >= :from) and (:to is null or o.createdAt < :to)
            """)
    Page<PackagingOrder> search(@Param("q") String q, @Param("statut") String statut, @Param("from") Instant from,
                                @Param("to") Instant to, Pageable pageable);

    @Query("""
            select p.nom, count(o), sum(o.quantiteProduite), sum(o.volumeUtilise)
            from PackagingOrder o join o.produit p
            where o.statut = 'VALIDE' and o.createdAt >= :from and o.createdAt < :to
            group by p.nom order by sum(o.volumeUtilise) desc
            """)
    List<Object[]> byProduct(@Param("from") Instant from, @Param("to") Instant to);
}
