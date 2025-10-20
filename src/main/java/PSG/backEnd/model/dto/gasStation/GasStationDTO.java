package PSG.backEnd.model.dto.gasStation;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating a gas station. " +
        "Represents a fuel supplier location with its pricing information for different fuel types.")
public record GasStationDTO(

        @Schema(description = "ID of the supplier that owns this gas station.",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Supplier id is required.", groups = OnCreate.class)
        @Positive(message = "Supplier id must be positive.", groups = {OnCreate.class, OnUpdate.class})
        Long supplierId,

        @Schema(description = "List of fuel prices offered at this gas station. At least one price is required.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Prices list is required.", groups = OnCreate.class)
        @Size(min = 1, message = "At least one price is required.", groups = {OnCreate.class, OnUpdate.class})
        List<@Valid GasStationPriceDTO> prices,

        @Schema(description = "Soft deletion flag. When true, the gas station is marked as deleted but remains in the database.",
                example = "false")
        boolean deleted
) {}