package PSG.backEnd.model.dto.item;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemDTO(

    @NotNull(groups = OnUpdate.class, message = "ID is required for update operations")
    Long id,

    @NotBlank(groups = OnCreate.class, message = "Name is required")
    @Size(max = 100, groups = {OnCreate.class, OnUpdate.class}, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 500, groups = {OnCreate.class, OnUpdate.class}, message = "Description must not exceed 500 characters")
    String description
) {}
