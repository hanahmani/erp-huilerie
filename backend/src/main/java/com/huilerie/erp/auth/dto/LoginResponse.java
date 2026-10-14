package com.huilerie.erp.auth.dto;

public record LoginResponse(String token, long expiresIn, MeResponse user) {
}
