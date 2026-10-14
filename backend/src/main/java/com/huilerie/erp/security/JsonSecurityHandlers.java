package com.huilerie.erp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huilerie.erp.common.response.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Réponses JSON homogènes pour 401 et 403 produits par Spring Security. */
@Component
public class JsonSecurityHandlers {

    private final ObjectMapper objectMapper;

    public JsonSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> write(response, ApiError.of(401, "NON_AUTHENTIFIE",
                "Connexion requise ou session expirée.", request.getRequestURI()));
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(response, ApiError.of(403, "ACCES_REFUSE",
                "Vous n'avez pas la permission d'effectuer cette action.", request.getRequestURI()));
    }

    private void write(HttpServletResponse response, ApiError error) throws IOException {
        response.setStatus(error.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
