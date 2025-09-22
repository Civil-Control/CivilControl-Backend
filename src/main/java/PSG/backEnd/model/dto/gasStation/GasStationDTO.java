package PSG.backEnd.model.dto.gasStation;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GasStationDTO(
        @NotNull(message = "Supplier id is required.", groups = OnCreate.class)
        @Positive(message = "Supplier id must be positive.", groups = {OnCreate.class, OnUpdate.class})
        Long supplierId,


        @NotNull(message = "Prices list is required.", groups = OnCreate.class)
        @Size(min = 1, message = "At least one price is required.", groups = {OnCreate.class, OnUpdate.class})
        List<@Valid GasStationPriceDTO> prices,

        boolean deleted
) {}