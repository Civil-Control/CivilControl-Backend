package PSG.backEnd.model.dto.insurance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Vehicle carried over from the policy being renewed. Amounts are the previous period's values, shown for reference only.")
public record RenewalDraftVehicleDTO(
        Long vehicleId,
        String licensePlate,
        String brand,
        String model,
        Integer year,
        BigDecimal previousSumInsured,
        BigDecimal previousPremioTotal,
        BigDecimal previousPremioMensual,
        Integer numberOfInstallments,
        @Schema(description = "Number of another active policy that already covers this vehicle in the new period, if any.")
        String otherActivePolicyNumber
) {}
