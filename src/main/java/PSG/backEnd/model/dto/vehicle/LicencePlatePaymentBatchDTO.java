package PSG.backEnd.model.dto.vehicle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LicencePlatePaymentBatchDTO(
        @NotNull(message = "La lista de pagos de patente no puede ser nula")
        @Size(min = 1, max = 100, message = "Debe proporcionar entre 1 y 100 pagos de patente")
        @Valid
        List<LicencePlatePaymentDTO> payments
) {
}
