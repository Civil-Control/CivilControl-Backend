package PSG.backEnd.model.dto.serviceSupplier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response DTO containing complete information about a service supplier, " +
        "including supplier details and the list of services provided.")
public record ServiceSupplierResponseDTO(

        @Schema(description = "Unique identifier of the service supplier.",
                example = "5")
        Long id,

        @Schema(description = "ID of the supplier that provides the services.",
                example = "10")
        Long supplierId,

        @Schema(description = "Name of the supplier that provides the services.",
                example = "Municipal Services Company")
        String supplierName,

        @Schema(description = "List of service types provided by this supplier.",
                example = "[\"LUZ\", \"AGUA\", \"GAS\"]")
        List<String> providedServices
) {}

