package com.huilerie.erp.notifications;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_reads")
public class NotificationRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notif_key", nullable = false, length = 120)
    private String key;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    protected NotificationRead() {
    }

    public NotificationRead(Long userId, String key) {
        this.userId = userId;
        this.key = key;
        this.readAt = Instant.now();
    }

    public String getKey() { return key; }
}
