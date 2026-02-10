package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentAlreadyActiveException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.model.dto.item.ItemDetailDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentFilterDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.*;
import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.mapper.ItemDetailMapper;
import PSG.backEnd.model.mapper.TransactionalDocumentMapper;
import PSG.backEnd.repository.ItemDetailRepository;
import PSG.backEnd.repository.ItemRepository;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.port.IProjectAreaService;
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
    private final ISupplierService iSupplierService;
    private final MessageSourceHelper messageSourceHelper;

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

        return transactionalDocumentMapper.toResponseDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionalDocumentResponseDTO> getAllTransactionalDocuments(TransactionalDocumentFilterDTO filterDTO, Pageable pageable) {
        return transactionalDocumentRepository.findAllWithFilters(
                filterDTO.documentNumber(),
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

        // If the document is an unpaid invoice, we need to recalculate the supplier's balance
        if (isInvoice(existingDocument.getDocumentType()) && !existingDocument.getPaid()) {
            Supplier supplier = existingDocument.getSupplier();

            // Revert the previous amount with discounts
            BigDecimal previousDiscountedAmount = calculateDiscountedAmount(existingDocument, supplier);
            BigDecimal currentBalance = supplier.getPendingBalance() != null
                ? supplier.getPendingBalance()
                : BigDecimal.ZERO;
            supplier.setPendingBalance(currentBalance.subtract(previousDiscountedAmount));

            // Update the document with new values (including items if provided)
            updateDocumentFromDTO(existingDocument, dto);

            // Calculate and apply the new amount with discounts
            BigDecimal newDiscountedAmount = calculateDiscountedAmount(existingDocument, supplier);
            supplier.setPendingBalance(supplier.getPendingBalance().add(newDiscountedAmount));
        } else {
            // For paid documents or non-invoices, just update without affecting balance
            updateDocumentFromDTO(existingDocument, dto);
        }

        TransactionalDocument savedDocument = transactionalDocumentRepository.save(existingDocument);
        return transactionalDocumentMapper.toResponseDto(savedDocument);
    }

    /**
     * Updates a TransactionalDocument from DTO, handling ItemDetails correctly
     */
    private void updateDocumentFromDTO(TransactionalDocument document, TransactionalDocumentDTO dto) {
        // Update basic fields using mapper
        transactionalDocumentMapper.partialUpdate(dto, document);

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

    @Override
    @Transactional
    public void deleteTransactionalDocument(Long id) {
        TransactionalDocument document = getEntityById(id);

        // If the document is an unpaid invoice, we need to revert its impact on the balance
        if (isInvoice(document.getDocumentType()) && !document.getPaid()) {
            Supplier supplier = document.getSupplier();
            BigDecimal discountedAmount = calculateDiscountedAmount(document, supplier);

            BigDecimal currentBalance = supplier.getPendingBalance() != null
                ? supplier.getPendingBalance()
                : BigDecimal.ZERO;
            supplier.setPendingBalance(currentBalance.subtract(discountedAmount));
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

        // Process ItemDetails if present
        if (dto.items() != null && !dto.items().isEmpty()) {
        // Set ProjectArea if provided
        if (dto.projectAreaId() != null) {
            ProjectArea projectArea = iProjectAreaService.getEntityById(dto.projectAreaId());
            document.setProjectArea(projectArea);
        }

            processItemDetails(document, dto.items());
        }

        if (isInvoice(document.getDocumentType())) {
            List<PaymentMethod> methods = supplier.getAllowedPaymentMethods();
            boolean paid = methods != null
                    && methods.size() == 1
                    && methods.contains(PaymentMethod.CASH);
            document.setPaid(paid);

            if (!paid) {
                // Calculate the amount with applied discounts
                BigDecimal discountedAmount = calculateDiscountedAmount(document, supplier);

                BigDecimal pendingBalance = supplier.getPendingBalance() != null
                        ? supplier.getPendingBalance()
                        : BigDecimal.ZERO;
                supplier.setPendingBalance(pendingBalance.add(discountedAmount));
            }
        } else {
            document.setPaid(true);
        }

        return document;
    }

    /**
     * Processes ItemDetails for a TransactionalDocument, validating items and calculating totals
     */
    private void processItemDetails(TransactionalDocument document, List<ItemDetailDTO> itemDetailDTOs) {
        for (ItemDetailDTO itemDetailDTO : itemDetailDTOs) {
            // Load the complete Item from database (not just validate existence)
            Item item = itemRepository.findById(itemDetailDTO.itemId())
                    .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", itemDetailDTO.itemId())));

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

    private void validateDocumentForPayment(TransactionalDocument document, Long supplierId, BigDecimal amount) {
        validateDocumentIsInvoice(document);
        validateSupplierMatch(document, supplierId);
        validateDocumentNotAlreadyPaid(document);
        validatePaymentAmount(document, amount);
    }

    private void validateDocumentIsInvoice(TransactionalDocument document) {
        if (!isInvoice(document.getDocumentType())) {
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

        if (!document.getPaid()) {
            throw new IllegalStateException(messageSourceHelper.getMessage("document.notPaid"));
        }

        // If it's an invoice, add the discounted amount to the pending balance
        if (isInvoice(document.getDocumentType())) {
            Supplier supplier = document.getSupplier();
            BigDecimal discountedAmount = calculateDiscountedAmount(document, supplier);

            BigDecimal currentBalance = supplier.getPendingBalance() != null
                ? supplier.getPendingBalance()
                : BigDecimal.ZERO;
            supplier.setPendingBalance(currentBalance.add(discountedAmount));
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
