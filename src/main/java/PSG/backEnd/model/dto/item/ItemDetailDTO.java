package PSG.backEnd.model.dto.item;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ItemDetailDTO(

    Long id,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "{validation.required}")
    Long itemId,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "{validation.required}")
    @DecimalMin(value = "0.01", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.positive}")
    @Digits(integer = 17, fraction = 2, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.pattern}")
    BigDecimal unitAmount,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "{validation.required}")
    @Min(value = 1, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.positive}")
    Integer quantity,

    @NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "{validation.required}")
    @DecimalMin(value = "0.00", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.positiveOrZero}")
    @DecimalMax(value = "100.00", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.max}")
    @Digits(integer = 3, fraction = 2, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.pattern}")
    BigDecimal ivaPercentage,

    @DecimalMin(value = "0.01", inclusive = true, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.positive}")
    @Digits(integer = 19, fraction = 2, groups = {OnCreate.class, OnUpdate.class}, message = "{validation.pattern}")
    BigDecimal totalAmount
) {}
