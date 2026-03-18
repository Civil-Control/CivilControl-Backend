package PSG.backEnd.model.dto.projectArea;

public record ProjectAreaResponseDTO(
    Long id,
    String name,
    String description,
    Boolean active,
    Boolean deleted,
    String color
) {}