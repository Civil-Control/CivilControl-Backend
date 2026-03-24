package PSG.backEnd.model.dto.contracts;

import PSG.backEnd.model.enums.contracts.CertificationStatus;

import java.time.LocalDate;

public record CertificationFilterDTO(
    Long workContractId,
    Long clientId,
    CertificationStatus status,
    LocalDate dateFrom,
    LocalDate dateTo,
    Boolean hasInvoice,
    Long salesDocumentId
) {}
