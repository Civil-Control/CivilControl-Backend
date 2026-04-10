package PSG.backEnd.model.dto.tenant;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Data Transfer Object for creating or updating tenants.")
public record TenantDTO(
    @NotBlank(groups = OnCreate.class, message = "{validation.required}")
    @Size(max = 150, groups = {OnCreate.class, OnUpdate.class}, message = "{tenant.name.size}")
    String name,

    @NotBlank(groups = OnCreate.class, message = "{validation.required}")
    @Pattern(regexp = "^\\d{2}-\\d{8}-\\d{1}$", groups = {OnCreate.class, OnUpdate.class}, message = "{tenant.cuit.format}")
    String cuit,

    @Size(max = 200, groups = {OnCreate.class, OnUpdate.class}, message = "{tenant.legalName.size}")
    String legalName,

    @Valid
    AddressDTO address,

    @Size(max = 30, groups = {OnCreate.class, OnUpdate.class}, message = "{tenant.phone.size}")
    String phone,

    @Email(groups = {OnCreate.class, OnUpdate.class}, message = "{tenant.email.format}")
    @Size(max = 100, groups = {OnCreate.class, OnUpdate.class}, message = "{tenant.email.size}")
    String email,

    @Size(max = 500, groups = {OnCreate.class, OnUpdate.class}, message = "{tenant.logoUrl.size}")
    String logoUrl,

    LocalDate foundedDate,

    Boolean active,

    @Schema(description = "IVA tax condition of the tenant.",
            example = "RESPONSABLE_INSCRIPTO")
    PSG.backEnd.model.enums.IvaCondition ivaCondition
) {}
