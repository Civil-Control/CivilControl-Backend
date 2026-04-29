package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.payment.PaymentNotFoundException;
import PSG.backEnd.exception.payment.InvalidPaymentMethodException;
import PSG.backEnd.model.dto.payment.*;
import PSG.backEnd.model.entity.payment.*;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.treasury.BankAccount;
import PSG.backEnd.model.entity.treasury.Checkbook;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.mapper.*;
import PSG.backEnd.repository.PaymentRepository.CashPaymentRepository;
import PSG.backEnd.repository.PaymentRepository.CheckPaymentRepository;
import PSG.backEnd.repository.PaymentRepository.PaymentApplicationRepository;
import PSG.backEnd.repository.PaymentRepository.PaymentRepository;
import PSG.backEnd.repository.PaymentRepository.TransferPaymentRepository;
import PSG.backEnd.service.implementation.treasury.TreasuryPaymentHook;
import PSG.backEnd.service.port.IPaymentService;
import PSG.backEnd.service.port.ISupplierService;
import PSG.backEnd.service.port.ITenantService;
import PSG.backEnd.service.port.ITransactionalDocumentService;
import PSG.backEnd.service.export.PaymentOrderPdfService;
import PSG.backEnd.service.util.MessageSourceHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private final ITenantService iTenantService;
    private final PaymentOrderPdfService paymentOrderPdfService;
    private final MessageSourceHelper messageSourceHelper;
    private final TreasuryPaymentHook treasuryHook;
    private final PaymentApplicationRepository paymentApplicationRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public CashPaymentResponseDTO createCash(CashPaymentDTO dto) {
        treasuryHook.validateCashPaymentCashBox(dto.cashBoxId());
        validatePaymentMethodAllowed(dto.paymentDetails().supplierId(), PaymentMethod.CASH);
        CashPayment entity = cashPaymentMapper.toEntityOnCreate(dto);
        if (dto.cashBoxId() != null) {
            entity.setCashBox(treasuryHook.resolveCashBox(dto.cashBoxId()));
        }
        attachApplications(entity.getPaymentDetails(), dto.paymentDetails());
        executePaymentBusinessLogic(dto.paymentDetails());
        CashPayment saved = cashPaymentRepository.save(entity);
        recomputeAfterFlush(collectAffectedDocIds(saved.getPaymentDetails()));
        treasuryHook.onCashCreated(saved);
        return cashPaymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TransferPaymentResponseDTO createTransfer(TransferPaymentDTO dto) {
        validatePaymentMethodAllowed(dto.paymentDetails().supplierId(), PaymentMethod.TRANSFER);
        BankAccount acc = treasuryHook.resolveBankAccount(dto.bankAccountId());
        TransferPayment entity = transferPaymentMapper.toEntityOnCreate(dto);
        entity.setBankAccount(acc);
        attachApplications(entity.getPaymentDetails(), dto.paymentDetails());
        executePaymentBusinessLogic(dto.paymentDetails());
        TransferPayment saved = transferPaymentRepository.save(entity);
        recomputeAfterFlush(collectAffectedDocIds(saved.getPaymentDetails()));
        treasuryHook.onTransferCreated(saved);
        return transferPaymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CheckPaymentResponseDTO createCheck(CheckPaymentDTO dto) {
        validatePaymentMethodAllowed(dto.paymentDetails().supplierId(), PaymentMethod.CHECK);
        Checkbook checkbook = treasuryHook.resolveCheckbook(dto.checkbookId());
        BankAccount acc;
        if (checkbook != null) {
            acc = checkbook.getBankAccount();
        } else {
            acc = treasuryHook.resolveBankAccount(dto.bankAccountId());
        }
        treasuryHook.validateCheckbookConsistency(checkbook, acc, dto.checkNumber());

        CheckPayment entity = checkPaymentMapper.toEntityOnCreate(dto);
        entity.setBankAccount(acc);
        entity.setCheckbook(checkbook);
        attachApplications(entity.getPaymentDetails(), dto.paymentDetails());
        executePaymentBusinessLogic(dto.paymentDetails());
        CheckPayment saved = checkPaymentRepository.save(entity);
        recomputeAfterFlush(collectAffectedDocIds(saved.getPaymentDetails()));
        treasuryHook.onCheckCreated(saved);
        return checkPaymentMapper.toResponse(saved);
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
     * Updates the supplier balance side-effect of registering a payment. Per-document {@code paid}
     * flags are now derived from {@link PaymentApplication} rows by
     * {@link ITransactionalDocumentService#recomputePaidStatus(Long)}; callers must invoke
     * {@link #recomputeAfterFlush(java.util.Collection)} after the entity is saved.
     */
    private void executePaymentBusinessLogic(PaymentDetailsDTO paymentDetails) {
        iSupplierService.updateSupplierBalance(
            paymentDetails.supplierId(),
            paymentDetails.amount()
        );
    }

    /**
     * Reverts the supplier-balance side-effect of a payment (used by UPDATE and DELETE flows).
     * Per-document {@code paid} flags must be recomputed by the caller after the orphan-removal
     * of the underlying {@link PaymentApplication} rows is flushed.
     */
    private void revertPaymentBusinessLogic(PaymentDetailsDTO originalPaymentDetails) {
        iSupplierService.updateSupplierBalance(
            originalPaymentDetails.supplierId(),
            originalPaymentDetails.amount().negate()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payment-application wiring (new model, replaces the legacy paid-flag toggling)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the {@link PaymentApplication} set for the given payment, attaches it to the entity
     * (cascade will persist together with the parent on save) and computes {@code onAccountAmount}.
     * Also dual-writes the legacy {@code paidDocuments} list so existing reader queries
     * (PaymentRepository.findPaymentIdByDocumentId, etc.) keep working transparently.
     *
     * <p>Distribution rules:
     * <ul>
     *   <li>If {@code dto.applications()} is provided, it is the source of truth.</li>
     *   <li>Else if {@code dto.paidDocumentIds()} is provided (legacy clients), the amount is
     *       distributed proportionally to each document's outstanding balance, capped at the
     *       outstanding. Any remainder becomes on-account credit.</li>
     *   <li>Else (independent payment), zero applications and the full amount is on-account.</li>
     *   <li>Sending both fields at once is rejected to avoid ambiguity.</li>
     * </ul>
     */
    private void attachApplications(PaymentDetails details, PaymentDetailsDTO dto) {
        // A non-null `applications` field (even an empty list) is the authoritative source of truth:
        // the caller has explicitly stated the per-document distribution. Only when it is null do we
        // fall back to the legacy `paidDocumentIds` proportional path. This prevents stale legacy ids
        // (e.g. carried forward from the persisted entity during an update merge) from re-triggering
        // a duplicated allocation when the new client already sent its own applications payload.
        boolean explicitApps = dto.applications() != null;
        boolean hasNew = explicitApps && !dto.applications().isEmpty();
        boolean hasLegacy = !explicitApps && dto.paidDocumentIds() != null && !dto.paidDocumentIds().isEmpty();
        if (explicitApps && dto.paidDocumentIds() != null && !dto.paidDocumentIds().isEmpty()) {
            // Both explicitly populated => ambiguous, reject.
            throw new IllegalArgumentException(messageSourceHelper.getMessage(
                "payment.applications.bothFieldsProvided"));
        }

        BigDecimal totalAmount = dto.amount();
        // Reset both collections; orphan removal will delete any existing PaymentApplication rows
        if (details.getApplications() == null) {
            details.setApplications(new HashSet<>());
        } else {
            details.getApplications().clear();
        }
        if (details.getPaidDocuments() == null) {
            details.setPaidDocuments(new ArrayList<>());
        } else {
            details.getPaidDocuments().clear();
        }

        BigDecimal sumApplied = BigDecimal.ZERO;
        Set<TransactionalDocument> uniqueDocs = new LinkedHashSet<>();

        if (hasNew) {
            for (PaymentApplicationDTO appDto : dto.applications()) {
                if (appDto.amountApplied() == null || appDto.amountApplied().signum() <= 0) {
                    throw new IllegalArgumentException(messageSourceHelper.getMessage(
                        "paymentApplication.amountApplied.positive"));
                }
                TransactionalDocument doc = iTransactionalDocumentService.getEntityById(appDto.documentId());
                validateDocBelongsToSupplier(doc, dto.supplierId());
                BigDecimal outstanding = computeOutstanding(doc);
                if (appDto.amountApplied().compareTo(outstanding) > 0) {
                    throw new IllegalArgumentException(messageSourceHelper.getMessage(
                        "payment.applications.exceedsOutstanding",
                        doc.getId(), outstanding.toPlainString()));
                }
                PaymentApplication pa = PaymentApplication.builder()
                    .payment(details)
                    .document(doc)
                    .amountApplied(appDto.amountApplied())
                    .appliedAt(LocalDateTime.now())
                    .build();
                details.getApplications().add(pa);
                if (uniqueDocs.add(doc)) {
                    details.getPaidDocuments().add(doc);
                }
                sumApplied = sumApplied.add(appDto.amountApplied());
            }
        } else if (hasLegacy) {
            // Legacy proportional distribution path. Fetch docs + outstandings once.
            List<TransactionalDocument> docs = new ArrayList<>();
            List<BigDecimal> outstandings = new ArrayList<>();
            BigDecimal totalOutstanding = BigDecimal.ZERO;
            for (Long docId : dto.paidDocumentIds()) {
                TransactionalDocument doc = iTransactionalDocumentService.getEntityById(docId);
                validateDocBelongsToSupplier(doc, dto.supplierId());
                BigDecimal os = computeOutstanding(doc);
                docs.add(doc);
                outstandings.add(os);
                totalOutstanding = totalOutstanding.add(os.max(BigDecimal.ZERO));
            }
            BigDecimal remaining = totalAmount;
            for (int i = 0; i < docs.size(); i++) {
                TransactionalDocument doc = docs.get(i);
                BigDecimal os = outstandings.get(i);
                if (os.signum() <= 0 || remaining.signum() <= 0) continue;
                BigDecimal share;
                if (totalOutstanding.signum() == 0) {
                    share = BigDecimal.ZERO;
                } else if (totalAmount.compareTo(totalOutstanding) >= 0) {
                    // Enough to cover everything: pay full outstanding per doc.
                    share = os;
                } else {
                    share = totalAmount.multiply(os).divide(totalOutstanding, 2, RoundingMode.HALF_UP);
                    share = share.min(os).min(remaining);
                }
                if (share.signum() <= 0) continue;
                PaymentApplication pa = PaymentApplication.builder()
                    .payment(details)
                    .document(doc)
                    .amountApplied(share)
                    .appliedAt(LocalDateTime.now())
                    .build();
                details.getApplications().add(pa);
                if (uniqueDocs.add(doc)) {
                    details.getPaidDocuments().add(doc);
                }
                sumApplied = sumApplied.add(share);
                remaining = remaining.subtract(share);
            }
        }

        BigDecimal computedOnAccount = totalAmount.subtract(sumApplied);
        BigDecimal explicitOnAccount = dto.onAccountAmount();
        BigDecimal finalOnAccount = explicitOnAccount != null ? explicitOnAccount : computedOnAccount;

        if (finalOnAccount.signum() < 0) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage(
                "payment.onAccountAmount.nonNegative"));
        }
        if (sumApplied.add(finalOnAccount).compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage(
                "payment.applications.totalsMismatch",
                sumApplied.toPlainString(), finalOnAccount.toPlainString(), totalAmount.toPlainString()));
        }
        details.setOnAccountAmount(finalOnAccount);
    }

    /**
     * Outstanding amount for an invoice/debit note: total minus already-applied credit notes
     * minus already-applied payments (excluding any that belong to soft-deleted payments).
     */
    private BigDecimal computeOutstanding(TransactionalDocument doc) {
        BigDecimal credits = paymentApplicationRepository == null ? BigDecimal.ZERO : BigDecimal.ZERO;
        // Use the same queries other layers use; null-safe.
        BigDecimal alreadyPaid = paymentApplicationRepository.sumAppliedToDocument(doc.getId());
        BigDecimal alreadyCredited = creditAppliedToDocument(doc.getId());
        BigDecimal outstanding = doc.getTotal()
            .subtract(alreadyPaid == null ? BigDecimal.ZERO : alreadyPaid)
            .subtract(alreadyCredited == null ? BigDecimal.ZERO : alreadyCredited);
        return outstanding.signum() < 0 ? BigDecimal.ZERO : outstanding;
    }

    /** Thin shim so we don't need to inject CreditNoteApplicationRepository here. */
    private BigDecimal creditAppliedToDocument(Long documentId) {
        // The recompute in TransactionalDocumentService already factors credits in; for the
        // outstanding pre-validation we conservatively allow 0 if the lookup is unreachable.
        // Spring resolves this through the document service to keep the class boundary clean.
        try {
            // Fast path: query the canonical aggregate via JPQL on the EntityManager.
            Number n = (Number) entityManager.createQuery(
                "SELECT COALESCE(SUM(a.amountApplied), 0) FROM CreditNoteApplication a " +
                "WHERE a.invoice.id = :id AND a.creditNote.deleted = false")
                .setParameter("id", documentId)
                .getSingleResult();
            return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void validateDocBelongsToSupplier(TransactionalDocument doc, Long supplierId) {
        if (doc.getSupplier() == null || !doc.getSupplier().getId().equals(supplierId)) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("document.supplierMismatch"));
        }
    }

    private List<Long> collectAffectedDocIds(PaymentDetails details) {
        if (details == null || details.getApplications() == null) return Collections.emptyList();
        return details.getApplications().stream()
            .map(a -> a.getDocument() != null ? a.getDocument().getId() : null)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
    }

    /** Forces a flush so that subsequent recompute queries see freshly-inserted/orphan-removed rows. */
    private void recomputeAfterFlush(java.util.Collection<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) return;
        entityManager.flush();
        for (Long id : documentIds) {
            iTransactionalDocumentService.recomputePaidStatus(id);
        }
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
                filter.supplierName(),
                filter.supplierId(),
                normalizeOptional(filter.amount()),
                normalizeOptional(filter.search()),
                pageable
        ).map(this::mapToPaymentResponse);
    }

    /**
     * Treats blank/whitespace-only inputs as absent so that empty form fields don't accidentally
     * filter to "matches everything that has the empty string" (which would still be every row,
     * but normalizing keeps the SQL plan simple and intent explicit).
     */
    private static String normalizeOptional(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /**
     * Factory method to map PaymentDetails to PaymentResponseDTO.
     * Respects Factory pattern and OCP.
     */
    private PaymentResponseDTO mapToPaymentResponse(PaymentDetails paymentDetails) {
        // Force-initialize the lazy applications + paidDocuments collections so the response DTO
        // carries the per-document allocations. Without this the mapper returns an empty list and
        // edit forms can't hydrate the existing imputaciones, falling back to all-zero allocations.
        if (paymentDetails.getApplications() != null) {
            Hibernate.initialize(paymentDetails.getApplications());
        }
        if (paymentDetails.getPaidDocuments() != null) {
            Hibernate.initialize(paymentDetails.getPaidDocuments());
        }
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
                // Snapshot original data BEFORE mutating the entity so we can decide
                // whether the treasury movement actually has to be reverted + re-emitted.
                PaymentDetailsDTO originalDetails = extractPaymentDetails(entity);
                Long originalCashBoxId = entity.getCashBox() != null ? entity.getCashBox().getId() : null;
                java.math.BigDecimal originalAmount = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getAmount() : null;
                java.time.LocalDate originalDate = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getPaymentDate() : null;

                // Mutate the entity
                cashPaymentMapper.updateEntityFromDto(dto, entity);
                if (dto.cashBoxId() != null) {
                    treasuryHook.validateCashPaymentCashBox(dto.cashBoxId());
                    entity.setCashBox(treasuryHook.resolveCashBox(dto.cashBoxId()));
                }

                Long newCashBoxId = entity.getCashBox() != null ? entity.getCashBox().getId() : null;
                java.math.BigDecimal newAmount = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getAmount() : null;
                java.time.LocalDate newDate = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getPaymentDate() : null;

                boolean treasuryDirty = !java.util.Objects.equals(originalCashBoxId, newCashBoxId)
                        || !equalsAmount(originalAmount, newAmount)
                        || !java.util.Objects.equals(originalDate, newDate);

                if (treasuryDirty && originalCashBoxId != null) {
                    // Revert against the ORIGINAL cash box / amount, then re-emit (in saved -> ...).
                    treasuryHook.revertCashMovementSnapshot(originalCashBoxId, originalAmount);
                }
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails(), treasuryDirty);
            },
            (saved, info) -> {
                if (info.treasuryDirty) {
                    treasuryHook.onCashCreated(saved);
                }
                return cashPaymentMapper.toResponse(saved);
            }
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
                Long originalBankAccountId = entity.getBankAccount() != null ? entity.getBankAccount().getId() : null;
                java.math.BigDecimal originalAmount = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getAmount() : null;
                java.time.LocalDate originalDate = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getPaymentDate() : null;

                transferPaymentMapper.updateEntityFromDto(dto, entity);
                if (dto.bankAccountId() != null) {
                    entity.setBankAccount(treasuryHook.resolveBankAccount(dto.bankAccountId()));
                }

                Long newBankAccountId = entity.getBankAccount() != null ? entity.getBankAccount().getId() : null;
                java.math.BigDecimal newAmount = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getAmount() : null;
                java.time.LocalDate newDate = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getPaymentDate() : null;

                boolean treasuryDirty = !java.util.Objects.equals(originalBankAccountId, newBankAccountId)
                        || !equalsAmount(originalAmount, newAmount)
                        || !java.util.Objects.equals(originalDate, newDate);

                if (treasuryDirty && originalBankAccountId != null) {
                    treasuryHook.revertTransferMovementSnapshot(originalBankAccountId, originalAmount);
                }
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails(), treasuryDirty);
            },
            (saved, info) -> {
                if (info.treasuryDirty) {
                    treasuryHook.onTransferCreated(saved);
                }
                return transferPaymentMapper.toResponse(saved);
            }
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
                Long originalBankAccountId = entity.getBankAccount() != null ? entity.getBankAccount().getId() : null;
                java.math.BigDecimal originalAmount = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getAmount() : null;
                java.time.LocalDate originalDate = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getPaymentDate() : null;

                checkPaymentMapper.updateEntityFromDto(dto, entity);
                Checkbook checkbook = treasuryHook.resolveCheckbook(dto.checkbookId());
                BankAccount acc;
                if (checkbook != null) {
                    acc = checkbook.getBankAccount();
                } else if (dto.bankAccountId() != null) {
                    acc = treasuryHook.resolveBankAccount(dto.bankAccountId());
                } else {
                    acc = entity.getBankAccount();
                }
                treasuryHook.validateCheckbookConsistency(checkbook, acc, dto.checkNumber() != null ? dto.checkNumber() : entity.getCheckNumber());
                entity.setBankAccount(acc);
                entity.setCheckbook(checkbook);

                Long newBankAccountId = entity.getBankAccount() != null ? entity.getBankAccount().getId() : null;
                java.math.BigDecimal newAmount = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getAmount() : null;
                java.time.LocalDate newDate = entity.getPaymentDetails() != null ? entity.getPaymentDetails().getPaymentDate() : null;

                boolean treasuryDirty = !java.util.Objects.equals(originalBankAccountId, newBankAccountId)
                        || !equalsAmount(originalAmount, newAmount)
                        || !java.util.Objects.equals(originalDate, newDate);

                if (treasuryDirty && originalBankAccountId != null) {
                    treasuryHook.revertCheckMovementSnapshot(originalBankAccountId, originalAmount);
                }
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails(), treasuryDirty);
            },
            (saved, info) -> {
                if (info.treasuryDirty) {
                    treasuryHook.onCheckCreated(saved);
                }
                return checkPaymentMapper.toResponse(saved);
            }
        );
    }

    /** Treats null and zero as equal; uses compareTo so 100.00 and 100 match. */
    private static boolean equalsAmount(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    /**
     * Auxiliary class to transport update information.
     */
    private static class PaymentUpdateInfo<T> {
        final T entity;
        final PaymentDetailsDTO originalDetails;
        final PaymentDetailsDTO newDetails;
        final boolean treasuryDirty;

        PaymentUpdateInfo(T entity, PaymentDetailsDTO originalDetails, PaymentDetailsDTO newDetails) {
            this(entity, originalDetails, newDetails, true);
        }

        PaymentUpdateInfo(T entity, PaymentDetailsDTO originalDetails, PaymentDetailsDTO newDetails, boolean treasuryDirty) {
            this.entity = entity;
            this.originalDetails = originalDetails;
            this.newDetails = newDetails;
            this.treasuryDirty = treasuryDirty;
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
        java.util.function.BiFunction<T, PaymentUpdateInfo<T>, R> responseMapper
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
            // Resolve the live PaymentDetails entity (still attached, has OLD applications loaded
            // because the mapper was configured to ignore the applications collection).
            PaymentDetails liveDetails = resolvePaymentDetails(updateInfo.entity);
            // Make sure the lazy applications set is initialized before we mutate it.
            if (liveDetails != null && liveDetails.getApplications() != null) {
                Hibernate.initialize(liveDetails.getApplications());
            }
            java.util.Set<Long> oldDocIds = new java.util.LinkedHashSet<>(collectAffectedDocIds(liveDetails));

            // 1. Revert original payment supplier-balance effect.
            revertPaymentBusinessLogic(updateInfo.originalDetails);

            // 2. Rebuild applications + on-account from the merged DTO and apply new balance effect.
            if (liveDetails != null) {
                attachApplications(liveDetails, mergedDetails);
            }
            executePaymentBusinessLogic(mergedDetails);

            java.util.Set<Long> docsToRecompute = new java.util.LinkedHashSet<>(oldDocIds);
            docsToRecompute.addAll(collectAffectedDocIds(liveDetails));

            T savedEntity = repository.save(updateInfo.entity);
            recomputeAfterFlush(docsToRecompute);
            return responseMapper.apply(savedEntity, updateInfo);
        }

        T savedEntity = repository.save(updateInfo.entity);
        return responseMapper.apply(savedEntity, updateInfo);
    }

    /** Reflection-based accessor; mirrors the existing {@link #extractPaymentDetails} pattern. */
    private <T> PaymentDetails resolvePaymentDetails(T paymentEntity) {
        try {
            var m = paymentEntity.getClass().getDeclaredMethod("getPaymentDetails");
            m.setAccessible(true);
            return (PaymentDetails) m.invoke(paymentEntity);
        } catch (Exception e) {
            throw new RuntimeException(messageSourceHelper.getMessage("payment.extractDetailsError"), e);
        }
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
        // A document can now be paid by multiple payments (N:M PaymentApplication). Pick the most
        // recent one (ORDER BY pd.id DESC) so the legacy single-payment lookup keeps working
        // without throwing on non-unique results.
        return paymentRepository.findPaymentIdsByDocumentId(documentId).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierOnAccountDTO getSupplierOnAccount(Long supplierId) {
        BigDecimal total = paymentApplicationRepository.sumOnAccountBySupplier(supplierId);
        return new SupplierOnAccountDTO(supplierId, total == null ? BigDecimal.ZERO : total);
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

        // 1b. Capture affected document IDs from the live applications collection so we can
        // recompute their paid status after the soft-delete is flushed (the recompute query
        // filters out soft-deleted payments via NOT EXISTS, so the docs flip back to unpaid
        // automatically without us having to delete the application rows themselves).
        PaymentDetails liveDetails = resolvePaymentDetails(existing);
        if (liveDetails != null && liveDetails.getApplications() != null) {
            Hibernate.initialize(liveDetails.getApplications());
        }
        java.util.List<Long> affectedDocs = collectAffectedDocIds(liveDetails);

        // 2. Revert all payment effects in the system
        revertPaymentBusinessLogic(paymentDetails);

        // 2b. Revert treasury effects (bank account / cash box movements)
        if (existing instanceof CheckPayment cp) {
            treasuryHook.revertCheckMovement(cp);
        } else if (existing instanceof TransferPayment tp) {
            treasuryHook.revertTransferMovement(tp);
        } else if (existing instanceof CashPayment csp) {
            treasuryHook.revertCashMovement(csp);
        }

        // 3. Mark as deleted
        try {
            var setDeletedMethod = existing.getClass().getDeclaredMethod("setDeleted", Boolean.class);
            setDeletedMethod.setAccessible(true);
            setDeletedMethod.invoke(existing, true);
        } catch (Exception e) {
            throw new RuntimeException(messageSourceHelper.getMessage("payment.deleteFlagError"), e);
        }

        repository.save(existing);
        recomputeAfterFlush(affectedDocs);
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
        // Any non-null applications payload always re-derives the per-document distribution.
        boolean applicationsProvided = updated.applications() != null;
        boolean onAccountProvided = updated.onAccountAmount() != null;

        return supplierChanged || amountChanged || documentsChanged || applicationsProvided || onAccountProvided;
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
        // When the client provided an explicit `applications` payload (even empty), it owns the
        // distribution and the legacy `paidDocumentIds` from the persisted entity must not leak
        // into the merged DTO — otherwise attachApplications would dual-process and double-count.
        boolean explicitApps = updated.applications() != null;
        List<Long> mergedPaidDocs = explicitApps
            ? null
            : getValueOrOriginal(updated.paidDocumentIds(), original.paidDocumentIds());
        return new PaymentDetailsDTO(
            getValueOrOriginal(updated.paymentDate(), original.paymentDate()),
            getValueOrOriginal(updated.supplierId(), original.supplierId()),
            getValueOrOriginal(updated.amount(), original.amount()),
            getValueOrOriginal(updated.comment(), original.comment()),
            mergedPaidDocs,
            getValueOrOriginal(updated.applications(), original.applications()),
            getValueOrOriginal(updated.onAccountAmount(), original.onAccountAmount())
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

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePaymentOrderPdf(Long id) {
        PaymentDetails paymentDetails = paymentRepository.findByIdWithPaymentType(id)
                .orElseThrow(() -> new PaymentNotFoundException(messageSourceHelper.getMessage("payment.notFoundGeneric", id)));
        Hibernate.initialize(paymentDetails.getPaidDocuments());
        Hibernate.initialize(paymentDetails.getSupplier());

        var tenant = iTenantService.getEntityById(paymentDetails.getTenantId());

        return paymentOrderPdfService.generate(paymentDetails, tenant);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Check status lifecycle (Feature 16, Part A)
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public CheckPaymentResponseDTO updateCheckStatus(Long checkPaymentId, CheckStatusUpdateDTO dto) {
        CheckPayment check = checkPaymentRepository.findById(checkPaymentId)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new PaymentNotFoundException(
                        messageSourceHelper.getMessage("payment.checkNotFound", checkPaymentId)));

        PSG.backEnd.model.enums.payment.CheckStatus next = dto.status();
        if (next == PSG.backEnd.model.enums.payment.CheckStatus.VENCIDO) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("check.status.cannotSetDerived"));
        }

        PSG.backEnd.model.enums.payment.CheckStatus previous = check.getStatus();
        if (previous != null && previous.isTerminal() && previous != next) {
            throw new IllegalStateException(messageSourceHelper.getMessage("check.status.terminal"));
        }

        boolean requiresSettledDate = next == PSG.backEnd.model.enums.payment.CheckStatus.COBRADO
                || next == PSG.backEnd.model.enums.payment.CheckStatus.RECHAZADO;
        if (requiresSettledDate && dto.settledDate() == null) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("check.status.settledDate.required"));
        }
        if (dto.settledDate() != null) {
            java.time.LocalDate paymentDate = check.getPaymentDetails() != null
                    ? check.getPaymentDetails().getPaymentDate() : null;
            if (paymentDate != null && dto.settledDate().isBefore(paymentDate)) {
                throw new IllegalArgumentException(
                        messageSourceHelper.getMessage("check.status.settledDate.beforePayment"));
            }
        }

        // Mutate
        check.setStatus(next);
        check.setSettledDate(dto.settledDate());
        check.setStatusComment(dto.statusComment());
        check.setStatusChangedAt(java.time.LocalDateTime.now());
        check.setStatusChangedByUserId(currentUserIdSafe());

        CheckPayment saved = checkPaymentRepository.save(check);

        // Treasury reflection (does nothing if previous == next or terminal target without movement)
        treasuryHook.onCheckStatusChanged(saved, previous, next);

        return checkPaymentMapper.toResponse(saved);
    }

    /** Returns the current authenticated user id, or {@code null} if not available. */
    private static Long currentUserIdSafe() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof PSG.backEnd.model.entity.security.User u) return u.getId();
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }
}
