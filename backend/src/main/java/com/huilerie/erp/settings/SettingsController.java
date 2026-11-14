package com.huilerie.erp.settings;

import com.huilerie.erp.settings.dto.SettingsRequest;
import com.huilerie.erp.settings.dto.SettingsResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    /** Public (voir SecurityConfig) : affiché par une balise img, sans jeton. 404 tant qu'aucun logo n'est téléversé. */
    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        return service.logo()
                .map(l -> ResponseEntity.ok().contentType(MediaType.parseMediaType(l.type()))
                        .cacheControl(CacheControl.noCache()).body(l.data()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SettingsResponse uploadLogo(@RequestParam("fichier") MultipartFile fichier) throws IOException {
        return service.uploadLogo(fichier.getBytes());
    }

    @DeleteMapping("/logo")
    public SettingsResponse deleteLogo() {
        return service.deleteLogo();
    }
}
