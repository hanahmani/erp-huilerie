package com.huilerie.erp.audit;

import com.huilerie.erp.common.Text;
import com.huilerie.erp.common.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/** Consultation du journal d'audit (lecture seule). */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    public record Line(Long id, Instant date, String username, String action, String entityType, String entityId,
                       String details, String ip) {
        static Line from(AuditLog a) {
            return new Line(a.getId(), a.getCreatedAt(), a.getUsername(), a.getAction(), a.getEntityType(),
                    a.getEntityId(), a.getDetails(), a.getIp());
        }
    }

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @Transactional(readOnly = true)
    public PageResponse<Line> search(@RequestParam(required = false) String username,
                                     @RequestParam(required = false) String action,
                                     @RequestParam(required = false) String entity,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "id"));
        String act = Text.clean(action);
        return PageResponse.of(repository.search(Text.search(username), act == null ? null : act.toUpperCase(),
                Text.clean(entity), from, to, pr), Line::from);
    }
}
