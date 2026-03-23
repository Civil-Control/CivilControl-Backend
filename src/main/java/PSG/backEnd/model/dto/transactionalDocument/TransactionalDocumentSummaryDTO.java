package PSG.backEnd.model.dto.transactionalDocument;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight summary of a TransactionalDocument, included in response DTOs
 * of linked records (FuelLoad, Repair, SalaryPayment, Stock).
 */
public record TransactionalDocumentSummaryDTO(
        Long id,
        String documentType,
        String branchCode,
        String documentNumber,
        String supplierName,
        BigDecimal total,
        LocalDate date
) {}
