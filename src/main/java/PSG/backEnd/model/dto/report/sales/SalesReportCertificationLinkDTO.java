package PSG.backEnd.model.dto.report.sales;

import PSG.backEnd.model.enums.contracts.CertificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "A certification linked to a SalesDocument, exposed as metadata of an INVOICE row")
public record SalesReportCertificationLinkDTO(
        Long certificationId,
        Integer certificationNumber,
        BigDecimal certifiedAmount,
        CertificationStatus status,
        Long contractId,
        String contractNumber,
        String contractDescription
) {}
