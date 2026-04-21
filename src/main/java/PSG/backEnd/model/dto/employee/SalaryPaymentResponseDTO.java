package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response Data Transfer Object for salary payment. " +
        "Contains complete information about a salary payment including employee details and payment specifics.")
public record SalaryPaymentResponseDTO(
    @Schema(description = "Unique identifier of the salary payment record.",
            example = "78")
    Long id,

    @Schema(description = "Unique identifier of the employee who received the payment.",
            example = "25")
    Long employeeId,

    @Schema(description = "First name of the employee who received the payment.",
            example = "Juan Carlos")
    String employeeName,

    @Schema(description = "Last name of the employee who received the payment.",
            example = "García Pérez")
    String employeeLastName,

    @Schema(description = "Frequency of the salary payment. Values: MONTHLY, BIWEEKLY, WEEKLY.",
            example = "MONTHLY")
    SalaryFrecuency salaryFrequency,

    @Schema(description = "Date when the salary payment was made.",
            example = "2025-10-20")
    LocalDate paymentDate,

    @Schema(description = "Gross salary amount paid to the employee.",
            example = "150000.50")
    BigDecimal amount,

    @Schema(description = "ID of the project area directly assigned to this payment.",
            example = "5",
            nullable = true)
    Long projectAreaId,

    @Schema(description = "Name of the project area directly assigned to this payment.",
            example = "Mantenimiento",
            nullable = true)
    String projectAreaName,

    @Schema(description = "Color of the project area directly assigned to this payment.",
            example = "#FF5733",
            nullable = true)
    String projectAreaColor,

    @Schema(description = "ID of the project area task (sub-task).", nullable = true)
    Long projectAreaTaskId,

    @Schema(description = "Name of the project area task (sub-task).", nullable = true)
    String projectAreaTaskName,

    @Schema(description = "Payment method used for this salary payment.",
            example = "TRANSFER",
            nullable = true)
    PaymentMethod paymentMethod,

    @Schema(description = "Linked transactional document summary, if any.",
            nullable = true)
    TransactionalDocumentSummaryDTO transactionalDocument,

    @Schema(description = "IVA percentage applied to this salary payment.", example = "21.00")
    BigDecimal ivaPercentage,

    Integer documentSortOrder
) {}

