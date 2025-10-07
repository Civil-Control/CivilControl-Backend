package PSG.backEnd.model.dto.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LicencePlatePaymentFilterDTO(
        LocalDate dateFrom,
        LocalDate dateTo,
        Long vehicleId,
        String vehicleLicensePlate,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer year,
        Integer period,
        String jurisdictionType
) {}

