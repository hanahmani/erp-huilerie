package com.huilerie.erp.common.exception;

/** Ressource introuvable (renvoie HTTP 404). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
