package PSG.backEnd.model.dto.treasury;

import PSG.backEnd.model.enums.treasury.CheckType;

public record CheckbookResponseDTO(
        Long id,
        String name,
        String checkbookNumber,
        Long bankAccountId,
        String bankAccountName,
        String bankName,
        CheckType checkType,
        Long rangeFrom,
        Long rangeTo,
        int totalChecks,
        int usedChecks,
        int availableChecks,
        Long nextAvailableNumber,
        Boolean active
) {}
