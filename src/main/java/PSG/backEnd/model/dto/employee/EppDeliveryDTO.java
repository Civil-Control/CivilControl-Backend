package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating an EPP (Personal Protective Equipment) delivery. " +
        "Represents the delivery of safety equipment to an employee, including item details and quantity.")
public record EppDeliveryDTO(
    @Schema(description = "Unique identifier of the employee receiving the EPP. " +
            "Must reference an existing employee in the system.",
            example = "25",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Employee ID cannot be null", groups = OnCreate.class)
    Long employeeId,

    @Schema(description = "Date when the EPP was delivered to the employee. " +
            "Cannot be in the future. Must be today or a past date for accurate tracking.",
            example = "2025-11-10",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Delivery date cannot be null", groups = OnCreate.class)
    @PastOrPresent(message = "Delivery date cannot be in the future", groups = {OnCreate.class, OnUpdate.class})
    LocalDate deliveryDate,

    @Schema(description = "Name of the EPP item delivered. Must be between 2 and 150 characters. " +
            "Should be descriptive and specific (e.g., 'Safety Helmet', 'Steel-Toe Boots', 'High-Visibility Vest').",
            example = "Casco de seguridad",
            minLength = 2,
            maxLength = 150,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Item name cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 150, message = "Item name must be between 2 and 150 characters", groups = {OnCreate.class, OnUpdate.class})
    String itemName,

    @Schema(description = "Type or category of the EPP item. Must be between 2 and 100 characters. " +
            "Indicates the protection category (e.g., 'Head Protection', 'Foot Protection', 'Respiratory Protection').",
            example = "Protección craneal",
            minLength = 2,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Item type cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "Item type must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    String itemType,

    @Schema(description = "Brand or manufacturer of the EPP item. Optional field, maximum 100 characters. " +
            "Useful for quality control and warranty tracking.",
            example = "3M",
            maxLength = 100,
            nullable = true)
    @Size(max = 100, message = "Brand must not exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
    String brand,

    @Schema(description = "Quantity of items delivered. Must be a positive integer greater than zero. " +
            "Represents the number of units of the same EPP item delivered.",
            example = "2",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity cannot be null", groups = OnCreate.class)
    @Min(value = 1, message = "Quantity must be at least 1", groups = {OnCreate.class, OnUpdate.class})
    Integer quantity
) {}

