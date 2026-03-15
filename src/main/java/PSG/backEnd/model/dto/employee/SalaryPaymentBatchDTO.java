package PSG.backEnd.model.dto.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SalaryPaymentBatchDTO(
    @NotNull
    @Size(min = 1, max = 100)
    @Valid
    List<SalaryPaymentDTO> payments
) {}
