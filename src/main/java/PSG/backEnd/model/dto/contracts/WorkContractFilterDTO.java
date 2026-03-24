package PSG.backEnd.model.dto.contracts;

import PSG.backEnd.model.enums.contracts.Currency;
import PSG.backEnd.model.enums.contracts.WorkContractStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkContractFilterDTO(
    Long clientId,
    String clientBusinessName,
    String contractNumber,
    Long projectAreaId,
    WorkContractStatus status,
    Currency currency,
    LocalDate contractDateFrom,
    LocalDate contractDateTo,
    BigDecimal minContractedAmount,
    BigDecimal maxContractedAmount,
    String search
) {}
