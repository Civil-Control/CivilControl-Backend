package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response Data Transfer Object for service payment. " +
        "Contains complete information about a service payment including supplier, building, and payment details.")
public record ServicePaymentResponseDTO(

        @Schema(description = "Unique identifier of the service payment record.",
                example = "42")
        Long id,

        @Schema(description = "Unique identifier of the service supplier.",
                example = "5")
        Long serviceSupplierId,

        @Schema(description = "Name of the service supplier.",
                example = "Empresa Provincial de Energía")
        String supplierName,

        @Schema(description = "Unique identifier of the building.",
                example = "12")
        Long buildingId,

        @Schema(description = "Name of the building where the service was provided.",
                example = "Edificio Central")
        String buildingName,

        @Schema(description = "Type of service paid. Values: LUZ, AGUA, GAS, INTERNET, TELEFONIA, MUNICIPALES, PROVINCIALES, NACIONALES, OTRO.",
                example = "LUZ")
        ServiceType serviceType,

        @Schema(description = "Date when the service payment was made.",
                example = "2025-11-01")
        LocalDate paymentDate,

        @Schema(description = "Amount paid for the service.",
                example = "15750.50")
        BigDecimal amount,

        @Schema(description = "Reference number or invoice number of the payment.",
                example = "INV-2025-001234")
        String referenceNumber,

        @Schema(description = "Additional comments or notes about the payment.",
                example = "Payment for November 2025 electricity bill")
        String comment
) {}

