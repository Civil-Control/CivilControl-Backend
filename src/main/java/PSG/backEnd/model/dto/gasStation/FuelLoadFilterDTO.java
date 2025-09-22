package PSG.backEnd.model.dto.gasStation;

import java.time.LocalDate;

public record FuelLoadFilterDTO(
        LocalDate dateFrom,
        LocalDate dateTo,
        String branchCode,
        String ticketNumber,
        String fuelType,
        Long vehicleId,
        String vehicleLicensePlate,
        Long projectAreaId,
        String projectAreaName,
        Long gasStationId
) {}
