package com.logatron.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException ex){return response(ex.status(),ex.code(),ex.getMessage(),List.of());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex){var fields=ex.getBindingResult().getFieldErrors().stream().map(e->new ApiError.FieldError(e.getField(),Objects.toString(e.getCode(),"INVALID"),Objects.toString(e.getDefaultMessage(),"Invalid value"))).toList();return response(HttpStatus.BAD_REQUEST,ErrorCode.INVALID_REQUEST,"The supplied request is invalid.",fields);}
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException ex){return response(HttpStatus.BAD_REQUEST,ErrorCode.INVALID_REQUEST,"The supplied request is invalid.",List.of());}
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException ex){return response(HttpStatus.CONFLICT,ErrorCode.CONFLICT,"The request conflicts with existing data.",List.of());}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex,HttpServletRequest request){log.error("Unhandled API failure for {}",request.getRequestURI(),ex);return response(HttpStatus.INTERNAL_SERVER_ERROR,ErrorCode.INTERNAL_ERROR,"The request could not be completed.",List.of());}
    private ResponseEntity<ApiError> response(HttpStatus status,ErrorCode code,String message,List<ApiError.FieldError> fields){String trace=Optional.ofNullable(MDC.get("traceId")).orElse(MDC.get("correlationId"));return ResponseEntity.status(status).body(new ApiError(Instant.now(),status.value(),code.name(),message,trace,fields));}
}

