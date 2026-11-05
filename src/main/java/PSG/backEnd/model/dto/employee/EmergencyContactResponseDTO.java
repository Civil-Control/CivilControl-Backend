package PSG.backEnd.model.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response Data Transfer Object for emergency contact. " +
        "Contains the emergency contact information associated with an employee.")
public record EmergencyContactResponseDTO(
    @Schema(description = "Full name of the emergency contact person.",
            example = "María González")
    String name,

    @Schema(description = "Phone number of the emergency contact.",
            example = "+54 9 11 9876-5432")
    String phoneNumber,

    @Schema(description = "Relationship of the contact person to the employee.",
            example = "Spouse")
    String relationship
) {}

