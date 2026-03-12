package PSG.backEnd.model.dto.item;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Data Transfer Object for creating or updating items. " +
        "Represents purchasable goods or services that can be included in transactional documents, " +
        "such as materials, supplies, equipment, or services offered by suppliers.")
public record ItemDTO(

    @Schema(description = "Name of the item. Should be descriptive and identifiable. " +
            "This is the primary label for the item in the system. Maximum 100 characters.",
            example = "Cemento Portland tipo CPF-40",
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(groups = OnCreate.class, message = "{item.name.required}")
    @Size(max = 100, groups = {OnCreate.class, OnUpdate.class}, message = "{item.name.size}")
    String name,

    @Schema(description = "Detailed description of the item including specifications, characteristics, or usage notes. " +
            "Optional field. Maximum 500 characters.",
            example = "Cemento de alta resistencia para uso en construcción estructural. Presentación en bolsas de 50kg.",
            maxLength = 500,
            nullable = true)
    @Size(max = 500, groups = {OnCreate.class, OnUpdate.class}, message = "{item.description.size}")
    String description
) {}
