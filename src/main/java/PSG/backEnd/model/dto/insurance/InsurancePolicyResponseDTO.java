package PSG.backEnd.model.dto.insurance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response DTO containing complete information about an insurance policy, " +
        "including all coverage details and policy terms.")
public record InsurancePolicyResponseDTO(

        @Schema(description = "Unique identifier of the insurance policy.",
                example = "42")
        Long id,

        @Schema(description = "Unique policy number assigned by the insurance company.",
                example = "POL-2024-001234")
        String policyNumber,

        @Schema(description = "Term or renewal number for the policy period.",
                example = "2024-01")
        String termNumber,

        @Schema(description = "Endorsement sequence number for policy modifications.",
                example = "END-001")
        String endorsementSecuence,

        @Schema(description = "Type of insurance policy.",
                example = "AUTOMOTOR",
                allowableValues = {"AUTOMOTOR"})
        String policyType,

        @Schema(description = "Current status of the policy.",
                example = "ACTIVO",
                allowableValues = {"COTIZADO", "ACTIVO", "VENCIDO", "CANCELADO", "EXPIRADO"})
        String policyStatus,

        @Schema(description = "Frequency of premium payments.",
                example = "MENSUAL",
                allowableValues = {"MENSUAL", "BIMESTRAL", "TRIMESTRAL", "SEMI_ANUAL", "ANUAL", "PAGO_UNICO"})
        String paymentFrequency,

        @Schema(description = "Total sum insured or coverage amount.",
                example = "1500000.00")
        BigDecimal sumInsured,

        @Schema(description = "Date when the policy was issued.",
                example = "2024-01-10")
        LocalDate issueDate,

        @Schema(description = "Date when the policy coverage begins.",
                example = "2024-01-15")
        LocalDate effectiveFrom,

        @Schema(description = "Date when the policy coverage ends.",
                example = "2025-01-14")
        LocalDate effectiveTo,

        @Schema(description = "Date when the policy was cancelled, if applicable.",
                example = "2024-06-30")
        LocalDate cancellationDate,

        @Schema(description = "Number of payment installments for the policy premium.",
                example = "12")
        Integer numberOfInstallments,

        @Schema(description = "Total premium amount for the policy.",
                example = "50000.00")
        BigDecimal premioTotal,

        @Schema(description = "Monthly premium amount.",
                example = "4166.67")
        BigDecimal premioMensual,

        @Schema(description = "Day of the month for periodic payment due date (1-28). Null for PAGO_UNICO.",
                example = "20")
        Integer periodicDueDay
) {}
