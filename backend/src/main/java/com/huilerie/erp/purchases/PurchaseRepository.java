package com.huilerie.erp.purchases;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.id = :id")
    Optional<Purchase> lockById(@Param("id") Long id);

    @Query("""
            select p from Purchase p join p.fournisseur f
            where (:q is null or lower(p.numero) like concat('%', :q, '%') or lower(f.nom) like concat('%', :q, '%')
                   or lower(p.refFacture) like concat('%', :q, '%'))
              and (:supplierId is null or f.id = :supplierId)
              and (:statut is null or p.statut = :statut)
              and (:from is null or p.dateCommande >= :from)
              and (:to is null or p.dateCommande <= :to)
              and (:unpaid = false or (p.statut <> com.huilerie.erp.purchases.PurchaseStatus.BROUILLON
                   and p.statut <> com.huilerie.erp.purchases.PurchaseStatus.ANNULEE and p.montantPaye < p.totalTtc))
            """)
    Page<Purchase> search(@Param("q") String q, @Param("supplierId") Long supplierId,
                          @Param("statut") PurchaseStatus statut, @Param("from") LocalDate from,
                          @Param("to") LocalDate to, @Param("unpaid") boolean unpaid, Pageable pageable);

    /** Dettes fournisseurs : commandes engagées non soldées, par fournisseur. */
    @Query("""
            select f.id, f.code, f.nom, count(p), sum(p.totalTtc - p.montantPaye)
            from Purchase p join p.fournisseur f
            where p.statut <> com.huilerie.erp.purchases.PurchaseStatus.BROUILLON
              and p.statut <> com.huilerie.erp.purchases.PurchaseStatus.ANNULEE
              and p.montantPaye < p.totalTtc
            group by f.id, f.code, f.nom
            order by sum(p.totalTtc - p.montantPaye) desc
            """)
    List<Object[]> payables();

    @Query("""
            select coalesce(sum(p.totalTtc - p.montantPaye), 0) from Purchase p
            where p.statut <> com.huilerie.erp.purchases.PurchaseStatus.BROUILLON
              and p.statut <> com.huilerie.erp.purchases.PurchaseStatus.ANNULEE
            """)
    BigDecimal totalPayable();

    @Query("""
            select coalesce(sum(p.totalTtc), 0) from Purchase p
            where p.statut <> com.huilerie.erp.purchases.PurchaseStatus.BROUILLON
              and p.statut <> com.huilerie.erp.purchases.PurchaseStatus.ANNULEE
              and p.dateCommande >= :from and p.dateCommande <= :to
            """)
    BigDecimal totalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Achats engagés par fournisseur sur la période. */
    @Query("""
            select f.nom, count(p), sum(p.totalHt), sum(p.totalTtc), sum(p.montantPaye)
            from Purchase p join p.fournisseur f
            where p.statut <> com.huilerie.erp.purchases.PurchaseStatus.BROUILLON
              and p.statut <> com.huilerie.erp.purchases.PurchaseStatus.ANNULEE
              and p.dateCommande >= :from and p.dateCommande <= :to
            group by f.nom order by sum(p.totalTtc) desc
            """)
    List<Object[]> bySupplier(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByStatutIn(List<PurchaseStatus> statuts);
}
