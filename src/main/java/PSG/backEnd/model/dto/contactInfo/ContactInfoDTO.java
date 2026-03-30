package PSG.backEnd.model.dto.contactInfo;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "Data Transfer Object for contact information. " +
        "Contains a reference name, email addresses and phone numbers for communication purposes.")
public record ContactInfoDTO(

    @Schema(description = "Reference name to identify this contact (e.g. 'Oficina Central', 'Juan Pérez').",
            example = "Oficina Central",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{contactInfo.referenceName.required}", groups = OnCreate.class)
    @Size(min = 1, max = 100, message = "{contactInfo.referenceName.size}", groups = {OnCreate.class, OnUpdate.class})
    String referenceName,

    @Schema(description = "List of email addresses. Optional field. " +
            "Each email must be in valid format and not exceed 100 characters. Empty strings are ignored.",
            example = "[\"contacto@example.com\", \"ventas@example.com\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
    List<@Email(message = "{contactInfo.email.invalid}", groups = {OnCreate.class, OnUpdate.class})
         @Size(max = 100, message = "{validation.email}", groups = {OnCreate.class, OnUpdate.class})
         String> email,

    @Schema(description = "List of phone numbers. Optional field. " +
            "Each phone number must be between 10 and 15 digits, optionally starting with + for international format. " +
            "Maximum 30 characters per phone number. Empty strings are ignored.",
            example = "[\"+5493511234567\", \"+5493519876543\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
    List<@Pattern(regexp = "^(\\+?[0-9]{10,15})?$",
                 message = "{validation.size}",
                 groups = {OnCreate.class, OnUpdate.class})
         @Size(max = 30, message = "{contactInfo.phoneNumber.size}", groups = {OnCreate.class, OnUpdate.class})
         String> phoneNumber
) {}
