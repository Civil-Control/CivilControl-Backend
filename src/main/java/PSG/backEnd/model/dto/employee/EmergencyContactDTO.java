package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Data Transfer Object for emergency contact. " +
        "Represents contact information for a person to be notified in case of an employee emergency.")
public record EmergencyContactDTO(
    @Schema(description = "Full name of the emergency contact person. Minimum 2 characters, maximum 100 characters.",
            example = "María González",
            minLength = 2,
            maxLength = 100,
            nullable = true)
    @Size(max = 100, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String name,

    @Schema(description = "Phone number of the emergency contact. Can include country code, area code, and must be in valid phone format. " +
            "This number will be used to reach the contact in case of emergency situations. Maximum 30 characters.",
            example = "+54 9 11 9876-5432",
            maxLength = 30,
            nullable = true)
    @Size(max = 30, message = "{emergencyContact.phoneNumber.size}", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^$|^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$",
             message = "{emergencyContact.phoneNumber.invalid}",
             groups = {OnCreate.class, OnUpdate.class})
    String phoneNumber,

    @Schema(description = "Relationship of the contact person to the employee. Examples: spouse, parent, sibling, friend, etc. " +
            "Minimum 2 characters, maximum 50 characters.",
            example = "Spouse",
            minLength = 2,
            maxLength = 50,
            nullable = true)
    @Size(max = 50, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String relationship
) {}

