package PSG.backEnd.model.dto.contactInfo;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "Data Transfer Object for contact information. " +
        "Contains email addresses and phone numbers for communication purposes. " +
        "At least one email and one phone number are required.")
public record ContactInfoDTO(

    @Schema(description = "List of email addresses. At least one valid email address is required. " +
            "Each email must be in valid format and not exceed 100 characters.",
            example = "[\"contacto@example.com\", \"ventas@example.com\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "{validation.required}", groups = OnCreate.class)
    List<@Email(message = "Must be a valid email address", groups = {OnCreate.class, OnUpdate.class})
         @NotBlank(message = "{validation.email}", groups = {OnCreate.class, OnUpdate.class})
         @Size(max = 100, message = "{validation.email}", groups = {OnCreate.class, OnUpdate.class})
         String> email,

    @Schema(description = "List of phone numbers. At least one valid phone number is required. " +
            "Each phone number must be between 10 and 15 digits, optionally starting with + for international format. " +
            "Maximum 30 characters per phone number.",
            example = "[\"+5493511234567\", \"+5493519876543\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "{validation.required}", groups = OnCreate.class)
    List<@Pattern(regexp = "^\\+?[0-9]{10,15}$",
                 message = "{validation.size}",
                 groups = {OnCreate.class, OnUpdate.class})
         @Size(max = 30, message = "Phone number must not exceed 30 characters", groups = {OnCreate.class, OnUpdate.class})
         String> phoneNumber
) {}
