package PSG.backEnd.model.dto.tenant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filter DTO for searching and filtering tenants based on various criteria.")
public record TenantFilterDTO(
    String name,
    String cuit,
    Boolean active,
    String search
) {}
