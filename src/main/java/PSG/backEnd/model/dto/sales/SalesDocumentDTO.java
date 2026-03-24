package PSG.backEnd.model.dto.sales;

import PSG.backEnd.model.enums.documents.SalesDocumentType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating a sales document.")
public record SalesDocumentDTO(

    @Schema(description = "Type of sales document.", example = "FACTURA_A")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    SalesDocumentType documentType,

    @Schema(description = "Branch/point of sale code (4 digits).", example = "0001")
    @Size(max = 5, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String branchCode,

    @Schema(description = "Document sequence number (8 digits).", example = "00000001")
    @Size(max = 8, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String documentNumber,

    @Schema(description = "Date of the document.", example = "2024-01-15")
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    LocalDate date,

    @Schema(description = "ID of the client associated with this document.")
    Long clientId,

    @Schema(description = "External purchase order reference.", example = "OC-2024-001")
    @Size(max = 100, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String purchaseOrderReference,

    @Schema(description = "Net taxable amount before IVA.", example = "1000.00")
    BigDecimal netTotal,

    @Schema(description = "Total IVA amount.", example = "210.00")
    BigDecimal ivaTotal,

    @Schema(description = "Total IVA-exempt amount.", example = "0.00")
    BigDecimal ivaExemptTotal,

    @Schema(description = "Other taxes amount.", example = "0.00")
    BigDecimal otherTaxes,

    @Schema(description = "Grand total of the document.", example = "1210.00")
    BigDecimal total,

    @Schema(description = "Discount percentage applied.", example = "0.00")
    @DecimalMin(value = "0.0", inclusive = true, message = "{validation.decimalMin}", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "100.0", inclusive = true, message = "{validation.decimalMax}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal discountPercentage,

    @Schema(description = "Project area ID associated with this document.")
    Long projectAreaId,

    @Schema(description = "Line items included in this document.")
    @Valid
    List<SalesItemDetailDTO> items,

    @Schema(description = "Whether this document has been collected/paid.", example = "false")
    Boolean paid,

    @Schema(description = "Additional comments or notes.", example = "Pago a 30 días.")
    @Size(max = 500, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String comment,

    @Schema(description = "Whether this document is soft-deleted.", example = "false")
    Boolean deleted
) {}
