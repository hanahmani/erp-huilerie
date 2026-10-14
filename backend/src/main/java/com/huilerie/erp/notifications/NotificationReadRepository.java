package com.huilerie.erp.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    List<NotificationRead> findByUserId(Long userId);

    boolean existsByUserIdAndKey(Long userId, String key);
}
