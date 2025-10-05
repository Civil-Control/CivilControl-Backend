package PSG.backEnd.model.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LicencePlatePaymentResponseDTO(
        Long id,
        LocalDate date,
        Long vehicleId,
        String vehicleLicensePlate,
        BigDecimal amount,
        Integer year,
        Integer period,
        String jurisdictionType
) {}

