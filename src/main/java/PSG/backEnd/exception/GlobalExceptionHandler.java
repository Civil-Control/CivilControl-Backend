package PSG.backEnd.exception;

import PSG.backEnd.service.implementation.ExceptionAuditService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application
 * Uses ProblemDetail (RFC 7807) as standard response format
 * Automatically persists exceptions to database asynchronously
 * Translates error messages to Spanish using i18n
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;
    private final ExceptionAuditService exceptionAuditService;

    /**
     * Helper method to create ProblemDetail with common fields
     */
    private ProblemDetail createProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            WebRequest request) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(getRequestPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Helper method to extract request path
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }

    /**
     * Helper method to extract HTTP method
     */
    private String getHttpMethod(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getMethod();
        }
        return "UNKNOWN";
    }

    /**
     * Helper method to audit exception asynchronously
     */
    private void auditException(Exception ex, WebRequest request) {
        String path = getRequestPath(request);
        String method = getHttpMethod(request);
        exceptionAuditService.logException(ex, path, method);
        log.error("Exception occurred at {} {}: {}", method, path, ex.getMessage(), ex);
    }

    /**
     * Handles Bean Validation errors (JSR-380)
     * Returns validation errors in ProblemDetail format
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        auditException(ex, request);

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ?
                                error.getDefaultMessage() : "Error de validación",
                        (existing, replacement) -> existing
                ));

        String message = getMessage("error.validation");

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                message,
                request
        );
        problemDetail.setProperty("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handles custom NotFoundException
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(
            NotFoundException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    /**
     * Handles JPA EntityNotFoundException
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {

        auditException(ex, request);

        String message = getMessage("error.entityNotFound");

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Entity Not Found",
                ex.getMessage() != null ? ex.getMessage() : message,
                request
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    /**
     * Handles IllegalArgumentException
     * Commonly used for business logic errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid Argument",
                ex.getMessage(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handles IllegalStateException
     * Commonly used for invalid state errors
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                "Invalid State",
                ex.getMessage(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handles generic RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {

        auditException(ex, request);

        String message = getMessage("error.unexpected") + ": " + ex.getMessage();

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Runtime Error",
                message,
                request
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
    }

    /**
     * Handles any other uncaught exception
     * This is the last resort handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(
            Exception ex,
            WebRequest request) {

        auditException(ex, request);

        String message = getMessage("error.internalServerError");

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                message,
                request
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
    }

    /**
     * Helper method to get translated messages from MessageSource
     */
    private String getMessage(String key, Object... args) {
        try {
            return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            // If key is not found, return the key itself
            return key;
        }
    }
}

