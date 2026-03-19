package PSG.backEnd.controller;

import PSG.backEnd.model.dto.employee.SalaryPaymentBatchDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentFilterDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ISalaryPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/salary-payments")
@RequiredArgsConstructor
@Tag(name = "Salary Payments", description = "API for managing employee salary payments. Handles recording, tracking, and managing salary transactions including payment dates, amounts, frequencies, and employee payroll history.")
public class SalaryPaymentController {

    private final ISalaryPaymentService iSalaryPaymentService;

    @PostMapping
    @Operation(summary = "Create a new salary payment",
            description = "Registers a new salary payment for an employee. Includes employee identification, payment date, " +
                    "payment amount, and salary frequency (monthly, biweekly, weekly). Used for payroll tracking and history.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Salary payment successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "409", description = "Conflict with existing salary payment record")
    })
    public ResponseEntity<SalaryPaymentResponseDTO> createSalaryPayment(
            @Validated(OnCreate.class) @RequestBody SalaryPaymentDTO salaryPaymentDTO) {
        SalaryPaymentResponseDTO createdSalaryPayment = iSalaryPaymentService.createSalaryPayment(salaryPaymentDTO);
        return new ResponseEntity<>(createdSalaryPayment, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple salary payments in one request")
    public ResponseEntity<List<SalaryPaymentResponseDTO>> createBatchSalaryPayments(
            @Validated @RequestBody SalaryPaymentBatchDTO batchDTO) {
        List<SalaryPaymentResponseDTO> created = iSalaryPaymentService.createBatchSalaryPayments(batchDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all salary payments",
            description = "Retrieves a paginated list of salary payments with optional filtering by employee (ID, first name, last name), " +
                    "salary frequency (monthly, biweekly, weekly), payment date range, and amount range (minimum and maximum). " +
                    "Supports sorting and pagination. Useful for payroll reports and employee payment history.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved salary payments list")
    public ResponseEntity<Page<SalaryPaymentResponseDTO>> getSalaryPayments(
            @Parameter(description = "Filter by employee ID") @RequestParam(required = false) Long employeeId,
            @Parameter(description = "Filter by employee first name (case-insensitive partial match)") @RequestParam(required = false) String firstName,
            @Parameter(description = "Filter by employee last name (case-insensitive partial match)") @RequestParam(required = false) String lastName,
            @Parameter(description = "Filter by salary frequency (MONTHLY, BIWEEKLY, WEEKLY)") @RequestParam(required = false) SalaryFrecuency salaryFrequency,
            @Parameter(description = "Filter by project area ID") @RequestParam(required = false) Long projectAreaId,
            @Parameter(description = "Filter by minimum payment date") @RequestParam(required = false) LocalDate paymentDateFrom,
            @Parameter(description = "Filter by maximum payment date") @RequestParam(required = false) LocalDate paymentDateTo,
            @Parameter(description = "Filter by minimum payment amount") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Filter by maximum payment amount") @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Filter by payment method (CASH, TRANSFER, CHECK)") @RequestParam(required = false) PaymentMethod paymentMethod,
            @Parameter(description = "Generic search across employee name and lastName (partial match)") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by. Available fields: id, paymentDate, amount, salaryFrequency, employeeName, employeeLastName, employeeDni, employeeCuil, employeeId",
                    example = "paymentDate")
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @Parameter(description = "Sort direction (asc or desc)") @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Map simple field names to entity paths
        String mappedSortBy = mapSortField(sortBy);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), mappedSortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        SalaryPaymentFilterDTO filterDTO = new SalaryPaymentFilterDTO(
                employeeId, firstName, lastName, salaryFrequency, projectAreaId, paymentDateFrom, paymentDateTo,
                minAmount, maxAmount, paymentMethod, search
        );

        return ResponseEntity.ok(iSalaryPaymentService.getAllSalaryPayments(filterDTO, pageable));
    }

    /**
     * Maps simple field names to their corresponding entity paths.
     * This allows the frontend to use intuitive field names without knowing the internal entity structure.
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "employeeName" -> "employee.name";
            case "employeeLastName" -> "employee.lastName";
            case "employeeDni" -> "employee.dni";
            case "employeeCuil" -> "employee.cuil";
            case "employeeId" -> "employee.id";
            // Keep backward compatibility with old field names
            case "name" -> "employee.name";
            case "lastName" -> "employee.lastName";
            case "dni" -> "employee.dni";
            case "cuil" -> "employee.cuil";
            default -> sortBy; // For 'id', 'paymentDate', 'amount', 'salaryFrequency', etc.
        };
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get salary payment by ID",
            description = "Retrieves detailed information about a specific salary payment by its unique identifier. " +
                    "Includes employee information, payment date, amount, and frequency.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Salary payment found"),
            @ApiResponse(responseCode = "404", description = "Salary payment not found")
    })
    public ResponseEntity<SalaryPaymentResponseDTO> getSalaryPaymentById(
            @Parameter(description = "Salary payment unique identifier", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(iSalaryPaymentService.getSalaryPaymentById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update salary payment",
            description = "Updates an existing salary payment record. Only provided fields will be updated. Allows updating payment date, " +
                    "amount, and frequency. Use with caution as this modifies financial records.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Salary payment successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Salary payment or employee not found"),
            @ApiResponse(responseCode = "409", description = "Update conflict")
    })
    public ResponseEntity<SalaryPaymentResponseDTO> updateSalaryPayment(
            @Parameter(description = "Salary payment unique identifier", required = true) @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody SalaryPaymentDTO salaryPaymentDTO) {
        return ResponseEntity.ok(iSalaryPaymentService.updateSalaryPayment(id, salaryPaymentDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete salary payment",
            description = "Deletes a salary payment record from the system. This operation cannot be undone. " +
                    "Use with extreme caution as it removes financial transaction history. May require additional authorization.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Salary payment successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Salary payment not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete salary payment due to constraints")
    })
    public ResponseEntity<Void> deleteSalaryPayment(
            @Parameter(description = "Salary payment unique identifier", required = true) @PathVariable Long id) {
        iSalaryPaymentService.deleteSalaryPayment(id);
        return ResponseEntity.noContent().build();
    }
}

