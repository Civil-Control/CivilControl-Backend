package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filter DTO for searching service assignments by various criteria.")
public record ServiceAssignmentFilterDTO(

        @Schema(description = "Filter by subject type (BUILDING or VEHICLE).")
        SubjectType subjectType,

        @Schema(description = "Filter by service supplier ID.")
        Long serviceSupplierId,

        @Schema(description = "Filter by building ID.")
        Long buildingId,

        @Schema(description = "Filter by vehicle ID.")
        Long vehicleId,

        @Schema(description = "Filter by service type.")
        ServiceType serviceType,

        @Schema(description = "Generic search across supplier name, building name, vehicle plate, account holder (partial match).")
        String search
) {}
