package PSG.backEnd.model.dto.insurance;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Active fleet vehicle that has no insurance coverage in the renewal period.")
public record RenewalFleetVehicleDTO(
        Long vehicleId,
        String licensePlate,
        String brand,
        String model,
        Integer year
) {}
