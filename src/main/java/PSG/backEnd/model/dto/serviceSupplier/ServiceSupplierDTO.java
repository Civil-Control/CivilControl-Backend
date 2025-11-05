package PSG.backEnd.model.dto.serviceSupplier;

import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating a service supplier. " +
        "Represents a supplier that provides utility services such as electricity, water, gas, internet, etc.")
public record ServiceSupplierDTO(

        @Schema(description = "ID of the supplier that provides the services.",
                example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Supplier id is required.", groups = OnCreate.class)
        @Positive(message = "Supplier id must be positive.", groups = {OnCreate.class, OnUpdate.class})
        Long supplierId,

        @Schema(description = "List of service types provided by this supplier. At least one service type is required.",
                example = "[\"LUZ\", \"AGUA\", \"GAS\"]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "At least one service type is required.", groups = OnCreate.class)
        List<@NotNull(message = "Service type cannot be null.", groups = {OnCreate.class, OnUpdate.class}) ServiceType> providedServices
) {}

