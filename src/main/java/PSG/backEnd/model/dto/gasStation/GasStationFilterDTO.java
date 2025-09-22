package PSG.backEnd.model.dto.gasStation;

import java.util.List;

public record GasStationFilterDTO(
        Long supplierId,
        List<String> fuelTypes
) {}
