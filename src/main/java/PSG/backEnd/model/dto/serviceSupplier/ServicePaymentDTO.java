package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating a service payment. " +
        "Represents a payment made for utility services (electricity, water, gas, etc.) for a specific building.")
public record ServicePaymentDTO(

        @Schema(description = "Unique identifier of the service supplier that provided the service. " +
                "Must reference an existing active service supplier in the system.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.serviceSupplierId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long serviceSupplierId,

        @Schema(description = "Unique identifier of the building where the service was provided. " +
                "Must reference an existing active building in the system.",
                example = "12",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.buildingId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long buildingId,

        @Schema(description = "Type of service paid. Valid values: " +
                "LUZ (electricity), AGUA (water), GAS (gas), INTERNET (internet), " +
                "TELEFONIA (phone), MUNICIPALES (municipal services), PROVINCIALES (provincial services), " +
                "NACIONALES (national services), OTRO (other services).",
                example = "LUZ",
                allowableValues = {"LUZ", "AGUA", "GAS", "INTERNET", "TELEFONIA", "MUNICIPALES", "PROVINCIALES", "NACIONALES", "OTRO"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.serviceType.required}", groups = OnCreate.class)
        ServiceType serviceType,

        @Schema(description = "Date when the service payment was made. " +
                "Cannot be in the future. Must be today or a past date.",
                example = "2025-11-01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{servicePayment.paymentDate.required}", groups = OnCreate.class)
        @PastOrPresent(message = "{servicePayment.paymentDate.pastOrPresent}", groups = {OnCreate.class, OnUpdate.class})
        LocalDate paymentDate,

        @Schema(description = "Amount paid for the service. Must be greater than zero. " +
                "Format: maximum 8 integer digits and 2 decimal places (e.g., 99999999.99).",
                example = "15750.50",
                minimum = "0.01",
                requiredMode = Schema.RequiredMode.REQUIRED,
                type = "number",
                format = "decimal")
        @NotNull(message = "{servicePayment.amount.required}", groups = OnCreate.class)
        @DecimalMin(value = "0.01", message = "{servicePayment.amount.positive}", groups = {OnCreate.class, OnUpdate.class})
        @Digits(integer = 8, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
        BigDecimal amount,

        @Schema(description = "Reference number or invoice number of the service payment. " +
                "Optional field for tracking purposes.",
                example = "INV-2025-001234")
        @Size(max = 100, message = "{servicePayment.referenceNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String referenceNumber,

        @Schema(description = "Additional comments or notes about the service payment. " +
                "Optional field with maximum 500 characters.",
                example = "Payment for November 2025 electricity bill")
        @Size(max = 500, message = "{servicePayment.comment.size}", groups = {OnCreate.class, OnUpdate.class})
        String comment
) {}

