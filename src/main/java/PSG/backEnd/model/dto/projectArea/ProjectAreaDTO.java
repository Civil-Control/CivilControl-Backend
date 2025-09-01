package PSG.backEnd.model.dto.projectArea;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectAreaDTO(

    @NotBlank(message = "Name cannot be blank", groups = {OnCreate.class})
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters", groups = {OnCreate.class, OnUpdate.class})
    String name,

    @Size(min = 1, max = 200, message = "Description must not exceed 200 characters", groups = {OnCreate.class, OnUpdate.class})
    String description,

    Boolean active
) {}