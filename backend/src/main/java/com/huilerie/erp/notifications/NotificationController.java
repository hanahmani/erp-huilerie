package com.huilerie.erp.notifications;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    public record ReadRequest(List<String> keys) {
    }

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationService.Notification> list() {
        return service.list();
    }

    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@RequestBody ReadRequest r) {
        service.markRead(r.keys() == null ? List.of() : r.keys());
    }
}
