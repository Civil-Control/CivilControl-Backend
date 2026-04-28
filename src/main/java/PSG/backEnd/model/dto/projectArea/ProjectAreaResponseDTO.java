package PSG.backEnd.model.dto.projectArea;

public record ProjectAreaResponseDTO(
    Long id,
    String name,
    String description,
    Boolean active,
    Boolean deleted,
    String color,
    /** True when this area is the tenant's hidden Value Recovery sector (Feature 18). */
    Boolean isRecoverySector
) {}