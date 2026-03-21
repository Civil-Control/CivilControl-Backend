package PSG.backEnd.model.dto.tenant;

import PSG.backEnd.model.dto.address.AddressResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Response DTO containing complete information about a tenant.")
public record TenantResponseDTO(
    Long id,
    String name,
    String cuit,
    String legalName,
    AddressResponseDTO address,
    String phone,
    String email,
    String logoUrl,
    LocalDate foundedDate,
    Boolean active,
    Boolean deleted
) {}
