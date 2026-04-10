package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Response DTO containing complete information about a vehicle repair.")
public record RepairResponseDTO(

        @Schema(description = "Unique identifier of the repair record.")
        Long id,

        @Schema(description = "Date when the repair was performed.")
        LocalDate date,

        @Schema(description = "ID of the vehicle that received the repair.")
        Long vehicleId,

        @Schema(description = "License plate of the vehicle.")
        String vehicleLicensePlate,

        @Schema(description = "Detailed description of the repair work performed.", nullable = true)
        String description,

        @Schema(description = "Vehicle mileage (km) at the time of repair.", nullable = true)
        Integer mileage,

        @Schema(description = "ID of the external supplier.", nullable = true)
        Long supplierId,

        @Schema(description = "Legal name of the external supplier.", nullable = true)
        String supplierLegalName,

        @Schema(description = "Trade name of the external supplier.", nullable = true)
        String supplierTradeName,

        @Schema(description = "Repair items (materials and labor).")
        List<RepairItemResponseDTO> items,

        @Schema(description = "Subtotal of material items.")
        BigDecimal materialSubtotal,

        @Schema(description = "Subtotal of labor items.")
        BigDecimal laborSubtotal,

        @Schema(description = "Total cost (sum of all items).")
        BigDecimal totalCost,

        @Schema(description = "The repair order that originated this repair, if any.", nullable = true)
        RepairOrderResponseDTO repairOrder
) {}
