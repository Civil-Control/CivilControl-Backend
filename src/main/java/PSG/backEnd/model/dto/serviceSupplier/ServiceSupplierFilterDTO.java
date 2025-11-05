package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Filter DTO for searching and filtering service suppliers by various criteria.")
public record ServiceSupplierFilterDTO(

        @Schema(description = "Filter by supplier name (legal name or trade name).",
                example = "Municipal Services")
        String supplierName,

        @Schema(description = "Filter by service type provided.",
                example = "LUZ")
        ServiceType serviceType,

        @Schema(description = "Filter by supplier CUIT.",
                example = "20-12345678-9")
        String cuit
) {}

