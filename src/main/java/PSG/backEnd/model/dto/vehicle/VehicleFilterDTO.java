package PSG.backEnd.model.dto.vehicle;

import java.time.LocalDate;

public record VehicleFilterDTO(
        String licensePlate,
        String brand,
        String model,
        Integer year,
        String color,
        String nickName,
        String vehicleType,
        String projectAreaName,
        String storedIn,
        LocalDate vtvExpirationDate,
        String jurisdictionType
) {}