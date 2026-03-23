package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter Data Transfer Object for salary payments. " +
        "Used to filter and search salary payments by employee, frequency, date range, and amount range.")
public record SalaryPaymentFilterDTO(
    @Schema(description = "Filter by employee ID. Returns only salary payments for the specified employee.",
            example = "25",
            nullable = true)
    Long employeeId,

    @Schema(description = "Filter by employee first name. Case-insensitive partial match.",
            example = "Juan",
            nullable = true)
    String firstName,

    @Schema(description = "Filter by employee last name. Case-insensitive partial match.",
            example = "Perez",
            nullable = true)
    String lastName,

    @Schema(description = "Filter by salary frequency. Values: MONTHLY, BIWEEKLY, WEEKLY.",
            example = "MONTHLY",
            nullable = true)
    SalaryFrecuency salaryFrequency,

    @Schema(description = "Filter by project area ID. Returns salary payments for employees assigned to this project area.",
            example = "5",
            nullable = true)
    Long projectAreaId,

    @Schema(description = "Filter by minimum payment date. Returns salary payments from this date onwards.",
            example = "2025-01-01",
            nullable = true)
    LocalDate paymentDateFrom,

    @Schema(description = "Filter by maximum payment date. Returns salary payments up to this date.",
            example = "2025-12-31",
            nullable = true)
    LocalDate paymentDateTo,

    @Schema(description = "Filter by minimum payment amount. Returns salary payments with amount greater than or equal to this value.",
            example = "100000.00",
            nullable = true)
    BigDecimal minAmount,

    @Schema(description = "Filter by maximum payment amount. Returns salary payments with amount less than or equal to this value.",
            example = "200000.00",
            nullable = true)
    BigDecimal maxAmount,

    @Schema(description = "Filter by payment method. Values: CASH, TRANSFER, CHECK.",
            example = "TRANSFER",
            nullable = true)
    PaymentMethod paymentMethod,

    @Schema(description = "Generic search across employee name and last name (case-insensitive partial match).",
            nullable = true)
    String search,
    Long transactionalDocumentId
) {}

