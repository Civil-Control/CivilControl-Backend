package PSG.backEnd.model.dto.insurance;

import PSG.backEnd.model.enums.vehicle.PaymentFrequency;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Filter criteria for querying insurance policies. All fields are optional and can be combined.")
public record InsurancePolicyFilterDTO(

        @Schema(description = "Filter by policy number. Partial matches are supported.",
                example = "POL-2024",
                nullable = true)
        String policyNumber,

        @Schema(description = "Filter by term number. Partial matches are supported.",
                example = "2024-01",
                nullable = true)
        String termNumber,

        @Schema(description = "Filter by policy type.",
                example = "AUTOMOTOR",
                allowableValues = {"AUTOMOTOR"},
                nullable = true)
        PolicyType policyType,

        @Schema(description = "Filter by policy status.",
                example = "ACTIVO",
                allowableValues = {"COTIZADO", "ACTIVO", "VENCIDO", "CANCELADO", "EXPIRADO"},
                nullable = true)
        PolicyStatus policyStatus,

        @Schema(description = "Filter by payment frequency.",
                example = "MENSUAL",
                allowableValues = {"MENSUAL", "BIMESTRAL", "TRIMESTRAL", "SEMI_ANUAL", "ANUAL", "PAGO_UNICO"},
                nullable = true)
        PaymentFrequency paymentFrequency,

        @Schema(description = "Filter policies issued from this date (inclusive).",
                example = "2024-01-01",
                nullable = true)
        LocalDate issueDateFrom,

        @Schema(description = "Filter policies issued to this date (inclusive).",
                example = "2024-12-31",
                nullable = true)
        LocalDate issueDateTo,

        @Schema(description = "Filter policies with effective from date starting from this date (inclusive).",
                example = "2024-01-01",
                nullable = true)
        LocalDate effectiveFromStart,

        @Schema(description = "Filter policies with effective from date ending at this date (inclusive).",
                example = "2024-12-31",
                nullable = true)
        LocalDate effectiveFromEnd,

        @Schema(description = "Filter policies with effective to date starting from this date (inclusive).",
                example = "2024-01-01",
                nullable = true)
        LocalDate effectiveToStart,

        @Schema(description = "Filter policies with effective to date ending at this date (inclusive).",
                example = "2025-12-31",
                nullable = true)
        LocalDate effectiveToEnd,

        @Schema(description = "Filter by cancellation status. True for cancelled policies, false for non-cancelled, null for all.",
                example = "false",
                nullable = true)
        Boolean isCancelled
) {}
