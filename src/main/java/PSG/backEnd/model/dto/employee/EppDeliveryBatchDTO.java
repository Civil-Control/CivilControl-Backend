package PSG.backEnd.model.dto.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EppDeliveryBatchDTO(
    @NotNull
    @Size(min = 1, max = 100)
    @Valid
    List<EppDeliveryDTO> deliveries
) {}
