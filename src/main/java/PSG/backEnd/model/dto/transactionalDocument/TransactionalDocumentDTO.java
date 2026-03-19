package PSG.backEnd.model.dto.transactionalDocument;

import PSG.backEnd.model.dto.item.ItemDetailDTO;
import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating transactional documents. " +
        "Represents commercial documents such as invoices, credit notes, debit notes issued by suppliers. " +
        "These documents record purchases and expenses, including itemized details, taxes, and discounts.")
public record TransactionalDocumentDTO(

    @Schema(description = "Date when the document was issued. Required for creating documents.",
            example = "2024-10-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate date,

    @Schema(description = "Type of commercial document. Valid values: BILL_A (Factura A), " +
            "BILL_B (Factura B), BILL_C (Factura C), CREDIT_NOTE_A (Nota de crédito A), " +
            "CREDIT_NOTE_B (Nota de crédito B), CREDIT_NOTE_C (Nota de crédito C), " +
            "DEBIT_NOTE_A (Nota de débito A), DEBIT_NOTE_B (Nota de débito B), DEBIT_NOTE_C (Nota de débito C), " +
            "OTHER_DOCUMENT (Otros documentos). " +
            "Types A, B, C correspond to Argentine AFIP document types.",
            example = "BILL_B",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @Valid
    DocumentType documentType,

    @Schema(description = "Branch or point of sale code that issued the document. " +
            "Must be exactly 5 digits following AFIP standards.",
            example = "00001",
            pattern = "\\d{5}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.required}", groups = OnCreate.class)
    @Pattern(regexp = "\\d{5}", message = "{document.branchCode.size}", groups = {OnCreate.class, OnUpdate.class})
    String branchCode,

    @Schema(description = "Sequential document number within the branch. " +
            "Must be exactly 8 digits following AFIP standards.",
            example = "00012345",
            pattern = "\\d{8}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.required}", groups = OnCreate.class)
    @Pattern(regexp = "\\d{8}", message = "{document.documentNumber.size}", groups = {OnCreate.class, OnUpdate.class})
    String documentNumber,

    @Schema(description = "ID of the supplier that issued this document.",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    Long supplierId,

    @Schema(description = "ID of the project area to which this expense is assigned. " +
            "Optional field for expense tracking and budgeting purposes.",
            example = "7",
            nullable = true)
    Long projectAreaId,

    @Schema(description = "Total amount of other taxes (excluding IVA/VAT). Must be zero or positive. " +
            "Can include municipal taxes, provincial taxes, or other applicable charges.",
            example = "125.50",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @DecimalMin(value = "0.00", inclusive = true, message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 12, fraction = 2, message = "{validation.digits}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal otherTaxes,

    @Schema(description = "Net total amount (subtotal before taxes). Must be zero or positive.",
            example = "10000.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @DecimalMin(value = "0.00", message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal netTotal,

    @Schema(description = "Total IVA (VAT) amount applied to taxable items. Must be zero or positive.",
            example = "2100.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @DecimalMin(value = "0.00", message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal ivaTotal,

    @Schema(description = "Total amount exempt from IVA (VAT). Must be zero or positive. " +
            "Applies to items that are not subject to VAT according to tax regulations.",
            example = "500.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @DecimalMin(value = "0.00", message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal ivaExemptTotal,

    @Schema(description = "Final total amount of the document including all taxes and discounts. Must be zero or positive.",
            example = "12725.50",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @DecimalMin(value = "0.00", message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal total,

    @Schema(description = "Discount percentage applied to the document. Must be between 0 and 100.",
            example = "5.50",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @DecimalMin(value = "0.00", inclusive = true, message = "{validation.positiveOrZero}", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "100.00", inclusive = true, message = "{validation.max}", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 3, fraction = 2, message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal discountPercentage,

    @Schema(description = "Additional notes or comments about the document. Maximum 500 characters. Optional field.",
            example = "Pago en 3 cuotas. Entrega coordinada para el 20/10.",
            maxLength = 500,
            nullable = true)
    @Size(max = 500, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String comment,

    @Schema(description = "Soft deletion flag. When true, the document is marked as deleted but remains in database.",
            example = "false")
    boolean deleted,

    @Schema(description = "List of items included in the document. At least one item is required. " +
            "Each item includes description, quantity, price, and tax details.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(groups = OnCreate.class, message = "{validation.notNull}")
    List<ItemDetailDTO> items
) {}
