package PSG.backEnd.model.dto.report.repair;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "A single repair entry within the report")
public record RepairReportItemDTO(

    @Schema(description = "Repair ID")
    Long id,

    @Schema(description = "Date of the repair")
    LocalDate date,

    @Schema(description = "Repair description")
    String description,

    @Schema(description = "Mileage at the time of repair", nullable = true)
    Integer mileage,

    @Schema(description = "Material cost (sum of MATERIAL items)")
    BigDecimal materialCost,

    @Schema(description = "Labor cost (sum of MANO_DE_OBRA items)")
    BigDecimal laborCost,

    @Schema(description = "Total cost (materialCost + laborCost)")
    BigDecimal totalCost,

    @Schema(description = "IVA total (sum of IVA amounts for items linked to a document; negative for credit note items)")
    BigDecimal totalIva,

    @Schema(description = "Total including IVA (totalCost + totalIva)")
    BigDecimal totalWithIva,

    @Schema(description = "Supplier name (for external repairs)", nullable = true)
    String supplierName,

    @Schema(description = "Number of repair items")
    int itemCount,

    @Schema(description = "Whether any item has a linked document")
    Boolean hasLinkedDocuments,

    @Schema(description = "Vehicle license plate", nullable = true)
    String vehicleLicensePlate,

    @Schema(description = "Vehicle description (brand + model)", nullable = true)
    String vehicleDescription,

    @Schema(description = "Project area name")
    String projectAreaName
) {}
