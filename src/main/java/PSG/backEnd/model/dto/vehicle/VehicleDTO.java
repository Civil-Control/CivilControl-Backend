package PSG.backEnd.model.dto.vehicle;

import PSG.backEnd.model.enums.vehicle.JurisdictionType;
import PSG.backEnd.model.enums.vehicle.VehicleType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record VehicleDTO(

        @NotBlank(message = "License plate is required.", groups = OnCreate.class)
        @Size(min = 6, max = 10, message = "License plate length must be between 6 and 10 characters.", groups = {OnCreate.class, OnUpdate.class})
        @Pattern(
                regexp = "^([A-Z]{2}\\s?\\d{3}\\s?[A-Z]{2}|[A-Z]{3}\\s?\\d{3})$",
                message = "License plate format must be either 'AA 123 BB' or 'AAA 123'. Use uppercase letters (A–Z) and digits only."
                , groups = {OnCreate.class, OnUpdate.class})
        String licensePlate,

        @Size(max = 60, message = "Brand must be at most 60 characters.", groups = {OnCreate.class, OnUpdate.class})
        String brand,

        @Size(max = 60, message = "Model must be at most 60 characters.", groups = {OnCreate.class, OnUpdate.class})
        String model,

        @Min(value = 1950, message = "Year must be greater than or equal to 1950.", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 2100, message = "Year must be less than or equal to 2100.", groups = {OnCreate.class, OnUpdate.class})
        Integer year,

        @Size(max = 40, message = "Color must be at most 40 characters.", groups = {OnCreate.class, OnUpdate.class})
        String color,

        @Size(max = 40, message = "Nickname must be at most 40 characters.", groups = {OnCreate.class, OnUpdate.class})
        String nickName,

        VehicleType vehicleType,

        Long projectAreaId,

        @Size(max = 60, message = "Storage location must be at most 60 characters.", groups = {OnCreate.class, OnUpdate.class})
        String storedIn,

        @FutureOrPresent(message = "VTV expiration date must be today or in the future.", groups = {OnCreate.class, OnUpdate.class})
        LocalDate vtvExpirationDate,

        JurisdictionType jurisdictionType
) {}