package com.huilerie.erp.pressing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SellerAdvanceRepository extends JpaRepository<SellerAdvance, Long> {

    List<SellerAdvance> findByClientIdOrderByIdAsc(Long clientId);

    /** Avances encore disponibles, les plus anciennes d'abord, verrouillées pendant leur déduction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SellerAdvance a where a.client.id = :clientId and a.montantImpute < a.montant order by a.id")
    List<SellerAdvance> lockAvailable(@Param("clientId") Long clientId);

    @Query("select a.client.id, coalesce(sum(a.montant - a.montantImpute), 0) from SellerAdvance a group by a.client.id")
    List<Object[]> availableByClient();
}
