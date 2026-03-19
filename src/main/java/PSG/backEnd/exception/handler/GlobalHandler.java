package PSG.backEnd.exception.handler;

import PSG.backEnd.exception.auth.InvalidCredentialsException;
import PSG.backEnd.exception.auth.InvalidTokenException;
import PSG.backEnd.exception.building.BuildingAlreadyExistsException;
import PSG.backEnd.exception.building.BuildingDataConflictException;
import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.building.BuildingNotValidException;
import PSG.backEnd.exception.disciplinaryAction.DisciplinaryActionNotFoundException;
import PSG.backEnd.exception.disciplinaryAction.DisciplinaryActionNotValidException;
import PSG.backEnd.exception.employee.EmployeeAlreadyExistsException;
import PSG.backEnd.exception.employee.EmployeeDataConflictException;
import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.employee.EmployeeNotValidException;
import PSG.backEnd.exception.employeeVacation.EmployeeVacationNotFoundException;
import PSG.backEnd.exception.employeeVacation.EmployeeVacationNotValidException;
import PSG.backEnd.exception.eppDelivery.EppDeliveryNotFoundException;
import PSG.backEnd.exception.eppDelivery.EppDeliveryNotValidException;
import PSG.backEnd.exception.gasStation.DuplicateTicketException;
import PSG.backEnd.exception.gasStation.FuelLoadNotFoundException;
import PSG.backEnd.exception.gasStation.FuelTypeNotAvailableException;
import PSG.backEnd.exception.gasStation.GasStationNotFoundException;
import PSG.backEnd.exception.payment.InvalidPaymentMethodException;
import PSG.backEnd.exception.permission.PermissionNotFoundException;
import PSG.backEnd.exception.report.InvalidReportFilterException;
import PSG.backEnd.exception.report.InvalidReportFormatException;
import PSG.backEnd.exception.report.ReportGenerationException;
import PSG.backEnd.exception.role.RoleAlreadyExistsException;
import PSG.backEnd.exception.role.RoleDataConflictException;
import PSG.backEnd.exception.role.RoleNotFoundException;
import PSG.backEnd.exception.role.RoleNotValidException;
import PSG.backEnd.exception.salaryPayment.DuplicateSalaryPaymentException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotFoundException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotValidException;
import PSG.backEnd.exception.supplier.SupplierAlreadyExistsException;
import PSG.backEnd.exception.supplier.SupplierDataConflictException;
import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentAlreadyActiveException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotDeletedException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.exception.user.UserAlreadyExistsException;
import PSG.backEnd.exception.user.UserDataConflictException;
import PSG.backEnd.exception.user.UserNotFoundException;
import PSG.backEnd.exception.user.UserNotValidException;
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

    private ResponseEntity<ResponseMessage> buildResponse(Exception ex, HttpStatus status) {
        return ResponseEntity.status(status).body(
                ResponseMessage.builder()
                        .message(ex.getMessage())
                        .status(status.value())
                        .exception(ex.getClass().getSimpleName())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

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
                .exception(ex.getClass().getSimpleName())
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
                .exception(ex.getClass().getSimpleName())
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

    // ==================== General Exceptions ====================

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ResponseMessage> handleSQLException(SQLException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessage> handleGenericException(Exception ex) {
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== Authentication & Authorization Exceptions ====================

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ResponseMessage> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return buildResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ResponseMessage> handleInvalidTokenException(InvalidTokenException ex) {
        return buildResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    // ==================== User Exceptions ====================

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleUserNotFoundException(UserNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotValidException.class)
    public ResponseEntity<ResponseMessage> handleUserNotValidException(UserNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleUserDataConflictException(UserDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== Role Exceptions ====================

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleRoleNotFoundException(RoleNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleRoleAlreadyExistsException(RoleAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RoleNotValidException.class)
    public ResponseEntity<ResponseMessage> handleRoleNotValidException(RoleNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RoleDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleRoleDataConflictException(RoleDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== Permission Exceptions ====================

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<ResponseMessage> handlePermissionNotFoundException(PermissionNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    // ==================== Supplier Exceptions ====================

    @ExceptionHandler(SupplierNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleSupplierNotFoundException(SupplierNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SupplierAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleSupplierAlreadyExistsException(SupplierAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SupplierDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleSupplierDataConflictException(SupplierDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== TransactionalDocument Exceptions ====================

    @ExceptionHandler(TransactionalDocumentNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleTransactionalDocumentNotFoundException(TransactionalDocumentNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TransactionalDocumentAlreadyActiveException.class)
    public ResponseEntity<ResponseMessage> handleTransactionalDocumentAlreadyActiveException(TransactionalDocumentAlreadyActiveException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TransactionalDocumentNotDeletedException.class)
    public ResponseEntity<ResponseMessage> handleTransactionalDocumentNotDeletedException(TransactionalDocumentNotDeletedException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    // ==================== Payment Exceptions ====================

    @ExceptionHandler(InvalidPaymentMethodException.class)
    public ResponseEntity<ResponseMessage> handleInvalidPaymentMethodException(InvalidPaymentMethodException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    // ==================== Employee Exceptions ====================

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeNotFoundException(EmployeeNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeAlreadyExistsException(EmployeeAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmployeeNotValidException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeNotValidException(EmployeeNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmployeeDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeDataConflictException(EmployeeDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== EmployeeVacation Exceptions ====================

    @ExceptionHandler(EmployeeVacationNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeVacationNotFoundException(EmployeeVacationNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmployeeVacationNotValidException.class)
    public ResponseEntity<ResponseMessage> handleEmployeeVacationNotValidException(EmployeeVacationNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    // ==================== SalaryPayment Exceptions ====================

    @ExceptionHandler(SalaryPaymentNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleSalaryPaymentNotFoundException(SalaryPaymentNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SalaryPaymentNotValidException.class)
    public ResponseEntity<ResponseMessage> handleSalaryPaymentNotValidException(SalaryPaymentNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateSalaryPaymentException.class)
    public ResponseEntity<ResponseMessage> handleDuplicateSalaryPaymentException(DuplicateSalaryPaymentException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== DisciplinaryAction Exceptions ====================

    @ExceptionHandler(DisciplinaryActionNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleDisciplinaryActionNotFoundException(DisciplinaryActionNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DisciplinaryActionNotValidException.class)
    public ResponseEntity<ResponseMessage> handleDisciplinaryActionNotValidException(DisciplinaryActionNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    // ==================== Building Exceptions ====================

    @ExceptionHandler(BuildingNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleBuildingNotFoundException(BuildingNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BuildingAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleBuildingAlreadyExistsException(BuildingAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BuildingNotValidException.class)
    public ResponseEntity<ResponseMessage> handleBuildingNotValidException(BuildingNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BuildingDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleBuildingDataConflictException(BuildingDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== EppDelivery Exceptions ====================

    @ExceptionHandler(EppDeliveryNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleEppDeliveryNotFoundException(EppDeliveryNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EppDeliveryNotValidException.class)
    public ResponseEntity<ResponseMessage> handleEppDeliveryNotValidException(EppDeliveryNotValidException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    // ==================== Report Exceptions ====================

    @ExceptionHandler(InvalidReportFilterException.class)
    public ResponseEntity<ResponseMessage> handleInvalidReportFilterException(InvalidReportFilterException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidReportFormatException.class)
    public ResponseEntity<ResponseMessage> handleInvalidReportFormatException(InvalidReportFormatException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ResponseMessage> handleReportGenerationException(ReportGenerationException ex) {
        return buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ==================== GasStation & FuelLoad Exceptions ====================

    @ExceptionHandler(GasStationNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleGasStationNotFoundException(GasStationNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateTicketException.class)
    public ResponseEntity<ResponseMessage> handleDuplicateTicketException(DuplicateTicketException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FuelLoadNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleFuelLoadNotFoundException(FuelLoadNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(FuelTypeNotAvailableException.class)
    public ResponseEntity<ResponseMessage> handleFuelTypeNotAvailableException(FuelTypeNotAvailableException ex) {
        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    // ==================== Vehicle Exceptions ====================

    @ExceptionHandler(PSG.backEnd.exception.vehicle.VehicleNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleVehicleNotFoundException(PSG.backEnd.exception.vehicle.VehicleNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PSG.backEnd.exception.vehicle.VehicleAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleVehicleAlreadyExistsException(PSG.backEnd.exception.vehicle.VehicleAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PSG.backEnd.exception.vehicle.VehicleDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleVehicleDataConflictException(PSG.backEnd.exception.vehicle.VehicleDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== ProjectArea Exceptions ====================

    @ExceptionHandler(PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleProjectAreaNotFoundException(PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PSG.backEnd.exception.projectarea.ProjectAreaAlreadyExistsException.class)
    public ResponseEntity<ResponseMessage> handleProjectAreaAlreadyExistsException(PSG.backEnd.exception.projectarea.ProjectAreaAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PSG.backEnd.exception.projectarea.ProjectAreaDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleProjectAreaDataConflictException(PSG.backEnd.exception.projectarea.ProjectAreaDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    // ==================== InsurancePolicy Exceptions ====================

    @ExceptionHandler(PSG.backEnd.exception.insurance.InsurancePolicyNotFoundException.class)
    public ResponseEntity<ResponseMessage> handleInsurancePolicyNotFoundException(PSG.backEnd.exception.insurance.InsurancePolicyNotFoundException ex) {
        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PSG.backEnd.exception.insurance.DuplicatePolicyNumberException.class)
    public ResponseEntity<ResponseMessage> handleDuplicatePolicyNumberException(PSG.backEnd.exception.insurance.DuplicatePolicyNumberException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PSG.backEnd.exception.insurance.InsurancePolicyDataConflictException.class)
    public ResponseEntity<ResponseMessage> handleInsurancePolicyDataConflictException(PSG.backEnd.exception.insurance.InsurancePolicyDataConflictException ex) {
        return buildResponse(ex, HttpStatus.CONFLICT);
    }
}

