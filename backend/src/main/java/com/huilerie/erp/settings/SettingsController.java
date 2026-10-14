package com.huilerie.erp.settings;

import com.huilerie.erp.settings.dto.SettingsRequest;
import com.huilerie.erp.settings.dto.SettingsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public SettingsResponse get() {
        return service.get();
    }

    @PutMapping
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return service.update(request);
    }
}
