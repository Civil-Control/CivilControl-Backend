package PSG.backEnd.model.dto.contactInfo;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;
import java.util.List;

public record ContactInfoDTO(
    @NotEmpty(message = "At least one email is required", groups = OnCreate.class)
    List<@Email(message = "Must be a valid email address", groups = {OnCreate.class, OnUpdate.class})
         @NotBlank(message = "Email cannot be blank", groups = {OnCreate.class, OnUpdate.class})
         @Size(max = 100, message = "Email must not exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
         String> email,

    @NotEmpty(message = "At least one phone number is required", groups = OnCreate.class)
    List<@Pattern(regexp = "^\\+?[0-9]{10,15}$",
                 message = "Phone number must be between 10 and 15 digits, optionally starting with +",
                 groups = {OnCreate.class, OnUpdate.class})
         String> phoneNumber
) {}
