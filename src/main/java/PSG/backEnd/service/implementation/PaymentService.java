package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.payment.PaymentNotFoundException;
import PSG.backEnd.exception.payment.InvalidPaymentMethodException;
import PSG.backEnd.model.dto.payment.*;
import PSG.backEnd.model.entity.payment.*;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.mapper.*;
import PSG.backEnd.repository.PaymentRepository.CashPaymentRepository;
import PSG.backEnd.repository.PaymentRepository.CheckPaymentRepository;
import PSG.backEnd.repository.PaymentRepository.PaymentRepository;
import PSG.backEnd.repository.PaymentRepository.TransferPaymentRepository;
import PSG.backEnd.service.port.IPaymentService;
import PSG.backEnd.service.port.ISupplierService;
import PSG.backEnd.service.port.ITransactionalDocumentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final CashPaymentRepository cashPaymentRepository;
    private final TransferPaymentRepository transferPaymentRepository;
    private final CheckPaymentRepository checkPaymentRepository;
    private final CashPaymentMapper cashPaymentMapper;
    private final TransferPaymentMapper transferPaymentMapper;
    private final CheckPaymentMapper checkPaymentMapper;

    private final ISupplierService iSupplierService;
    private final ITransactionalDocumentService iTransactionalDocumentService;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public CashPaymentResponseDTO createCash(CashPaymentDTO dto) {
        return createPayment(
            dto.paymentDetails(),
            cashPaymentMapper::toEntityOnCreate,
            cashPaymentRepository::save,
            cashPaymentMapper::toResponse,
            dto
        );
    }

    @Override
    @Transactional
    public TransferPaymentResponseDTO createTransfer(TransferPaymentDTO dto) {
        return createPayment(
            dto.paymentDetails(),
            transferPaymentMapper::toEntityOnCreate,
            transferPaymentRepository::save,
            transferPaymentMapper::toResponse,
            dto
        );
    }

    @Override
    @Transactional
    public CheckPaymentResponseDTO createCheck(CheckPaymentDTO dto) {
        return createPayment(
            dto.paymentDetails(),
            checkPaymentMapper::toEntityOnCreate,
            checkPaymentRepository::save,
            checkPaymentMapper::toResponse,
            dto
        );
    }

    /**
     * Template method that handles common payment creation logic.
     * Respects DRY principle and SRP.
     */
    private <T, R extends PaymentResponseDTO, D> R createPayment(
        PaymentDetailsDTO paymentDetails,
        Function<D, T> entityMapper,
        Function<T, T> repository,
        Function<T, R> responseMapper,
        D dto
    ) {
        // 1. Validate that the payment method is compatible with the supplier
        PaymentMethod paymentMethod = determinePaymentMethod(dto);
        validatePaymentMethodAllowed(paymentDetails.supplierId(), paymentMethod);

        // 2. Create the specific payment type entity
        T entity = entityMapper.apply(dto);

        // 3. Execute common business logic (apply Information Expert - GRASP)
        executePaymentBusinessLogic(paymentDetails);

        // 4. Save and return response
        T savedEntity = repository.apply(entity);
        return responseMapper.apply(savedEntity);
    }

    /**
     * Determines the payment method type based on the DTO type.
     */
    private <D> PaymentMethod determinePaymentMethod(D dto) {
        if (dto instanceof CashPaymentDTO) {
            return PaymentMethod.CASH;
        } else if (dto instanceof TransferPaymentDTO) {
            return PaymentMethod.TRANSFER;
        } else if (dto instanceof CheckPaymentDTO) {
            return PaymentMethod.CHECK;
        } else {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("payment.unrecognizedType", dto.getClass().getSimpleName()));
        }
    }

    /**
     * Validates that the payment method is allowed for the supplier.
     * Respects Single Responsibility principle.
     */
    private void validatePaymentMethodAllowed(Long supplierId, PaymentMethod paymentMethod) {
        Supplier supplier = iSupplierService.getEntityById(supplierId);

        if (supplier.getAllowedPaymentMethods() == null || supplier.getAllowedPaymentMethods().isEmpty()) {
            throw new InvalidPaymentMethodException(
                messageSourceHelper.getMessage("payment.method.notConfigured", supplierId)
            );
        }

        if (!supplier.getAllowedPaymentMethods().contains(paymentMethod)) {
            String allowedMethods = supplier.getAllowedPaymentMethods().stream()
                .map(PaymentMethod::getDisplayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            throw new InvalidPaymentMethodException(
                messageSourceHelper.getMessage("payment.method.notAllowed",
                    paymentMethod.getDisplayName(),
                    supplierId,
                    allowedMethods)
            );
        }
    }

    /**
     * Method that encapsulates all common business logic.
     * Respects Information Expert principle (GRASP).
     */
    private void executePaymentBusinessLogic(PaymentDetailsDTO paymentDetails) {
        // Update supplier balance
        iSupplierService.updateSupplierBalance(
            paymentDetails.supplierId(),
            paymentDetails.amount()
        );

        // Update paid documents status
        updatePaidDocuments(paymentDetails);
    }

    /**
     * Reverts the effects of a payment in the system.
     * Used for UPDATE and DELETE operations.
     */
    private void revertPaymentBusinessLogic(PaymentDetailsDTO originalPaymentDetails) {
        // Revert supplier balance (subtract payment amount)
        iSupplierService.updateSupplierBalance(
            originalPaymentDetails.supplierId(),
            originalPaymentDetails.amount().negate() // Negative to subtract
        );

        // Revert document status (mark as unpaid)
        revertPaidDocuments(originalPaymentDetails);
    }

    /**
     * Marks documents as unpaid (reverts the payment).
     * Respects SRP - Single Responsibility Principle.
     * Silently skips documents that were already soft-deleted.
     */
    private void revertPaidDocuments(PaymentDetailsDTO paymentDetails) {
        if (paymentDetails.paidDocumentIds() == null || paymentDetails.paidDocumentIds().isEmpty()) {
            // Independent payment — no documents to revert
            return;
        }

        for (Long documentId : paymentDetails.paidDocumentIds()) {
            iTransactionalDocumentService.revertTransactionalDocumentStatusIfExists(
                documentId,
                paymentDetails.supplierId()
            );
        }
    }

    /**
     * Method responsible for updating paid documents.
     * Respects SRP - Single Responsibility Principle.
     */
    private void updatePaidDocuments(PaymentDetailsDTO paymentDetails) {
        // Check if there are documents to update (independent payments have no documents)
        if (paymentDetails.paidDocumentIds() != null && !paymentDetails.paidDocumentIds().isEmpty()) {
            for (Long documentId : paymentDetails.paidDocumentIds()) {
                iTransactionalDocumentService.updateTransactionalDocumentStatus(
                    documentId,
                    paymentDetails.supplierId(),
                    paymentDetails.amount()
                );
            }
        }
        // If paidDocumentIds is null or empty, it's an independent payment - no action on documents
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDTO> findAll(PaymentFilterDTO filter, Pageable pageable) {
        // Convert enum to its name() string — the repository query compares against string literals
        String paymentMethodName = filter.paymentMethod() != null ? filter.paymentMethod().name() : null;
        return paymentRepository.findAllWithFilters(
                paymentMethodName,
                filter.startDate(),
                filter.endDate(),
                filter.minAmount(),
                filter.maxAmount(),
                filter.transactionNumber(),
                filter.supplierId(),
                pageable
        ).map(this::mapToPaymentResponse);
    }

    /**
     * Factory method to map PaymentDetails to PaymentResponseDTO.
     * Respects Factory pattern and OCP.
     */
    private PaymentResponseDTO mapToPaymentResponse(PaymentDetails paymentDetails) {
        if (paymentDetails.getCashPayment() != null) {
            return cashPaymentMapper.toResponse(paymentDetails.getCashPayment());
        } else if (paymentDetails.getTransferPayment() != null) {
            return transferPaymentMapper.toResponse(paymentDetails.getTransferPayment());
        } else if (paymentDetails.getCheckPayment() != null) {
            return checkPaymentMapper.toResponse(paymentDetails.getCheckPayment());
        } else {
            throw new IllegalStateException(messageSourceHelper.getMessage("payment.noAssociatedType", paymentDetails.getId()));
        }
    }

    @Override
    @Transactional
    public CashPaymentResponseDTO updateCash(Long id, CashPaymentDTO dto) {
        return updatePaymentWithBusinessLogic(
            id,
            cashPaymentRepository,
            messageSourceHelper.getMessage("payment.cashNotFound", id),
            entity -> {
                // Get original data before update
                PaymentDetailsDTO originalDetails = extractPaymentDetails(entity);
                // Update the entity
                cashPaymentMapper.updateEntityFromDto(dto, entity);
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails());
            },
            cashPaymentMapper::toResponse
        );
    }

    @Override
    @Transactional
    public TransferPaymentResponseDTO updateTransfer(Long id, TransferPaymentDTO dto) {
        return updatePaymentWithBusinessLogic(
            id,
            transferPaymentRepository,
            messageSourceHelper.getMessage("payment.transferNotFound", id),
            entity -> {
                PaymentDetailsDTO originalDetails = extractPaymentDetails(entity);
                transferPaymentMapper.updateEntityFromDto(dto, entity);
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails());
            },
            transferPaymentMapper::toResponse
        );
    }

    @Override
    @Transactional
    public CheckPaymentResponseDTO updateCheck(Long id, CheckPaymentDTO dto) {
        return updatePaymentWithBusinessLogic(
            id,
            checkPaymentRepository,
            messageSourceHelper.getMessage("payment.checkNotFound", id),
            entity -> {
                PaymentDetailsDTO originalDetails = extractPaymentDetails(entity);
                checkPaymentMapper.updateEntityFromDto(dto, entity);
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails());
            },
            checkPaymentMapper::toResponse
        );
    }

    /**
     * Auxiliary class to transport update information.
     */
    private static class PaymentUpdateInfo<T> {
        final T entity;
        final PaymentDetailsDTO originalDetails;
        final PaymentDetailsDTO newDetails;

        PaymentUpdateInfo(T entity, PaymentDetailsDTO originalDetails, PaymentDetailsDTO newDetails) {
            this.entity = entity;
            this.originalDetails = originalDetails;
            this.newDetails = newDetails;
        }
    }

    /**
     * Template method for update operations with business logic.
     */
    private <T, R extends PaymentResponseDTO> R updatePaymentWithBusinessLogic(
        Long id,
        JpaRepository<T, Long> repository,
        String errorMessage,
        Function<T, PaymentUpdateInfo<T>> updateFunction,
        Function<T, R> responseMapper
    ) {
        T existing = repository.findById(id)
            .filter(entity -> {
                try {
                    var deletedField = entity.getClass().getDeclaredMethod("getDeleted");
                    deletedField.setAccessible(true);
                    Boolean deleted = (Boolean) deletedField.invoke(entity);
                    return !Boolean.TRUE.equals(deleted);
                } catch (Exception e) {
                    return true;
                }
            })
            .orElseThrow(() -> new PaymentNotFoundException(errorMessage));

        // Execute update and get information
        PaymentUpdateInfo<T> updateInfo = updateFunction.apply(existing);

        // Validate supplier change if necessary
        PaymentDetailsDTO mergedDetails = mergePaymentDetails(updateInfo.originalDetails, updateInfo.newDetails);
        if (!updateInfo.originalDetails.supplierId().equals(mergedDetails.supplierId())) {
            // If supplier changed, validate that the new supplier supports this payment type
            PaymentMethod paymentMethod = determinePaymentMethodFromEntity(existing);
            validatePaymentMethodAllowed(mergedDetails.supplierId(), paymentMethod);
        }

        // Only apply business logic if there are significant changes
        if (hasSignificantChanges(updateInfo.originalDetails, updateInfo.newDetails)) {
            // 1. Revert original payment effects
            revertPaymentBusinessLogic(updateInfo.originalDetails);

            // 2. Apply updated payment effects
            executePaymentBusinessLogic(mergedDetails);
        }

        T savedEntity = repository.save(updateInfo.entity);
        return responseMapper.apply(savedEntity);
    }

    /**
     * Determines payment method type based on payment entity.
     */
    private <T> PaymentMethod determinePaymentMethodFromEntity(T paymentEntity) {
        String className = paymentEntity.getClass().getSimpleName();
        return switch (className) {
            case "CashPayment" -> PaymentMethod.CASH;
            case "TransferPayment" -> PaymentMethod.TRANSFER;
            case "CheckPayment" -> PaymentMethod.CHECK;
            default -> throw new IllegalArgumentException(messageSourceHelper.getMessage("payment.unrecognizedEntityType", className));
        };
    }

    /**
     * Deletes a payment by its PaymentDetails ID without needing to know the type.
     * Resolves the subtype automatically and delegates to the specific delete method.
     */
    @Override
    @Transactional
    public void deleteById(Long id) {
        PaymentDetails paymentDetails = paymentRepository.findByIdWithPaymentType(id)
                .orElseThrow(() -> new PaymentNotFoundException(messageSourceHelper.getMessage("payment.notFoundGeneric", id)));
        // Initialize lazy collection within the transaction
        Hibernate.initialize(paymentDetails.getPaidDocuments());

        if (paymentDetails.getCashPayment() != null) {
            deletePaymentWithBusinessLogic(paymentDetails.getCashPayment().getId(), cashPaymentRepository,
                    messageSourceHelper.getMessage("payment.cashNotFound", id));
        } else if (paymentDetails.getTransferPayment() != null) {
            deletePaymentWithBusinessLogic(paymentDetails.getTransferPayment().getId(), transferPaymentRepository,
                    messageSourceHelper.getMessage("payment.transferNotFound", id));
        } else if (paymentDetails.getCheckPayment() != null) {
            deletePaymentWithBusinessLogic(paymentDetails.getCheckPayment().getId(), checkPaymentRepository,
                    messageSourceHelper.getMessage("payment.checkNotFound", id));
        } else {
            throw new PaymentNotFoundException(messageSourceHelper.getMessage("payment.noAssociatedType", id));
        }
    }

    /**
     * Method responsible for deleting cash payments.
     * Reverts all payment effects before marking it as deleted.
     */
    @Override
    public java.util.Optional<Long> findPaymentIdByDocumentId(Long documentId) {
        return paymentRepository.findPaymentIdByDocumentId(documentId);
    }

    @Override
    @Transactional
    public void deleteCash(Long id) {
        deletePaymentWithBusinessLogic(id, cashPaymentRepository,
                messageSourceHelper.getMessage("payment.cashNotFound", id));
    }

    /**
     * Method responsible for deleting transfer payments.
     * Reverts all payment effects before marking it as deleted.
     */
    @Override
    @Transactional
    public void deleteTransfer(Long id) {
        deletePaymentWithBusinessLogic(id, transferPaymentRepository,
                messageSourceHelper.getMessage("payment.transferNotFound", id));
    }

    /**
     * Method responsible for deleting check payments.
     * Reverts all payment effects before marking it as deleted.
     */
    @Override
    @Transactional
    public void deleteCheck(Long id) {
        deletePaymentWithBusinessLogic(id, checkPaymentRepository,
                messageSourceHelper.getMessage("payment.checkNotFound", id));
    }

    /**
     * Template method for delete operations with business logic.
     * Reverts all payment effects before marking it as deleted.
     */
    private <T> void deletePaymentWithBusinessLogic(Long id, JpaRepository<T, Long> repository, String errorMessage) {
        T existing = repository.findById(id)
            .filter(entity -> {
                try {
                    var deletedField = entity.getClass().getDeclaredMethod("getDeleted");
                    deletedField.setAccessible(true);
                    Boolean deleted = (Boolean) deletedField.invoke(entity);
                    return !Boolean.TRUE.equals(deleted);
                } catch (Exception e) {
                    return true;
                }
            })
            .orElseThrow(() -> new PaymentNotFoundException(errorMessage));

        // 1. Extract payment information before deleting it
        PaymentDetailsDTO paymentDetails = extractPaymentDetails(existing);

        // 2. Revert all payment effects in the system
        revertPaymentBusinessLogic(paymentDetails);

        // 3. Mark as deleted
        try {
            var setDeletedMethod = existing.getClass().getDeclaredMethod("setDeleted", Boolean.class);
            setDeletedMethod.setAccessible(true);
            setDeletedMethod.invoke(existing, true);
        } catch (Exception e) {
            throw new RuntimeException(messageSourceHelper.getMessage("payment.deleteFlagError"), e);
        }

        repository.save(existing);
    }

    /**
     * Extracts PaymentDetailsDTO from any payment entity type.
     */
    private <T> PaymentDetailsDTO extractPaymentDetails(T paymentEntity) {
        try {
            var paymentDetailsField = paymentEntity.getClass().getDeclaredMethod("getPaymentDetails");
            paymentDetailsField.setAccessible(true);
            PaymentDetails details = (PaymentDetails) paymentDetailsField.invoke(paymentEntity);

            List<Long> documentIds = null;
            if (details.getPaidDocuments() != null && !details.getPaidDocuments().isEmpty()) {
                documentIds = details.getPaidDocuments().stream()
                    .map(TransactionalDocument::getId)
                    .toList();
            }

            return new PaymentDetailsDTO(
                details.getPaymentDate(),
                details.getSupplier().getId(),
                details.getAmount(),
                details.getComment(),
                documentIds
            );
        } catch (Exception e) {
            throw new RuntimeException(messageSourceHelper.getMessage("payment.extractDetailsError"), e);
        }
    }

    /**
     * Verifies if there are significant changes that require business logic update.
     */
    private boolean hasSignificantChanges(PaymentDetailsDTO original, PaymentDetailsDTO updated) {
        // Get final values for comparison
        Long finalSupplierId = getValueOrOriginal(updated.supplierId(), original.supplierId());
        BigDecimal finalAmount = getValueOrOriginal(updated.amount(), original.amount());
        List<Long> finalPaidDocumentIds = getValueOrOriginal(updated.paidDocumentIds(), original.paidDocumentIds());

        // Changes in fields that affect business logic
        boolean supplierChanged = !original.supplierId().equals(finalSupplierId);
        boolean amountChanged = !original.amount().equals(finalAmount);
        boolean documentsChanged = !areDocumentListsEqual(original.paidDocumentIds(), finalPaidDocumentIds);

        return supplierChanged || amountChanged || documentsChanged;
    }

    /**
     * Compares two document ID lists, handling null cases.
     */
    private boolean areDocumentListsEqual(List<Long> list1, List<Long> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.isEmpty() && list2.isEmpty()) return true;
        return list1.equals(list2);
    }

    /**
     * Combines original details with updates (only non-null fields).
     */
    private PaymentDetailsDTO mergePaymentDetails(PaymentDetailsDTO original, PaymentDetailsDTO updated) {
        return new PaymentDetailsDTO(
            getValueOrOriginal(updated.paymentDate(), original.paymentDate()),
            getValueOrOriginal(updated.supplierId(), original.supplierId()),
            getValueOrOriginal(updated.amount(), original.amount()),
            getValueOrOriginal(updated.comment(), original.comment()),
            getValueOrOriginal(updated.paidDocumentIds(), original.paidDocumentIds())
        );
    }

    /**
     * Returns the updated value if not null, otherwise the original.
     */
    private <V> V getValueOrOriginal(V updatedValue, V originalValue) {
        return updatedValue != null ? updatedValue : originalValue;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getById(Long id) {
        PaymentDetails paymentDetails = paymentRepository.findByIdWithPaymentType(id)
                .orElseThrow(() -> new PaymentNotFoundException(messageSourceHelper.getMessage("payment.notFoundGeneric", id)));
        // Initialize lazy collection within the transaction to prevent LazyInitializationException
        Hibernate.initialize(paymentDetails.getPaidDocuments());
        return mapToPaymentResponse(paymentDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public CashPaymentResponseDTO getCash(Long id) {
        return getPayment(id, cashPaymentRepository,
                messageSourceHelper.getMessage("payment.cashNotFound", id),
                cashPaymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferPaymentResponseDTO getTransfer(Long id) {
        return getPayment(id, transferPaymentRepository,
                messageSourceHelper.getMessage("payment.transferNotFound", id),
                transferPaymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckPaymentResponseDTO getCheck(Long id) {
        return getPayment(id, checkPaymentRepository,
                messageSourceHelper.getMessage("payment.checkNotFound", id),
                checkPaymentMapper::toResponse);
    }

    /**
     * Template method for ID-based query operations.
     * Centralizes search and mapping logic.
     */
    private <T, R extends PaymentResponseDTO> R getPayment(
        Long id,
        JpaRepository<T, Long> repository,
        String errorMessage,
        Function<T, R> responseMapper
    ) {
        T existing = repository.findById(id)
            .filter(entity -> {
                try {
                    var deletedField = entity.getClass().getDeclaredMethod("getDeleted");
                    deletedField.setAccessible(true);
                    Boolean deleted = (Boolean) deletedField.invoke(entity);
                    return !Boolean.TRUE.equals(deleted);
                } catch (Exception e) {
                    return true;
                }
            })
            .orElseThrow(() -> new PaymentNotFoundException(errorMessage));

        return responseMapper.apply(existing);
    }
}
