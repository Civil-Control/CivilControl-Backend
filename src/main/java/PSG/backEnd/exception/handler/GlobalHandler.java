package PSG.backEnd.exception.handler;

import PSG.backEnd.exception.building.BuildingAlreadyExistsException;
import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.building.BuildingNotValidException;
import PSG.backEnd.exception.employee.EmployeeAlreadyExistsException;
import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.employee.EmployeeNotValidException;
import PSG.backEnd.exception.gasStation.DuplicateTicketException;
import PSG.backEnd.exception.gasStation.FuelLoadNotFoundException;
import PSG.backEnd.exception.gasStation.FuelTypeNotAvailableException;
import PSG.backEnd.exception.gasStation.GasStationNotFoundException;
import PSG.backEnd.exception.payment.InvalidPaymentMethodException;
import PSG.backEnd.exception.disciplinaryAction.DisciplinaryActionNotFoundException;
import PSG.backEnd.exception.disciplinaryAction.DisciplinaryActionNotValidException;
import PSG.backEnd.exception.report.InvalidReportFilterException;
import PSG.backEnd.exception.report.InvalidReportFormatException;
import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.exception.salaryPayment.DuplicateSalaryPaymentException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotFoundException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotValidException;
import PSG.backEnd.exception.supplier.SupplierAlreadyExistsException;
import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentAlreadyActiveException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotDeletedException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ResponseMessage response = ResponseMessage.builder()
                .message("Validation errors: " + errors)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message;
        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            if (invalidFormatEx.getTargetType() != null && invalidFormatEx.getTargetType().isEnum()) {
                message = String.format("Invalid value for enum %s. Allowed values are: %s",
                        invalidFormatEx.getTargetType().getSimpleName(),
                        String.join(", ", getEnumValues(invalidFormatEx.getTargetType())));
            } else if (invalidFormatEx.getTargetType() == LocalDateTime.class ||
                      invalidFormatEx.getTargetType() == LocalDate.class) {
                message = "Invalid date format, expected yyyy-MM-dd";
            } else {
                message = String.format("Invalid value for field %s: %s",
                        invalidFormatEx.getPath().get(0).getFieldName(),
                        invalidFormatEx.getValue());
            }
        } else {
            message = "Invalid request format";
        }

        ResponseMessage response = ResponseMessage.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    private String[] getEnumValues(Class<?> enumClass) {
        return enumClass.getEnumConstants() != null ?
            java.util.Arrays.stream(enumClass.getEnumConstants())
                .map(Object::toString)
                .toArray(String[]::new) :
            new String[0];
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ResponseMessage> handleSQLException(SQLException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleSupplierNotFoundException(SupplierNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(SupplierAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleSupplierAlreadyExistsException(SupplierAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(TransactionalDocumentNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleTransactionalDocumentNotFoundException(TransactionalDocumentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(TransactionalDocumentAlreadyActiveException.class)
    public ResponseEntity<ResponseMessage> handleTransactionalDocumentAlreadyActiveException(TransactionalDocumentAlreadyActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }


    @ExceptionHandler(TransactionalDocumentNotDeletedException.class)
    public ResponseEntity<ResponseMessage> handleTransactionalDocumentNotDeletedException(
            TransactionalDocumentNotDeletedException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(InvalidPaymentMethodException.class)
    public ResponseEntity<ResponseMessage> handleInvalidPaymentMethodException(InvalidPaymentMethodException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeNotFoundException(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeAlreadyExistsException(EmployeeAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(EmployeeNotValidException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeNotValidException(EmployeeNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(SalaryPaymentNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleSalaryPaymentNotFoundException(SalaryPaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(SalaryPaymentNotValidException.class)
    public ResponseEntity<ResponseMessage> handleSalaryPaymentNotValidException(SalaryPaymentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @ExceptionHandler(DuplicateSalaryPaymentException.class)
    public ResponseEntity<ResponseMessage> handleDuplicateSalaryPaymentException(DuplicateSalaryPaymentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(DisciplinaryActionNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleDisciplinaryActionNotFoundException(DisciplinaryActionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(DisciplinaryActionNotValidException.class)
    public ResponseEntity<ResponseMessage> handleDisciplinaryActionNotValidException(DisciplinaryActionNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(BuildingNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleBuildingNotFoundException(BuildingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(BuildingAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleBuildingAlreadyExistsException(BuildingAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(BuildingNotValidException.class)
    public ResponseEntity<ResponseMessage> handleBuildingNotValidException(BuildingNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(InvalidReportFilterException.class)
    public ResponseEntity<ResponseMessage> handleInvalidReportFilterException(InvalidReportFilterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(InvalidReportFormatException.class)
    public ResponseEntity<ResponseMessage> handleInvalidReportFormatException(InvalidReportFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ResponseMessage> handleReportGenerationException(ReportGenerationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessage> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ResponseMessage.builder()
                        .message("An unexpected error occurred: " + ex.getMessage())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(GasStationNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleGasStationNotFoundException(GasStationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(DuplicateTicketException.class)
    public ResponseEntity<ResponseMessage> handleDuplicateTicketException(DuplicateTicketException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(FuelLoadNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleFuelLoadNotFoundException(FuelLoadNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(FuelTypeNotAvailableException.class)
    public ResponseEntity<ResponseMessage> handleFuelTypeNotAvailableException(FuelTypeNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}