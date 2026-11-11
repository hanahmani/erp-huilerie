package com.huilerie.erp.pressing;

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

public interface PressingRepository extends JpaRepository<Pressing, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pressing p where p.id = :id")
    Optional<Pressing> lockById(@Param("id") Long id);

    @Query("""
            select p from Pressing p join p.client c
            where (:q is null or lower(p.numero) like concat('%', :q, '%') or lower(c.nom) like concat('%', :q, '%')
                   or c.telephone like concat('%', :q, '%') or p.numeroBulletin like concat('%', :q, '%')
                   or lower(p.vehicule) like concat('%', :q, '%'))
              and (:statut is null or p.statut = :statut)
              and (:clientId is null or c.id = :clientId)
              and (:from is null or p.dateReception >= :from)
              and (:to is null or p.dateReception < :to)
            """)
    Page<Pressing> search(@Param("q") String q, @Param("statut") Pressing.Status statut, @Param("clientId") Long clientId,
                          @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    /** Pressages à façon terminés d'un client (huile encore chez l'huilerie), les plus anciens d'abord, verrouillés. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Pressing p where p.client.id = :clientId
              and p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE
              and p.statut = com.huilerie.erp.pressing.Pressing.Status.TERMINE
            order by p.dateFin, p.id
            """)
    List<Pressing> lockOilOf(@Param("clientId") Long clientId);

    /** Pressages à façon non annulés d'un client dont le service reste à payer, les plus anciens d'abord, verrouillés. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Pressing p where p.client.id = :clientId
              and p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE
              and p.statut <> com.huilerie.erp.pressing.Pressing.Status.ANNULE
              and p.montantPaye < p.montant
            order by p.dateReception, p.id
            """)
    List<Pressing> lockUnpaidServiceOf(@Param("clientId") Long clientId);

    List<Pressing> findByClientIdAndModeOrderByIdAsc(Long clientId, Pressing.Mode mode);

    /** Par client : huile à remettre, service encore dû, reste dû au vendeur (achats). */
    @Query("""
            select c.id, c.nom, c.code, c.telephone,
              coalesce(sum(case when p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE
                                 and p.statut = com.huilerie.erp.pressing.Pressing.Status.TERMINE
                                then p.huileObtenue - p.huileRemise else 0 end), 0),
              coalesce(sum(case when p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE
                                then p.montant - p.montantPaye else 0 end), 0),
              coalesce(sum(case when p.mode = com.huilerie.erp.pressing.Pressing.Mode.ACHAT
                                then p.montantAchat - p.montantRegle else 0 end), 0)
            from Pressing p join p.client c
            where p.statut <> com.huilerie.erp.pressing.Pressing.Status.ANNULE
            group by c.id, c.nom, c.code, c.telephone
            """)
    List<Object[]> accounts();

    long countByStatutIn(List<Pressing.Status> statuts);

    long countByStatutInAndMode(List<Pressing.Status> statuts, Pressing.Mode mode);

    @Query("""
            select coalesce(sum(p.huileObtenue - p.huileRemise), 0) from Pressing p
            where p.statut = com.huilerie.erp.pressing.Pressing.Status.TERMINE
              and p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE
            """)
    BigDecimal oilToReturn();

    @Query("""
            select coalesce(sum(p.montant - p.montantPaye), 0) from Pressing p
            where p.statut <> com.huilerie.erp.pressing.Pressing.Status.ANNULE
              and p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE
            """)
    BigDecimal unpaid();

    /** Statistiques de la période : nombre, kg reçus, huile obtenue, chiffre du service. */
    @Query("""
            select count(p), coalesce(sum(p.poidsNet), 0), coalesce(sum(p.huileObtenue), 0), coalesce(sum(p.montant), 0)
            from Pressing p
            where p.statut <> com.huilerie.erp.pressing.Pressing.Status.ANNULE
              and p.dateReception >= :from and p.dateReception < :to
            """)
    List<Object[]> stats(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Olives reçues sur la période, hors annulés : [kg achetés, montant payé aux vendeurs,
     * kg pressés en service, montant du service].
     */
    @Query("""
            select coalesce(sum(case when p.mode = com.huilerie.erp.pressing.Pressing.Mode.ACHAT then p.poidsNet else 0 end), 0),
                   coalesce(sum(case when p.mode = com.huilerie.erp.pressing.Pressing.Mode.ACHAT then p.montantAchat else 0 end), 0),
                   coalesce(sum(case when p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE then p.poidsNet else 0 end), 0),
                   coalesce(sum(case when p.mode = com.huilerie.erp.pressing.Pressing.Mode.SERVICE then p.montant else 0 end), 0)
            from Pressing p
            where p.statut <> com.huilerie.erp.pressing.Pressing.Status.ANNULE
              and p.dateReception >= :from and p.dateReception < :to
            """)
    List<Object[]> olivesByMode(@Param("from") Instant from, @Param("to") Instant to);

    /** Rendement par variété sur les pressages terminés. */
    @Query("""
            select coalesce(p.variete, '(non précisée)'), count(p), sum(p.poidsNet), sum(p.huileObtenue)
            from Pressing p
            where p.huileObtenue is not null and p.statut <> com.huilerie.erp.pressing.Pressing.Status.ANNULE
              and p.dateReception >= :from and p.dateReception < :to
            group by coalesce(p.variete, '(non précisée)')
            """)
    List<Object[]> yieldByVariety(@Param("from") Instant from, @Param("to") Instant to);
}
