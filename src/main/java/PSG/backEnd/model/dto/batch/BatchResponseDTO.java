package PSG.backEnd.model.dto.batch;

import java.util.List;

public record BatchResponseDTO<T>(
    List<T> successful,
    List<BatchItemErrorDTO> failed,
    int totalRequested,
    int totalSuccessful,
    int totalFailed
) {
}
