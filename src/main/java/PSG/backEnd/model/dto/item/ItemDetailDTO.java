package PSG.backEnd.model.dto.item;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ItemDetailDTO(

    @NotNull(groups = OnUpdate.class, message = "ID is required for update operations")
    Long id,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "Item ID is required")
    Long itemId,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "Unit amount is required")
    @DecimalMin(value = "0.01", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "Unit amount must be greater than 0")
    @Digits(integer = 17, fraction = 2, groups = {OnCreate.class, OnUpdate.class}, message = "Unit amount must have at most 2 decimal places")
    BigDecimal unitAmount,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "Quantity is required")
    @Min(value = 1, groups = {OnCreate.class, OnUpdate.class}, message = "Quantity must be at least 1")
    Integer quantity,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "IVA percentage is required")
    @DecimalMin(value = "0.00", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "IVA percentage must be at least 0")
    @DecimalMax(value = "100.00", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "IVA percentage must not exceed 100")
    @Digits(integer = 3, fraction = 2, groups = {OnCreate.class, OnUpdate.class}, message = "IVA percentage must have at most 2 decimal places")
    BigDecimal ivaPercentage
) {}
