package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmergencyContactDTO(
    @NotBlank(message = "Emergency contact name cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "Emergency contact name must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    String name,

    @NotBlank(message = "Emergency contact phone number cannot be blank", groups = OnCreate.class)
    @Pattern(regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$",
             message = "Invalid phone number format",
             groups = {OnCreate.class, OnUpdate.class})
    String phoneNumber,

    @NotBlank(message = "Relationship cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 50, message = "Relationship must be between 2 and 50 characters", groups = {OnCreate.class, OnUpdate.class})
    String relationship
) {}

