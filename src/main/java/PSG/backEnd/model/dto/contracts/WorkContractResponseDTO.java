package PSG.backEnd.model.dto.contracts;

import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.contracts.WorkContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Full work contract data returned from the API.")
public record WorkContractResponseDTO(
    Long id,
    String contractNumber,
    Long clientId,
    String clientBusinessName,
    String clientCuit,
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    String description,
    LocalDate contractDate,
    LocalDate endDate,
    BigDecimal contractedAmount,
    Currency currency,
    WorkContractStatus status,
    String comment,
    Boolean deleted
) {}
