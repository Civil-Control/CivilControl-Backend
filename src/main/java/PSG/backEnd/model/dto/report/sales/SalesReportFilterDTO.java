package PSG.backEnd.model.dto.report.sales;

import PSG.backEnd.model.enums.IvaCondition;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filter criteria for the sales report")
public record SalesReportFilterDTO(
        LocalDate startDate,
        LocalDate endDate,
        List<Long> projectAreaIds,
        List<Long> clientIds,
        SalesDocumentType documentType,
        IvaCondition ivaCondition,
        Boolean paid,
        Long workContractId,
        Boolean onlyLinkedToCertifications,
        Boolean includeCertificationsOnly,
        CertificationStatus certificationStatus,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {}
