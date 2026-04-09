package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO containing complete information about a service assignment.")
public record ServiceAssignmentResponseDTO(

        @Schema(description = "Unique identifier of the service assignment.")
        Long id,

        @Schema(description = "Subject type: BUILDING or VEHICLE.")
        SubjectType subjectType,

        @Schema(description = "ID of the service supplier.")
        Long serviceSupplierId,

        @Schema(description = "Legal name of the supplier.")
        String supplierName,

        @Schema(description = "Trade name of the supplier.")
        String supplierTradeName,

        @Schema(description = "CUIT of the supplier.")
        String supplierCuit,

        @Schema(description = "ID of the building (null for vehicle assignments).")
        Long buildingId,

        @Schema(description = "Name of the building (null for vehicle assignments).")
        String buildingName,

        @Schema(description = "ID of the vehicle (null for building assignments).")
        Long vehicleId,

        @Schema(description = "License plate of the vehicle (null for building assignments).")
        String vehicleLicensePlate,

        @Schema(description = "Type of service assigned.")
        ServiceType serviceType,

        @Schema(description = "Account number for the service.")
        String accountNumber,

        @Schema(description = "Estimated day of the month when the service is due.")
        Integer estimatedDueDay,

        @Schema(description = "Name of the person or entity responsible for the service payment.")
        String accountHolder,

        @Schema(description = "ID of the building from where the payment is issued.")
        Long paymentLocationId,

        @Schema(description = "Name of the building from where the payment is issued.")
        String paymentLocationName,

        @Schema(description = "Project area ID inherited from the subject (building or vehicle).")
        Long subjectProjectAreaId,

        @Schema(description = "Project area name inherited from the subject.")
        String subjectProjectAreaName
) {}
