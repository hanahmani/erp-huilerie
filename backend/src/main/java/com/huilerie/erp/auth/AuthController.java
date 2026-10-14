package com.huilerie.erp.auth;

import com.huilerie.erp.auth.dto.ChangePasswordRequest;
import com.huilerie.erp.auth.dto.LoginRequest;
import com.huilerie.erp.auth.dto.LoginResponse;
import com.huilerie.erp.auth.dto.MeResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Aucune route d'inscription : les comptes sont créés uniquement par un administrateur. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        return authService.me(auth.getName());
    }

    @PostMapping("/change-password")
    public LoginResponse changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
        return authService.changePassword(auth.getName(), request.currentPassword(), request.newPassword());
    }
}
