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

        @Schema(description = "Subtotal of material items (net amounts, no IVA).")
        BigDecimal materialSubtotal,

        @Schema(description = "Subtotal of labor items (net amounts, no IVA).")
        BigDecimal laborSubtotal,

        @Schema(description = "Total cost summing every item amount (net, no IVA). Kept for backwards " +
                "compatibility with filters and sorting.")
        BigDecimal totalCost,

        @Schema(description = "Sum of IVA amounts of every item that is linked to a transactional document. " +
                "Items not linked to any document do not contribute to this total because their IVA is unknown.")
        BigDecimal totalIva,

        @Schema(description = "Effective amount actually spent on this repair: {@link #totalCost} plus " +
                "{@link #totalIva}. This is the figure the UI surfaces as the headline total.")
        BigDecimal totalWithIva,

        @Schema(description = "The repair order that originated this repair, if any.", nullable = true)
        RepairOrderResponseDTO repairOrder
) {}
