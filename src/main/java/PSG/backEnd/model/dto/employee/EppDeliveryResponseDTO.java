package PSG.backEnd.model.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Response Data Transfer Object for EPP (Personal Protective Equipment) delivery. " +
        "Contains complete information about an EPP delivery including employee details and item specifications.")
public record EppDeliveryResponseDTO(
    @Schema(description = "Unique identifier of the EPP delivery record.",
            example = "45")
    Long id,

    @Schema(description = "Unique identifier of the employee who received the EPP.",
            example = "25")
    Long employeeId,

    @Schema(description = "First name of the employee who received the EPP.",
            example = "Juan Carlos")
    String employeeName,

    @Schema(description = "Last name of the employee who received the EPP.",
            example = "García Pérez")
    String employeeLastName,

    @Schema(description = "Date when the EPP was delivered to the employee.",
            example = "2025-11-10")
    LocalDate deliveryDate,

    @Schema(description = "Name of the EPP item delivered.",
            example = "Casco de seguridad")
    String itemName,

    @Schema(description = "Type or category of the EPP item.",
            example = "Protección craneal")
    String itemType,

    @Schema(description = "Brand or manufacturer of the EPP item.",
            example = "3M",
            nullable = true)
    String brand,

    @Schema(description = "Quantity of items delivered.",
            example = "2")
    Integer quantity
) {}

