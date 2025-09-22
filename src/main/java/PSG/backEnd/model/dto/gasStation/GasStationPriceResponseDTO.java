package PSG.backEnd.model.dto.gasStation;

public record GasStationPriceResponseDTO(
        String supplierName,
        String fuelType,
        Double price
) {}