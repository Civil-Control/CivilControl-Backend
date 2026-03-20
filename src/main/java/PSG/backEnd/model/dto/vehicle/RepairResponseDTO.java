package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Response DTO containing complete information about a vehicle repair, " +
        "including related vehicle and supplier details.")
public record RepairResponseDTO(

        @Schema(description = "Unique identifier of the repair record.",
                example = "23")
        Long id,

        @Schema(description = "Date when the repair was performed.",
                example = "2024-10-01")
        LocalDate date,

        @Schema(description = "ID of the vehicle that received the repair.",
                example = "15")
        Long vehicleId,

        @Schema(description = "License plate of the vehicle that received the repair.",
                example = "ABC123")
        String vehicleLicensePlate,

        @Schema(description = "Total cost of the repair. May be null for internal employee repairs.",
                example = "1500.50",
                nullable = true)
        BigDecimal cost,

        @Schema(description = "Detailed description of the repair work performed.",
                example = "Replaced brake pads and rotors on front wheels. Performed full brake system inspection.",
                nullable = true)
        String description,

        @Schema(description = "Name of the internal employee who performed the repair. " +
                "Null if the repair was performed by an external supplier.",
                example = "John Smith",
                nullable = true)
        String employee,

        @Schema(description = "ID of the external supplier who performed the repair. " +
                "Null if the repair was performed by an internal employee.",
                example = "8",
                nullable = true)
        Long supplierId,

        @Schema(description = "Legal name of the external supplier who performed the repair. " +
                "Null if the repair was performed by an internal employee.",
                example = "Auto Repairs Inc.",
                nullable = true)
        String supplierLegalName,

        @Schema(description = "Trade name of the external supplier who performed the repair. " +
                "Null if the repair was performed by an internal employee.",
                example = "AutoFix",
                nullable = true)
        String supplierTradeName,

        @Schema(description = "Types of repair performed.",
                example = "[\"ARRANQUE\", \"SISTEMA_ELECTRICO\"]")
        List<String> repairTypes
) {}
