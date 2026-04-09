package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Data Transfer Object for creating or updating a service assignment. " +
        "Represents the binding of a specific service from a supplier to a building or vehicle.")
public record ServiceAssignmentDTO(

        @Schema(description = "Subject type: BUILDING or VEHICLE.",
                example = "BUILDING",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{serviceAssignment.subjectType.required}", groups = OnCreate.class)
        SubjectType subjectType,

        @Schema(description = "ID of the service supplier.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{serviceAssignment.serviceSupplierId.required}", groups = OnCreate.class)
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long serviceSupplierId,

        @Schema(description = "ID of the building (required when subjectType = BUILDING).",
                example = "12")
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long buildingId,

        @Schema(description = "ID of the vehicle (required when subjectType = VEHICLE).",
                example = "7")
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long vehicleId,

        @Schema(description = "Type of service assigned.",
                example = "LUZ",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{serviceAssignment.serviceType.required}", groups = OnCreate.class)
        ServiceType serviceType,

        @Schema(description = "Account number for the service.",
                example = "1234567890")
        @Size(max = 100, message = "{serviceAssignment.accountNumber.size}", groups = {OnCreate.class, OnUpdate.class})
        String accountNumber,

        @Schema(description = "Estimated day of the month when the service is due (1-31).",
                example = "15")
        @Min(value = 1, message = "{serviceAssignment.estimatedDueDay.min}", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 31, message = "{serviceAssignment.estimatedDueDay.max}", groups = {OnCreate.class, OnUpdate.class})
        Integer estimatedDueDay,

        @Schema(description = "Name of the person or entity responsible for the service payment.",
                example = "Juan Perez S.A.")
        @Size(max = 200, message = "{serviceAssignment.accountHolder.size}", groups = {OnCreate.class, OnUpdate.class})
        String accountHolder,

        @Schema(description = "ID of the building from where the payment is issued.",
                example = "3")
        @Positive(message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
        Long paymentLocationId
) {}
