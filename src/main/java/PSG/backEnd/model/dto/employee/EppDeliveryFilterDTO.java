package PSG.backEnd.model.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Filter Data Transfer Object for EPP (Personal Protective Equipment) deliveries. " +
        "Used to filter and search EPP deliveries by employee, date range, item details, and brand.")
public record EppDeliveryFilterDTO(
    @Schema(description = "Filter by employee ID. Returns only EPP deliveries for the specified employee.",
            example = "25",
            nullable = true)
    Long employeeId,

    @Schema(description = "Filter by minimum delivery date. Returns EPP deliveries from this date onwards.",
            example = "2025-01-01",
            nullable = true)
    LocalDate deliveryDateFrom,

    @Schema(description = "Filter by maximum delivery date. Returns EPP deliveries up to this date.",
            example = "2025-12-31",
            nullable = true)
    LocalDate deliveryDateTo,

    @Schema(description = "Filter by item name. Searches for EPP deliveries containing this text in the item name (case-insensitive).",
            example = "casco",
            nullable = true)
    String itemName,

    @Schema(description = "Filter by item type. Searches for EPP deliveries containing this text in the item type (case-insensitive).",
            example = "protección",
            nullable = true)
    String itemType,

    @Schema(description = "Filter by brand. Searches for EPP deliveries containing this text in the brand name (case-insensitive).",
            example = "3M",
            nullable = true)
    String brand
) {}

