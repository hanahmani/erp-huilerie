package com.huilerie.erp.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    /** Verrou de ligne : deux ventes simultanées ne peuvent pas vendre le même litre. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalance b where b.article.id = :articleId and b.lotRef = :lotRef")
    Optional<StockBalance> lockFor(@Param("articleId") Long articleId, @Param("lotRef") long lotRef);

    boolean existsByArticleIdAndLotRef(Long articleId, long lotRef);

    Optional<StockBalance> findByArticleIdAndLotRef(Long articleId, long lotRef);

    List<StockBalance> findByArticleIdIn(Collection<Long> articleIds);

    List<StockBalance> findByLotId(Long lotId);

    @Query("select coalesce(sum(b.quantite), 0) from StockBalance b where b.lot.id = :lotId")
    BigDecimal quantityOfLot(@Param("lotId") Long lotId);

    /** Stock vendable : sans lot, ou lot de l'entreprise disponible. */
    @Query("""
            select b from StockBalance b left join b.lot l
            where b.quantite > 0 and b.article.id in :articleIds
              and (l is null or (l.proprietaire = :owner and l.statut = :statut))
            """)
    List<StockBalance> sellable(@Param("articleIds") Collection<Long> articleIds,
                                @Param("owner") LotOwner owner, @Param("statut") LotStatus statut);
}
