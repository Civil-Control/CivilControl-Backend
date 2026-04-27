package PSG.backEnd.model.dto.transactionalDocument;

import PSG.backEnd.model.dto.item.ItemDetailResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record TransactionalDocumentResponseDTO(

    Long id,
    String date,
    Long supplierId,
    String supplierName,
    String documentType,
    String branchCode,
    String documentNumber,
    List<ItemDetailResponseDTO> items,
    BigDecimal otherTaxes,
    BigDecimal netTotal,
    BigDecimal ivaTotal,
    BigDecimal ivaExemptTotal,
    BigDecimal total,
    BigDecimal discountPercentage,
    Long projectAreaId,
    String projectAreaName,
    String projectAreaColor,
    Long projectAreaTaskId,
    String projectAreaTaskName,
    String comment,
    boolean paid,
    boolean deleted,

    /**
     * Derived business status. One of:
     * PAID, PENDING, PARTIALLY_CREDITED, CREDITED (for invoices / debit notes);
     * APPLIED, UNAPPLIED                          (for credit notes);
     * NEUTRAL                                     (for OTHER_DOCUMENT).
     */
    String status,

    /** Sum of credit-note amounts applied against this invoice/debit-note (0 for credit notes / others). */
    BigDecimal creditApplied,

    /** Outstanding amount = total - creditApplied for unpaid invoices/debit notes; 0 otherwise. */
    BigDecimal pendingAmount,

    /** Populated when this document IS a credit note: invoices/debit-notes it credits. */
    List<CreditNoteApplicationResponseDTO> creditApplications,

    /** Populated when this document IS an invoice/debit-note: the credit notes that have been applied to it. */
    List<CreditNoteApplicationResponseDTO> appliedCredits,

    /** True when a credit note was manually marked as applied (no invoice/debit-note links required). False otherwise. */
    boolean manuallyApplied
) {}
