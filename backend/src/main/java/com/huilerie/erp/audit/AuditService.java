package com.huilerie.erp.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Trace les actions sensibles. Transaction séparée : la trace reste même si l'action échoue. */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String action, String entityType, String entityId, String details) {
        AuditLog entry = new AuditLog(username, action, entityType, entityId, truncate(details));
        entry.setIp(clientIp());
        repository.save(entry);
    }

    /** Adresse du poste à l'origine de l'action (derrière Nginx : X-Forwarded-For). */
    private static String clientIp() {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra)) {
            return null;
        }
        var req = sra.getRequest();
        String fwd = req.getHeader("X-Forwarded-For");
        String ip = fwd != null && !fwd.isBlank() ? fwd.split(",")[0].trim() : req.getRemoteAddr();
        return ip == null ? null : ip.substring(0, Math.min(ip.length(), 45));
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
