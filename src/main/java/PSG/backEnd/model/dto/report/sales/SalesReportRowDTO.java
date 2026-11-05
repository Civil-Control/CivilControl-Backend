package PSG.backEnd.model.dto.report.sales;

import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Single row in the sales report detail layer (INVOICE or orphan CERTIFICATION_ONLY)")
public record SalesReportRowDTO(
        SalesReportRowKind kind,
        LocalDate date,
        BigDecimal amount,
        Boolean paid,

        // SalesDocument-specific (null when kind = CERTIFICATION_ONLY)
        Long salesDocumentId,
        SalesDocumentType documentType,
        String branchCode,
        String documentNumber,
        BigDecimal netTotal,
        BigDecimal ivaTotal,
        BigDecimal ivaExemptTotal,
        BigDecimal otherTaxes,
        String purchaseOrderReference,
        Long projectAreaTaskId,
        String projectAreaTaskName,
        List<SalesReportCertificationLinkDTO> linkedCertifications,

        // Orphan Certification-specific (null when kind = INVOICE)
        Long certificationId,
        Integer certificationNumber,
        CertificationStatus certificationStatus,
        Long contractId,
        String contractNumber,
        String contractDescription
) {}
