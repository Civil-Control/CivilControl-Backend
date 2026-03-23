package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Response DTO containing complete information about a repair order.")
public record RepairOrderResponseDTO(

        @Schema(description = "Unique identifier of the repair order.", example = "10")
        Long id,

        @Schema(description = "Date when the failure was reported.", example = "2024-10-01")
        LocalDate date,

        @Schema(description = "ID of the vehicle that has the failure.", example = "15")
        Long vehicleId,

        @Schema(description = "License plate of the vehicle.", example = "ABC123")
        String vehicleLicensePlate,

        @Schema(description = "Description of the failure reported by the operator.")
        String description,

        @Schema(description = "Name of the field operator who reported the failure.", nullable = true)
        String reportedBy,

        @Schema(description = "Current status of the repair order.")
        RepairOrderStatus status,

        @Schema(description = "Full name of the user who created the order.", nullable = true)
        String createdByUserName
) {}
