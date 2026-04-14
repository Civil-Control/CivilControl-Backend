package PSG.backEnd.model.dto.gasStation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO containing complete information about a fuel load transaction, " +
        "including calculated amounts and related entity details.")
public record FuelLoadResponseDTO(

        @Schema(description = "Unique identifier of the fuel load transaction.",
                example = "42")
        Long id,

        @Schema(description = "Date when the fuel load was made.",
                example = "2024-10-15")
        String date,

        @Schema(description = "Gas station branch code.",
                example = "12345")
        String branchCode,

        @Schema(description = "Fuel purchase ticket number.",
                example = "87654321")
        String ticketNumber,

        @Schema(description = "Type of fuel loaded.",
                example = "NAFTA_SUPER",
                allowableValues = {"NAFTA_SUPER", "NAFTA_COMUN", "DIESEL", "GNC"})
        String fuelType,

        @Schema(description = "Amount of fuel loaded in liters.",
                example = "45.50")
        Double liters,

        @Schema(description = "Price per liter at the time of purchase.",
                example = "850.5500")
        Double pricePerLiter,

        @Schema(description = "Total amount paid for the fuel load (liters × price per liter).",
                example = "38700.025")
        Double totalAmount,

        @Schema(description = "ID of the vehicle that received the fuel load.",
                example = "25")
        Long vehicleId,

        @Schema(description = "License plate of the vehicle that received the fuel load.",
                example = "ABC 123")
        String vehicleLicensePlate,

        @Schema(description = "ID of the project area to which this fuel load is assigned.",
                example = "10")
        Long projectAreaId,

        @Schema(description = "Name of the project area to which this fuel load is assigned.",
                example = "Operations Department")
        String projectAreaName,

        @Schema(description = "Color of the project area to which this fuel load is assigned.",
                example = "#3b82f6",
                nullable = true)
        String projectAreaColor,

        @Schema(description = "ID of the project area task (sub-task).", nullable = true)
        Long projectAreaTaskId,

        @Schema(description = "Name of the project area task (sub-task).", nullable = true)
        String projectAreaTaskName,

        @Schema(description = "ID of the gas station where the fuel load was made.",
                example = "5")
        Long gasStationId,

        @Schema(description = "Name of the gas station where the fuel load was made.",
                example = "Shell Station Downtown")
        String gasStationName,

        @Schema(description = "Linked transactional document summary, if any.", nullable = true)
        PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentSummaryDTO transactionalDocument,

        Integer documentSortOrder
) {}