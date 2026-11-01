package com.huilerie.erp.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorageTankRepository extends JpaRepository<StorageTank, Long> {

    boolean existsByCodeIgnoreCase(String code);

    List<StorageTank> findAllByOrderByCodeAsc();

    /** Verrou du puits pendant un versement : deux pressages ne peuvent pas dépasser la capacité ensemble. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from StorageTank t where t.id = :id")
    Optional<StorageTank> lockById(@Param("id") Long id);
}
