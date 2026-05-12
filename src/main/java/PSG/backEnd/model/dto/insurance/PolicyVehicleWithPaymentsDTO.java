package PSG.backEnd.model.dto.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PolicyVehicleWithPaymentsDTO(
        Long id,
        Long vehicleId,
        String vehicleLicensePlate,
        String vehicleBrand,
        String vehicleModel,
        Integer vehicleYear,
        Long autoPolicyId,
        Long insurancePolicyId,
        String policyNumber,
        BigDecimal sumInsured,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LocalDate cancellationDate,
        Integer numberOfInstallments,
        BigDecimal premioTotal,
        BigDecimal premioMensual,
        List<InsurancePolicyPaymentResponseDTO> payments
) {}
