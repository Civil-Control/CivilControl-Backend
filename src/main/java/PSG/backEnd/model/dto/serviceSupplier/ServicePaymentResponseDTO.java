package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.ServiceCategory;
import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Response Data Transfer Object for service payment. " +
        "Contains complete information about a service payment including assignment, supplier, building, and payment details.")
public record ServicePaymentResponseDTO(

        @Schema(description = "Unique identifier of the service payment record.")
        Long id,

        @Schema(description = "ID of the service assignment.")
        Long serviceAssignmentId,

        @Schema(description = "ID of the service supplier.")
        Long serviceSupplierId,

        @Schema(description = "Legal name of the supplier.")
        String supplierName,

        @Schema(description = "Trade name of the supplier.")
        String supplierTradeName,

        @Schema(description = "CUIT of the supplier.")
        String supplierCuit,

        @Schema(description = "ID of the building that receives the service.")
        Long buildingId,

        @Schema(description = "Name of the building that receives the service.")
        String buildingName,

        @Schema(description = "Type of service.")
        ServiceType serviceType,

        @Schema(description = "Category of the service.")
        ServiceCategory serviceCategory,

        @Schema(description = "Account number for the service.")
        String accountNumber,

        @Schema(description = "Account holder name.")
        String accountHolder,

        @Schema(description = "Date when the service payment was made.")
        LocalDate paymentDate,

        @Schema(description = "Amount paid for the service.")
        BigDecimal amount,

        @Schema(description = "Reference number or invoice number of the payment.")
        String referenceNumber,

        @Schema(description = "Additional comments or notes about the payment.")
        String comment,

        @Schema(description = "ID of the project area for cost attribution.")
        Long projectAreaId,

        @Schema(description = "Name of the project area.")
        String projectAreaName,

        @Schema(description = "Color of the project area.")
        String projectAreaColor
) {}

