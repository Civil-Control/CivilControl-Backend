package PSG.backEnd.model.dto.stock;

import PSG.backEnd.model.enums.StockCategory;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record StockDTO(

    @NotBlank(groups = OnCreate.class, message = "Name is required")
    @Size(max = 100, groups = {OnCreate.class, OnUpdate.class}, message = "Name must not exceed 100 characters")
    String name,

    @NotNull(groups = OnCreate.class, message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "Quantity must be greater than or equal to 0")
    @Digits(integer = 8, fraction = 2, groups = {OnCreate.class, OnUpdate.class}, message = "Quantity must have at most 8 integer digits and 2 decimal places")
    BigDecimal quantity,

    @Size(max = 200, groups = {OnCreate.class, OnUpdate.class}, message = "Location must not exceed 200 characters")
    String location,

    @NotNull(groups = OnCreate.class, message = "Stock category is required")
    StockCategory stockCategory
) {}
