package PSG.backEnd.model.dto.gasStation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FuelLoadBatchDTO(
    @NotNull(message = "La lista de cargas de combustible no puede ser nula")
    @Size(min = 1, max = 100, message = "Debe proporcionar entre 1 y 100 cargas de combustible")
    @Valid
    List<FuelLoadDTO> fuelLoads
) {
}
