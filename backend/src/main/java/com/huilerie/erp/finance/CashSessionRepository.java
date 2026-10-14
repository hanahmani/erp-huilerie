package com.huilerie.erp.finance;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

    @Query("select s from CashSession s where s.ouverteFlag = 1")
    Optional<CashSession> findOpen();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CashSession s where s.ouverteFlag = 1")
    Optional<CashSession> lockOpen();

    Page<CashSession> findAllByOrderByIdDesc(Pageable pageable);
}
