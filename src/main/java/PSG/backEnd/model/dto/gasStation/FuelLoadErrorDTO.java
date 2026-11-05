package PSG.backEnd.model.dto.gasStation;

public record FuelLoadErrorDTO(
    int index,
    FuelLoadDTO fuelLoadDTO,
    String errorMessage
) {
}
