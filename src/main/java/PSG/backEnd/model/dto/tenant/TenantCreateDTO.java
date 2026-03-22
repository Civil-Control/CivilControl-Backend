package PSG.backEnd.model.dto.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO for creating a new tenant with its owner user.")
public record TenantCreateDTO(

    @NotNull(message = "{validation.required}")
    @Valid
    TenantDTO tenant,

    @NotBlank(message = "{validation.required}")
    @Email(message = "{tenant.owner.email.format}")
    @Size(max = 100, message = "{tenant.owner.email.size}")
    @Schema(description = "Email of the owner user", example = "owner@company.com")
    String ownerEmail,

    @NotBlank(message = "{validation.required}")
    @Size(min = 3, max = 50, message = "{tenant.owner.username.size}")
    @Schema(description = "Username of the owner user", example = "admin")
    String ownerUsername,

    @NotBlank(message = "{validation.required}")
    @Size(min = 6, max = 100, message = "{tenant.owner.password.size}")
    @Schema(description = "Password of the owner user")
    String ownerPassword,

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{tenant.owner.firstName.size}")
    @Schema(description = "First name of the owner user", example = "Juan")
    String ownerFirstName,

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{tenant.owner.lastName.size}")
    @Schema(description = "Last name of the owner user", example = "Pérez")
    String ownerLastName
) {}
