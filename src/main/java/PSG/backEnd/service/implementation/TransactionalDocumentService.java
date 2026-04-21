package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentAlreadyActiveException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.model.dto.item.ItemDetailDTO;
import PSG.backEnd.model.dto.transactionalDocument.LinkedRecordItemDTO;
import PSG.backEnd.model.dto.transactionalDocument.LinkedRecordsSummaryDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentFilterDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.*;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.mapper.ItemDetailMapper;
import PSG.backEnd.model.mapper.TransactionalDocumentMapper;
import PSG.backEnd.repository.FuelLoadRepository;
import PSG.backEnd.repository.ItemDetailRepository;
import PSG.backEnd.repository.ItemRepository;
import PSG.backEnd.repository.RepairItemRepository;
import PSG.backEnd.repository.SalaryPaymentRepository;
import PSG.backEnd.repository.StockPurchaseRepository;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.port.IProjectAreaService;
import PSG.backEnd.service.port.IProjectAreaTaskService;
import PSG.backEnd.service.port.IStockPurchaseService;
import PSG.backEnd.service.port.ISupplierService;
import PSG.backEnd.service.port.ITransactionalDocumentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class TransactionalDocumentService implements ITransactionalDocumentService {

    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final TransactionalDocumentMapper transactionalDocumentMapper;
    private final ItemRepository itemRepository;
    private final ItemDetailRepository itemDetailRepository;
    private final ItemDetailMapper itemDetailMapper;

    private final IProjectAreaService iProjectAreaService;
    private final IProjectAreaTaskService iProjectAreaTaskService;
    private final ISupplierService iSupplierService;
    private final MessageSourceHelper messageSourceHelper;

    private final RepairItemRepository repairItemRepository;
    private final FuelLoadRepository fuelLoadRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final StockPurchaseRepository stockPurchaseRepository;
    private final IStockPurchaseService iStockPurchaseService;
    private final DocumentTotalRecalculator documentTotalRecalculator;

    @Override
    @Transactional
    public TransactionalDocumentResponseDTO createTransactionalDocument(TransactionalDocumentDTO dto) {
        // Validate the DTO and check for existing documents
        validateNewDocument(dto);

        // Check if an active document with the same branch code and document number already exists
        if (transactionalDocumentRepository
                .existsByBranchCodeAndDocumentNumberAndSupplierIdAndDeletedFalse(
                        dto.branchCode(),
                        dto.documentNumber(),
                        dto.supplierId())) {
            throw new TransactionalDocumentAlreadyActiveException(
                    "There is already an active transactional document with number "
                            + dto.branchCode() + "-" + dto.documentNumber()
                            + " for supplierId " + dto.supplierId());
        }

        // Check if a deleted document with the same branch code and document number exists
        Optional<TransactionalDocument> deletedDocument = transactionalDocumentRepository
                .findByBranchCodeAndDocumentNumberAndDeletedTrue(dto.branchCode(), dto.documentNumber());

        // If a deleted document exists, reactivate it; otherwise, create a new one
        TransactionalDocument document;
        document = deletedDocument
                .map(transactionalDocument -> reactivateExistingDocument(transactionalDocument, dto)).orElseGet(() -> createNewDocument(dto));

        document = transactionalDocumentRepository.save(document);

        // Recompute totals from authoritative sources after create.
        documentTotalRecalculator.recalculateDocumentTotals(document.getId());
        TransactionalDocument refreshed = transactionalDocumentRepository.findByIdAndDeletedFalse(document.getId())
                .orElse(document);
        return transactionalDocumentMapper.toResponseDto(refreshed);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionalDocumentResponseDTO> getAllTransactionalDocuments(TransactionalDocumentFilterDTO filterDTO, Pageable pageable) {
        DocumentType docType = null;
        if (filterDTO.documentType() != null && !filterDTO.documentType().isBlank()) {
            docType = DocumentType.valueOf(filterDTO.documentType());
        }
        return transactionalDocumentRepository.findAllWithFilters(
                filterDTO.documentNumber(),
                docType,
                filterDTO.supplierCuit(),
                filterDTO.supplierName(),
                filterDTO.projectAreaId(),
                filterDTO.projectAreaName(),
                filterDTO.maxTotalAmount(),
                filterDTO.minTotalAmount(),
                filterDTO.totalAmount(),
                filterDTO.fromDate(),
                filterDTO.toDate(),
                filterDTO.paid(),
                filterDTO.search(),
                pageable
        ).map(transactionalDocumentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionalDocumentResponseDTO getTransactionalDocumentById(Long id) {
        return transactionalDocumentRepository.findByIdAndDeletedFalse(id)
                .map(transactionalDocumentMapper::toResponseDto)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionalDocument getEntityById(Long id) {
        return transactionalDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(id));
    }

    @Override
    @Transactional
    public TransactionalDocumentResponseDTO updateTransactionalDocument(Long id, TransactionalDocumentDTO dto) {
        TransactionalDocument existingDocument = getEntityById(id);

        // Capture the document's current impact on the supplier's pending balance BEFORE any change.
        // This handles invoices, debit notes (positive impact when unpaid) and credit notes (always negative).
        Supplier oldSupplier = existingDocument.getSupplier();
        BigDecimal oldImpact = currentBalanceImpact(existingDocument, oldSupplier);

        // Update the document with new values (including items, supplier, etc.)
        updateDocumentFromDTO(existingDocument, dto);

        // Credit notes are always considered "applied" (paid=true). Force it after partialUpdate
        // in case the DTO sent paid=false.
        if (isCreditNote(existingDocument.getDocumentType())) {
            existingDocument.setPaid(true);
        }

        Supplier newSupplier = existingDocument.getSupplier();
        BigDecimal newImpact = currentBalanceImpact(existingDocument, newSupplier);

        if (oldSupplier != null && newSupplier != null && oldSupplier.getId().equals(newSupplier.getId())) {
            // Same supplier: apply only the delta
            BigDecimal delta = newImpact.subtract(oldImpact);
            if (delta.signum() != 0) {
                applyToBalance(newSupplier, delta);
            }
        } else {
            // Supplier changed: revert old impact on old supplier, apply full new impact on new supplier
            if (oldSupplier != null && oldImpact.signum() != 0) {
                applyToBalance(oldSupplier, oldImpact.negate());
            }
            if (newSupplier != null && newImpact.signum() != 0) {
                applyToBalance(newSupplier, newImpact);
            }
        }

        TransactionalDocument savedDocument = transactionalDocumentRepository.save(existingDocument);

        // Recompute totals from authoritative sources (items + linked records, per-record IVA).
        documentTotalRecalculator.recalculateDocumentTotals(savedDocument.getId());
        TransactionalDocument refreshed = transactionalDocumentRepository.findByIdAndDeletedFalse(savedDocument.getId())
                .orElse(savedDocument);
        return transactionalDocumentMapper.toResponseDto(refreshed);
    }

    /**
     * Updates a TransactionalDocument from DTO, handling ItemDetails correctly
     */
    private void updateDocumentFromDTO(TransactionalDocument document, TransactionalDocumentDTO dto) {
        // Update basic fields using mapper (supplier and projectArea are ignored)
        transactionalDocumentMapper.partialUpdate(dto, document);

        // Resolve supplier if changed
        if (dto.supplierId() != null) {
            Supplier supplier = iSupplierService.getEntityById(dto.supplierId());
            document.setSupplier(supplier);
        }

        // Resolve projectArea (null = remove assignment)
        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = iProjectAreaService.getEntityById(dto.projectAreaId());
            document.setProjectArea(projectArea);
        } else {
            document.setProjectArea(null);
        }

        // Resolve projectAreaTask (null = remove assignment)
        if (dto.projectAreaTaskId() != null) {
            document.setProjectAreaTask(iProjectAreaTaskService.getEntityById(dto.projectAreaTaskId()));
        } else {
            document.setProjectAreaTask(null);
        }

        // Handle ItemDetails updates if provided
        if (dto.items() != null) {
            updateItemDetails(document, dto.items());
        }
    }

    /**
     * Updates ItemDetails intelligently to avoid unnecessary deletions and recreations
     */
    private void updateItemDetails(TransactionalDocument document, List<ItemDetailDTO> newItemDetailDTOs) {
        List<ItemDetail> existingItems = document.getItems();

        // Create a map of existing items by their ID for quick lookup
        Map<Long, ItemDetail> existingItemsMap = existingItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(ItemDetail::getId, item -> item));

        // Track which existing items should be kept
        Set<Long> itemsToKeep = new HashSet<>();

        // Process each item from the DTO
        for (ItemDetailDTO itemDetailDTO : newItemDetailDTOs) {
            if (itemDetailDTO.id() != null && existingItemsMap.containsKey(itemDetailDTO.id())) {
                // UPDATE: Item exists, update its fields
                ItemDetail existingItem = existingItemsMap.get(itemDetailDTO.id());
                updateExistingItemDetail(existingItem, itemDetailDTO);
                itemsToKeep.add(itemDetailDTO.id());
            } else {
                // CREATE: New item, add it to the document
                ItemDetail newItem = createNewItemDetail(itemDetailDTO);
                document.addItemDetail(newItem);
            }
        }

        // REMOVE: Remove items that are not in the new list
        existingItems.removeIf(item ->
            item.getId() != null && !itemsToKeep.contains(item.getId()));
    }

    /**
     * Updates an existing ItemDetail with new values from DTO
     */
    private void updateExistingItemDetail(ItemDetail existingItem, ItemDetailDTO dto) {
        // Update item reference if changed
        if (dto.itemId() != null && !dto.itemId().equals(existingItem.getItem().getId())) {
            Item newItem = itemRepository.findById(dto.itemId())
                    .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", dto.itemId())));
            existingItem.setItem(newItem);
        }

        // Update other fields
        if (dto.unitAmount() != null) {
            existingItem.setUnitAmount(dto.unitAmount());
        }
        if (dto.quantity() != null) {
            existingItem.setQuantity(dto.quantity());
        }
        if (dto.ivaPercentage() != null) {
            existingItem.setIvaPercentage(dto.ivaPercentage());
        }
        if (dto.documentSortOrder() != null) {
            existingItem.setDocumentSortOrder(dto.documentSortOrder());
        }

        // Handle total amount - use provided value or calculate it
        if (dto.totalAmount() != null) {
            // Use the provided total amount
            existingItem.setTotalAmount(dto.totalAmount());
        } else {
            // Calculate total amount using current values
            existingItem.setTotalAmount(computeTotal(
                existingItem.getUnitAmount(),
                existingItem.getQuantity(),
                existingItem.getIvaPercentage()
            ));
        }
    }

    /**
     * Creates a new ItemDetail from DTO
     */
    private ItemDetail createNewItemDetail(ItemDetailDTO itemDetailDTO) {
        // Load the complete Item from database
        Item item = itemRepository.findById(itemDetailDTO.itemId())
                .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", itemDetailDTO.itemId())));

        // Create ItemDetail manually to ensure Item reference is complete
        ItemDetail itemDetail = ItemDetail.builder()
                .item(item)  // Complete Item with all fields loaded
                .unitAmount(itemDetailDTO.unitAmount())
                .quantity(itemDetailDTO.quantity())
                .ivaPercentage(itemDetailDTO.ivaPercentage())
                .documentSortOrder(itemDetailDTO.documentSortOrder() != null ? itemDetailDTO.documentSortOrder() : 0)
                .build();

        // Handle total amount - use provided value or calculate it
        if (itemDetailDTO.totalAmount() != null) {
            // Use the provided total amount
            itemDetail.setTotalAmount(itemDetailDTO.totalAmount());
        } else {
            // Calculate total amount
            itemDetail.setTotalAmount(computeTotal(
                itemDetailDTO.unitAmount(),
                itemDetailDTO.quantity(),
                itemDetailDTO.ivaPercentage()
            ));
        }

        return itemDetail;
    }

    @Override
    @Transactional
    public void updateTransactionalDocumentStatus(Long documentId, Long supplierId, BigDecimal amount) {
        TransactionalDocument document = getEntityById(documentId);

        // If amount is zero, revert the status (mark as unpaid)
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            revertDocumentPaymentStatus(document, supplierId);
        } else {
            // Normal logic: mark as paid
            validateDocumentForPayment(document, supplierId, amount);
            document.setPaid(true);
        }

        transactionalDocumentRepository.save(document);
    }

    /**
     * Reverts a document to unpaid status only if it still exists (not soft-deleted).
     * Used when deleting a payment — if the associated document was already deleted,
     * there is nothing to revert and the operation is silently skipped.
     */
    @Override
    @Transactional
    public void revertTransactionalDocumentStatusIfExists(Long documentId, Long supplierId) {
        transactionalDocumentRepository.findByIdAndDeletedFalse(documentId).ifPresent(document -> {
            revertDocumentPaymentStatus(document, supplierId);
            transactionalDocumentRepository.save(document);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public LinkedRecordsSummaryDTO getLinkedRecordsSummary(Long id) {
        List<LinkedRecordItemDTO> repairs = repairItemRepository.findByTransactionalDocumentId(id)
                .stream()
                .map(ri -> new LinkedRecordItemDTO(ri.getRepair().getId(),
                        "Reparación – " + ri.getRepair().getVehicle().getLicensePlate()
                                + " (" + ri.getDescription() + ")"
                                + " – " + ri.getRepair().getDate()))
                .toList();

        List<LinkedRecordItemDTO> fuelLoads = fuelLoadRepository.findByTransactionalDocumentId(id)
                .stream()
                .map(fl -> new LinkedRecordItemDTO(fl.getId(),
                        fl.getVehicle().getLicensePlate()
                                + " – " + fl.getLiters().stripTrailingZeros().toPlainString()
                                + "L " + fl.getFuelType()
                                + " – " + fl.getDate()))
                .toList();

        List<LinkedRecordItemDTO> salaryPayments = salaryPaymentRepository.findByTransactionalDocumentId(id)
                .stream()
                .map(sp -> new LinkedRecordItemDTO(sp.getId(),
                        sp.getEmployee().getLastName() + ", " + sp.getEmployee().getName()
                                + " – " + sp.getPaymentDate()))
                .toList();

        List<LinkedRecordItemDTO> stockPurchases = stockPurchaseRepository.findByTransactionalDocumentId(id)
                .stream()
                .map(sp -> new LinkedRecordItemDTO(sp.getId(),
                        "Compra de Stock – " + sp.getQuantity().stripTrailingZeros().toPlainString() + " uds."))
                .toList();

        return new LinkedRecordsSummaryDTO(repairs, fuelLoads, salaryPayments, stockPurchases);
    }

    @Override
    @Transactional
    public void deleteTransactionalDocument(Long id, boolean deleteLinkedRecords) {
        TransactionalDocument document = getEntityById(id);

        // Revert any impact this document has on the supplier's pending balance.
        // Invoices/debit notes (when unpaid) added to the balance; credit notes subtracted from it.
        Supplier supplier = document.getSupplier();
        BigDecimal impact = currentBalanceImpact(document, supplier);
        if (supplier != null && impact.signum() != 0) {
            applyToBalance(supplier, impact.negate());
        }

        if (deleteLinkedRecords) {
            // Unlink RepairItems from this document (don't delete the repair itself)
            repairItemRepository.findByTransactionalDocumentId(id)
                    .forEach(ri -> { ri.setTransactionalDocument(null); repairItemRepository.save(ri); });
            fuelLoadRepository.deleteAll(fuelLoadRepository.findByTransactionalDocumentId(id));
            salaryPaymentRepository.deleteAll(salaryPaymentRepository.findByTransactionalDocumentId(id));

            // Hard-delete StockPurchase records and reverse stock quantities
            stockPurchaseRepository.findByTransactionalDocumentId(id)
                    .forEach(sp -> iStockPurchaseService.deleteStockPurchase(sp.getId()));
        }

        document.setDeleted(true);
        transactionalDocumentRepository.save(document);
    }

    /// Private auxiliary methods

    private TransactionalDocument createNewDocument(TransactionalDocumentDTO dto) {
        TransactionalDocument document = transactionalDocumentMapper.toEntity(dto);
        document.setDeleted(false);

        Supplier supplier = iSupplierService.getEntityById(dto.supplierId());
        document.setSupplier(supplier);

        // Set ProjectArea if provided
        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = iProjectAreaService.getEntityById(dto.projectAreaId());
            document.setProjectArea(projectArea);
        }

        // Set ProjectAreaTask if provided
        if (dto.projectAreaTaskId() != null) {
            document.setProjectAreaTask(iProjectAreaTaskService.getEntityById(dto.projectAreaTaskId()));
        }

        // Process ItemDetails if present
        if (dto.items() != null && !dto.items().isEmpty()) {
            processItemDetails(document, dto.items());
        }

        applyDocumentTypeSemanticsOnCreate(document, supplier);

        return document;
    }

    /**
     * Applies document-type-specific business rules when a document is created or reactivated:
     * <ul>
     *   <li>Invoices and Debit Notes: auto-paid only when supplier accepts CASH exclusively;
     *       otherwise add the discounted amount to the supplier's pending balance.</li>
     *   <li>Credit Notes: always marked as paid (semantically "applied");
     *       subtract the discounted amount from the supplier's pending balance.</li>
     *   <li>Other documents: marked as paid; no balance impact.</li>
     * </ul>
     */
    private void applyDocumentTypeSemanticsOnCreate(TransactionalDocument document, Supplier supplier) {
        DocumentType type = document.getDocumentType();
        if (isCreditNote(type)) {
            document.setPaid(true);
            BigDecimal discountedAmount = calculateDiscountedAmount(document, supplier);
            applyToBalance(supplier, discountedAmount.negate());
        } else if (isInvoice(type) || isDebitNote(type)) {
            List<PaymentMethod> methods = supplier.getAllowedPaymentMethods();
            boolean paid = methods != null
                    && methods.size() == 1
                    && methods.contains(PaymentMethod.CASH);
            document.setPaid(paid);

            if (!paid) {
                BigDecimal discountedAmount = calculateDiscountedAmount(document, supplier);
                applyToBalance(supplier, discountedAmount);
            }
        } else {
            document.setPaid(true);
        }
    }

    /**
     * Adds {@code delta} (which may be negative) to the supplier's pending balance.
     * Treats null balance as zero.
     */
    private void applyToBalance(Supplier supplier, BigDecimal delta) {
        if (supplier == null || delta == null || delta.signum() == 0) return;
        BigDecimal current = supplier.getPendingBalance() != null
                ? supplier.getPendingBalance()
                : BigDecimal.ZERO;
        supplier.setPendingBalance(current.add(delta));
    }

    /**
     * Returns the signed impact this document currently has on the supplier's pending balance:
     * <ul>
     *   <li>Credit Notes: always {@code -discountedAmount} (they reduce the supplier's balance).</li>
     *   <li>Invoices / Debit Notes that are unpaid: {@code +discountedAmount}.</li>
     *   <li>Anything else (paid invoice/debit, OTHER_DOCUMENT): {@code 0}.</li>
     * </ul>
     */
    private BigDecimal currentBalanceImpact(TransactionalDocument document, Supplier supplier) {
        if (supplier == null) return BigDecimal.ZERO;
        DocumentType type = document.getDocumentType();
        if (isCreditNote(type)) {
            return calculateDiscountedAmount(document, supplier).negate();
        }
        if ((isInvoice(type) || isDebitNote(type)) && Boolean.FALSE.equals(document.getPaid())) {
            return calculateDiscountedAmount(document, supplier);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Processes ItemDetails for a TransactionalDocument, validating items and calculating totals
     */
    private void processItemDetails(TransactionalDocument document, List<ItemDetailDTO> itemDetailDTOs) {
        for (ItemDetailDTO itemDetailDTO : itemDetailDTOs) {
            // Load the complete Item from database (not just validate existence)
            Item item = itemRepository.findById(itemDetailDTO.itemId())
                    .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", itemDetailDTO.itemId())));

            // Validate the item is of type COMPRA
            if (!item.getItemTypes().contains(PSG.backEnd.model.enums.ItemType.COMPRA)) {
                throw new IllegalArgumentException(
                        messageSourceHelper.getMessage("item.type.invalid.purchase", item.getName()));
            }

            // Create ItemDetail manually instead of using mapper to ensure Item reference is complete
            ItemDetail itemDetail = ItemDetail.builder()
                    .item(item)  // Complete Item with all fields loaded
                    .unitAmount(itemDetailDTO.unitAmount())
                    .quantity(itemDetailDTO.quantity())
                    .ivaPercentage(itemDetailDTO.ivaPercentage())
                    .build();

            // Handle total amount - use provided value or calculate it
            if (itemDetailDTO.totalAmount() != null) {
                // Use the provided total amount
                itemDetail.setTotalAmount(itemDetailDTO.totalAmount());
            } else {
                // Calculate total amount
                itemDetail.setTotalAmount(computeTotal(
                    itemDetailDTO.unitAmount(),
                    itemDetailDTO.quantity(),
                    itemDetailDTO.ivaPercentage()
                ));
            }

            // Use helper method to maintain bidirectional relationship
            document.addItemDetail(itemDetail);
        }
    }

    /**
     * Calculates the final amount of an invoice applying all discounts:
     * 1. Supplier's default discount (defaultDiscountPercentage)
     * 2. Document-specific discount (discountPercentage)
     *
     * @param document The transactional document
     * @param supplier The associated supplier
     * @return The amount with applied discounts
     */
    private BigDecimal calculateDiscountedAmount(TransactionalDocument document, Supplier supplier) {
        BigDecimal originalAmount = document.getTotal();

        // Apply supplier's default discount
        BigDecimal supplierDiscountPercentage = supplier.getDefaultDiscountPercentage() != null
            ? supplier.getDefaultDiscountPercentage()
            : BigDecimal.ZERO;

        // Apply document-specific discount
        BigDecimal documentDiscountPercentage = document.getDiscountPercentage() != null
            ? document.getDiscountPercentage()
            : BigDecimal.ZERO;

        // Calculate combined total discount
        // Formula: amount * (1 - discount1/100) * (1 - discount2/100)
        BigDecimal supplierDiscountFactor = BigDecimal.ONE.subtract(
            supplierDiscountPercentage.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
        );

        BigDecimal documentDiscountFactor = BigDecimal.ONE.subtract(
            documentDiscountPercentage.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)
        );

        // Apply both discounts
        BigDecimal discountedAmount = originalAmount
            .multiply(supplierDiscountFactor)
            .multiply(documentDiscountFactor)
            .setScale(2, java.math.RoundingMode.HALF_UP);

        return discountedAmount;
    }

    private TransactionalDocument reactivateExistingDocument(TransactionalDocument document, TransactionalDocumentDTO dto) {
        updateDocumentFromDTO(document, dto);
        document.setDeleted(false);
        applyDocumentTypeSemanticsOnCreate(document, document.getSupplier());
        return document;
    }

    private void validateNewDocument(TransactionalDocumentDTO dto) {
        Supplier supplier = iSupplierService.getEntityById(dto.supplierId());
        if (supplier == null) {
            throw new SupplierNotFoundException(dto.supplierId());
        }
    }

    private boolean isInvoice(DocumentType type) {
        return type == DocumentType.BILL_A
                || type == DocumentType.BILL_B
                || type == DocumentType.BILL_C;
    }

    private boolean isDebitNote(DocumentType type) {
        return type == DocumentType.DEBIT_NOTE_A
                || type == DocumentType.DEBIT_NOTE_B
                || type == DocumentType.DEBIT_NOTE_C;
    }

    private boolean isCreditNote(DocumentType type) {
        return type == DocumentType.CREDIT_NOTE_A
                || type == DocumentType.CREDIT_NOTE_B
                || type == DocumentType.CREDIT_NOTE_C;
    }

    private void validateDocumentForPayment(TransactionalDocument document, Long supplierId, BigDecimal amount) {
        validateDocumentIsPayable(document);
        validateSupplierMatch(document, supplierId);
        validateDocumentNotAlreadyPaid(document);
        validatePaymentAmount(document, amount);
    }

    /**
     * Payable documents are those that represent a liability to the supplier:
     * Invoices and Debit Notes. Credit Notes reduce the supplier's balance and
     * cannot be paid; OTHER_DOCUMENT is informational only.
     */
    private void validateDocumentIsPayable(TransactionalDocument document) {
        DocumentType type = document.getDocumentType();
        if (isCreditNote(type)) {
            throw new IllegalStateException(messageSourceHelper.getMessage("document.creditNoteCannotBePaid"));
        }
        if (!isInvoice(type) && !isDebitNote(type)) {
            throw new IllegalStateException(messageSourceHelper.getMessage("document.onlyInvoicesPaid"));
        }
    }

    private void validateSupplierMatch(TransactionalDocument document, Long supplierId) {
        if (!document.getSupplier().getId().equals(supplierId)) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("document.supplierMismatch"));
        }
    }

    private void validateDocumentNotAlreadyPaid(TransactionalDocument document) {
        if (document.getPaid()) {
            throw new IllegalStateException(messageSourceHelper.getMessage("document.alreadyPaid"));
        }
    }

    private void validatePaymentAmount(TransactionalDocument document, BigDecimal amount) {
        if (amount.compareTo(document.getTotal()) < 0) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("document.paymentAmountInsufficient"));
        }
    }

    /**
     * Reverts the payment status of a document.
     * When a document is marked as unpaid, its amount (with discounts)
     * should be added back to the supplier's pending balance.
     */
    private void revertDocumentPaymentStatus(TransactionalDocument document, Long supplierId) {
        validateSupplierMatch(document, supplierId);

        // Idempotent: if already unpaid, nothing to revert
        if (!document.getPaid()) {
            return;
        }

        // Only invoices and debit notes contribute to pendingBalance via the paid flag.
        // Reverting them to unpaid means re-adding the discounted amount to the balance.
        // Credit notes are not subject to revert (they cannot be "unpaid").
        DocumentType type = document.getDocumentType();
        if (isInvoice(type) || isDebitNote(type)) {
            Supplier supplier = document.getSupplier();
            BigDecimal discountedAmount = calculateDiscountedAmount(document, supplier);
            applyToBalance(supplier, discountedAmount);
        }

        document.setPaid(false);
    }

    /**
     * Computes the total amount for an ItemDetail using BigDecimal arithmetic.
     * Formula: unitAmount * quantity * (1 + ivaPercentage/100)
     */
    private BigDecimal computeTotal(BigDecimal unitAmount, Integer quantity, BigDecimal ivaPercentage) {
        if (unitAmount == null || quantity == null || ivaPercentage == null) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("document.totalCalculation.missing"));
        }

        BigDecimal subtotal = unitAmount.multiply(BigDecimal.valueOf(quantity));
        BigDecimal ivaFactor = BigDecimal.ONE.add(ivaPercentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        return subtotal.multiply(ivaFactor).setScale(2, RoundingMode.HALF_UP);
    }
}
