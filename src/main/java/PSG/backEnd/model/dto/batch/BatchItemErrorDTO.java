package PSG.backEnd.model.dto.batch;

public record BatchItemErrorDTO(
    int index,
    String errorMessage
) {
}
