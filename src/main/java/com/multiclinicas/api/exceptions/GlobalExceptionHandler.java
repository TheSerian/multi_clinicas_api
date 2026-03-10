package com.multiclinicas.api.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Business Rule Violation");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Resource Not Found");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, String>> handleResourceConflictException(ResourceConflictException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Resource Conflict");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation Error");
        response.put("message", "Verifique os campos obrigatórios");
        response.put("details", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Conflito de dados");
        error.put("message", "Já existe um registro com estes dados (CPF ou Email) nesta clínica.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
            details.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Erro de validação na entidade");
        response.put("details", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> handleMissingRequestHeaderException(
            org.springframework.web.bind.MissingRequestHeaderException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad Request");
        error.put("message", "Required header '" + ex.getHeaderName() + "' is missing.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad Request");
        error.put("message", "Corpo da requisição inválido ou mal formatado. Verifique os tipos de dados enviados.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Access Denied");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        log.error("Erro inesperado capturado pelo GlobalExceptionHandler:", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal Server Error");
        error.put("message", "Ocorreu um erro inesperado. Entre em contato com o suporte.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
