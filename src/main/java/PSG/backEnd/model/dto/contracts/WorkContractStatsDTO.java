package PSG.backEnd.model.dto.contracts;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Financial tracking metrics for a work contract.")
public record WorkContractStatsDTO(
    BigDecimal contractedAmount,

    // Certification
    BigDecimal totalCertified,
    BigDecimal pendingToCertify,
    BigDecimal certificationProgress,

    // Invoicing
    BigDecimal totalInvoiced,
    BigDecimal pendingToInvoice,
    BigDecimal invoicingProgress,

    // Collection
    BigDecimal totalCollected,
    BigDecimal pendingToCollect,
    BigDecimal collectionProgress
) {}
