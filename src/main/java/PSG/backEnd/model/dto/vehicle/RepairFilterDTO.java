package PSG.backEnd.model.dto.vehicle;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Filter criteria for querying vehicle repairs. All fields are optional and can be combined.")
public record RepairFilterDTO(

        @Schema(description = "Filter repairs from this date (inclusive).", nullable = true)
        LocalDate dateFrom,

        @Schema(description = "Filter repairs to this date (inclusive).", nullable = true)
        LocalDate dateTo,

        @Schema(description = "Filter by vehicle ID.", nullable = true)
        Long vehicleId,

        @Schema(description = "Filter by vehicle license plate. Partial matches are supported.", nullable = true)
        String vehicleLicensePlate,

        @Schema(description = "Filter by project area ID.", nullable = true)
        Long projectAreaId,

        @Schema(description = "Minimum total cost (sum of items, inclusive).", nullable = true)
        BigDecimal minCost,

        @Schema(description = "Maximum total cost (sum of items, inclusive).", nullable = true)
        BigDecimal maxCost,

        @Schema(description = "Filter by supplier ID.", nullable = true)
        Long supplierId,

        @Schema(description = "Filter by supplier legal name. Partial matches are supported.", nullable = true)
        String supplierLegalName,

        @Schema(description = "Filter by item description. Partial matches are supported.", nullable = true)
        String itemDescription,

        @Schema(description = "Minimum mileage (inclusive).", nullable = true)
        Integer minMileage,

        @Schema(description = "Maximum mileage (inclusive).", nullable = true)
        Integer maxMileage,

        @Schema(description = "Generic search across vehicle license plate, supplier name and item descriptions.", nullable = true)
        String search,

        @Schema(description = "Filter by linked transactional document ID on any item.", nullable = true)
        Long transactionalDocumentId
) {}
