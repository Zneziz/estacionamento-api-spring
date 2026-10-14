package com.api_rest.est.infra.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Captura a exceção que lançamos no ClientService para CPF ou e-mail duplicado
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleRegraDeNegocio(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
