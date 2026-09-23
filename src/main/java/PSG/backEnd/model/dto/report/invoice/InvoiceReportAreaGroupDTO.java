package PSG.backEnd.model.dto.report.invoice;

import PSG.backEnd.model.enums.documents.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
@Schema(description = "Group of documents for a project area (sector)")
public record InvoiceReportAreaGroupDTO(
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    BigDecimal subtotalAmount,
    BigDecimal subtotalNet,
    BigDecimal subtotalIva,
    BigDecimal subtotalIvaExempt,
    BigDecimal subtotalOtherTaxes,
    BigDecimal subtotalIibbPerception,
    int documentCount,
    Map<DocumentType, BigDecimal> subtotalsByDocumentType,
    List<InvoiceReportSupplierGroupDTO> supplierGroups,

    /**
     * Value Recovery (hidden Feature 18): total recovered for this area's documents, from the
     * recovery_events ledger. Non-null only when this area is the tenant's recovery sector.
     */
    BigDecimal recoveryTotalRecovered,

    /** {@code subtotalAmount - recoveryTotalRecovered} — the real cash outflow. Same nullability. */
    BigDecimal recoveryNetOutflow
) {}
