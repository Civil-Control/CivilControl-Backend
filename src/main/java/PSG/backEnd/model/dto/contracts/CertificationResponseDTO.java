package PSG.backEnd.model.dto.contracts;

import PSG.backEnd.model.enums.contracts.CertificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Full certification data returned from the API.")
public record CertificationResponseDTO(
    Long id,
    Integer certificationNumber,
    Long workContractId,
    String contractNumber,
    LocalDate certificationDate,
    BigDecimal certifiedAmount,
    Long salesDocumentId,
    String salesDocumentLabel,
    CertificationStatus status,
    String comment,
    Boolean deleted
) {}
