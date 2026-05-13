package PSG.backEnd.exception;

import PSG.backEnd.exception.notification.NotificationSubscriptionNotValidException;
import PSG.backEnd.exception.purchaseOrder.PurchaseOrderNotValidException;
import PSG.backEnd.exception.serviceSupplier.ServiceAssignmentAlreadyExistsException;
import PSG.backEnd.exception.vehicle.VehicleAlreadyExistsException;
import PSG.backEnd.exception.vehicle.VehicleDataConflictException;
import PSG.backEnd.exception.vehicle.VehicleNotValidException;
import PSG.backEnd.exception.vehicle.VehicleTypeAlreadyExistsException;
import PSG.backEnd.service.implementation.ExceptionAuditService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
            WebRequest request,
            Exception ex) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(getRequestPath(request)));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("exception", ex.getClass().getSimpleName());

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
                request,
                ex
        );
        problemDetail.setProperty("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handles Jakarta Bean Validation constraint violations at the entity level.
     * Returns field-level errors in the same format as MethodArgumentNotValidException.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {

        auditException(ex, request);

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> extractFieldName(v),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        String message = getMessage("error.validation");

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                message,
                request,
                ex
        );
        problemDetail.setProperty("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handles malformed request bodies (invalid JSON, wrong types, bad date formats, etc.)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        auditException(ex, request);

        String message = getMessage("error.messageNotReadable");

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid Request Format",
                message,
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handles database constraint violations (unique constraints, FK violations, etc.)
     * that were not caught at the service layer.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {

        auditException(ex, request);

        String message = getMessage("error.dataIntegrity");

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                "Data Integrity Error",
                message,
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
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
                request,
                ex
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
                request,
                ex
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
                request,
                ex
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
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    @ExceptionHandler(PurchaseOrderNotValidException.class)
    public ResponseEntity<ProblemDetail> handlePurchaseOrderNotValidException(
            PurchaseOrderNotValidException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Purchase Order Validation Error",
                ex.getMessage(),
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handles vehicle validation errors (e.g., missing required fields at business level)
     * Returns 400 Bad Request
     */
    @ExceptionHandler(VehicleNotValidException.class)
    public ResponseEntity<ProblemDetail> handleVehicleNotValidException(
            VehicleNotValidException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Vehicle Validation Error",
                ex.getMessage(),
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handles vehicle already exists conflicts
     * Returns 409 Conflict
     */
    @ExceptionHandler(VehicleAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleVehicleAlreadyExistsException(
            VehicleAlreadyExistsException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                "Vehicle Already Exists",
                ex.getMessage(),
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handles service assignment already exists conflicts (e.g. duplicate account number)
     * Returns 409 Conflict
     */
    @ExceptionHandler(ServiceAssignmentAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleServiceAssignmentAlreadyExistsException(
            ServiceAssignmentAlreadyExistsException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                "Service Assignment Conflict",
                ex.getMessage(),
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handles vehicle type already exists conflicts
     * Returns 409 Conflict
     */
    @ExceptionHandler(VehicleTypeAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleVehicleTypeAlreadyExistsException(
            VehicleTypeAlreadyExistsException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                "Vehicle Type Already Exists",
                ex.getMessage(),
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handles vehicle data integrity conflicts (e.g., duplicate license plate on update)
     * Returns 409 Conflict
     */
    @ExceptionHandler(VehicleDataConflictException.class)
    public ResponseEntity<ProblemDetail> handleVehicleDataConflictException(
            VehicleDataConflictException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                "Vehicle Data Conflict",
                ex.getMessage(),
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    @ExceptionHandler(NotificationSubscriptionNotValidException.class)
    public ResponseEntity<ProblemDetail> handleNotificationSubscriptionNotValidException(
            NotificationSubscriptionNotValidException ex,
            WebRequest request) {

        auditException(ex, request);

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Notification Subscription Validation Error",
                ex.getMessage(),
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
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
                request,
                ex
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
                request,
                ex
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
    }

    /**
     * Extracts the field name from a ConstraintViolation's property path.
     */
    private String extractFieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
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

