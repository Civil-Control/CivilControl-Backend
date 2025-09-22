package PSG.backEnd.model.dto.gasStation;

public record FuelLoadResponseDTO(
        Long id,
        String date,
        String branchCode,
        String ticketNumber,
        String fuelType,
        Double liters,
        Double pricePerLiter,
        Double totalAmount,
        Long vehicleId,
        String vehicleLicensePlate,
        Long projectAreaId,
        String projectAreaName,
        Long gasStationId,
        String gasStationName
) {}