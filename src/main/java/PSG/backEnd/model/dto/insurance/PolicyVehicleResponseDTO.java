package PSG.backEnd.model.dto.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyVehicleResponseDTO(
        Long id,
        Long vehicleId,
        String vehicleLicensePlate,
        String vehicleBrand,
        String vehicleModel,
        Integer vehicleYear,
        Long autoPolicyId,
        BigDecimal sumInsured,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LocalDate cancellationDate,
        Integer numberOfInstallments,
        BigDecimal premioTotal,
        BigDecimal premioMensual
) {}
