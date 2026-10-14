package com.huilerie.erp.common.exception;

/** Règle métier non respectée (renvoie HTTP 409). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
