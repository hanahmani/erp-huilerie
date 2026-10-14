package com.huilerie.erp.salesdocs;

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

public interface SalesDocumentRepository extends JpaRepository<SalesDocument, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from SalesDocument d where d.id = :id")
    Optional<SalesDocument> lockById(@Param("id") Long id);

    @Query("""
            select d from SalesDocument d join d.client c
            where d.type = :type
              and (:q is null or lower(d.numero) like concat('%', :q, '%') or lower(c.nom) like concat('%', :q, '%')
                   or lower(d.refClient) like concat('%', :q, '%'))
              and (:statut is null or d.statut = :statut)
              and (:clientId is null or c.id = :clientId)
              and (:from is null or d.dateDoc >= :from) and (:to is null or d.dateDoc <= :to)
              and (:unpaid = false or (d.statut = com.huilerie.erp.salesdocs.SalesDocument.Status.VALIDE and d.montantPaye < d.totalTtc))
            """)
    Page<SalesDocument> search(@Param("type") SalesDocument.Type type, @Param("q") String q,
                               @Param("statut") SalesDocument.Status statut, @Param("clientId") Long clientId,
                               @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("unpaid") boolean unpaid,
                               Pageable pageable);

    /** Reste dû sur les factures validées d'un client. */
    @Query("""
            select coalesce(sum(d.totalTtc - d.montantPaye), 0) from SalesDocument d
            where d.client.id = :clientId and d.type = com.huilerie.erp.salesdocs.SalesDocument.Type.FACTURE
              and d.statut = com.huilerie.erp.salesdocs.SalesDocument.Status.VALIDE
            """)
    BigDecimal outstandingByClient(@Param("clientId") Long clientId);

    @Query("""
            select coalesce(sum(d.totalTtc - d.montantPaye), 0) from SalesDocument d
            where d.type = com.huilerie.erp.salesdocs.SalesDocument.Type.FACTURE
              and d.statut = com.huilerie.erp.salesdocs.SalesDocument.Status.VALIDE
            """)
    BigDecimal totalOutstanding();

    /** Factures échues non soldées (pour les alertes). */
    @Query("""
            select d from SalesDocument d where d.type = com.huilerie.erp.salesdocs.SalesDocument.Type.FACTURE
              and d.statut = com.huilerie.erp.salesdocs.SalesDocument.Status.VALIDE
              and d.montantPaye < d.totalTtc and d.echeance is not null and d.echeance < :today
            order by d.echeance asc
            """)
    List<SalesDocument> overdue(@Param("today") LocalDate today);

    @Query("""
            select coalesce(sum(d.totalTtc), 0) from SalesDocument d
            where d.type = com.huilerie.erp.salesdocs.SalesDocument.Type.FACTURE
              and d.statut <> com.huilerie.erp.salesdocs.SalesDocument.Status.BROUILLON
              and d.dateDoc >= :from and d.dateDoc <= :to
            """)
    BigDecimal invoicedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByTypeAndStatut(SalesDocument.Type type, SalesDocument.Status statut);

    List<SalesDocument> findBySourceIdOrderByIdAsc(Long sourceId);
}
