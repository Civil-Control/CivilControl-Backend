package PSG.backEnd.model.dto.gasStation;

import java.util.List;

public record FuelLoadBatchResponseDTO(
    List<FuelLoadResponseDTO> successfulLoads,
    List<FuelLoadErrorDTO> failedLoads,
    int totalRequested,
    int totalSuccessful,
    int totalFailed
) {
}
