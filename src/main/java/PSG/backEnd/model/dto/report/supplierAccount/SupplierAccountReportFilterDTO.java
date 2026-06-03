package PSG.backEnd.model.dto.report.supplierAccount;

import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.report.SupplierAccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Filter criteria for the supplier current-account report. " +
        "startDate and endDate are mandatory; all other filters are optional.")
public record SupplierAccountReportFilterDTO(
        LocalDate startDate,
        LocalDate endDate,
        List<Long> supplierIds,
        List<Long> projectAreaIds,
        SupplierAccountStatus statusFilter,
        DocumentType documentType,
        PaymentMethod paymentMethod,
        BigDecimal minFinalBalance,
        BigDecimal maxFinalBalance,
        Boolean onlyWithMovementsInPeriod,
        Boolean includeUnassigned
) {}
