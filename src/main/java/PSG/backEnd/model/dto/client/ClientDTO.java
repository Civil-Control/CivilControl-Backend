package PSG.backEnd.model.dto.client;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.dto.contactInfo.ContactInfoDTO;
import PSG.backEnd.model.enums.IvaCondition;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Schema(description = "Data Transfer Object for creating or updating a client.")
public record ClientDTO(

    @Schema(description = "CUIT of the client (optional for end consumers).", example = "20-12345678-9")
    @Pattern(regexp = "^\\d{2}-\\d{8}-\\d$", message = "{supplier.cuit.invalid}", groups = {OnCreate.class, OnUpdate.class})
    String cuit,

    @Schema(description = "Official business or legal name of the client.", example = "Empresa Ejemplo S.A.")
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 1, max = 200, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String businessName,

    @Schema(description = "Trade name or brand name of the client.", example = "Ejemplo")
    @Size(max = 200, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String tradeName,

    @Schema(description = "IVA tax condition for the client.", example = "RESPONSABLE_INSCRIPTO")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    IvaCondition ivaCondition,

    @Schema(description = "Physical address of the client.")
    @Valid
    AddressDTO address,

    @Schema(description = "Contact information for the client.")
    @Valid
    ContactInfoDTO contactInfo,

    @Schema(description = "Whether the client is active.", example = "true")
    Boolean active
) {}
