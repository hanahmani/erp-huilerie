package com.huilerie.erp.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    @Query("""
            select a from AuditLog a
            where (:username is null or lower(a.username) like concat('%', :username, '%'))
              and (:action is null or a.action like concat(:action, '%'))
              and (:entity is null or a.entityType = :entity)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt < :to)
            """)
    Page<AuditLog> search(@Param("username") String username, @Param("action") String action,
                          @Param("entity") String entity, @Param("from") Instant from, @Param("to") Instant to,
                          Pageable pageable);
}
