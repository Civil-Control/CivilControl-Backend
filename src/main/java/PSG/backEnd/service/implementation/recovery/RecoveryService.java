package PSG.backEnd.service.implementation.recovery;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.model.dto.recovery.RecoveryEventResponseDTO;
import PSG.backEnd.model.dto.recovery.RecoverySupplierConfigDTO;
import PSG.backEnd.model.dto.recovery.RecoverySupplierConfigResponseDTO;
import PSG.backEnd.model.entity.CreditNoteApplication;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.recovery.RecoveryEvent;
import PSG.backEnd.model.entity.recovery.RecoverySupplierConfig;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.treasury.CashBox;
import PSG.backEnd.model.entity.treasury.CashBoxMovement;
import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.enums.recovery.RecoveryEventType;
import PSG.backEnd.model.enums.treasury.CashBoxMovementType;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.repository.recovery.RecoveryEventRepository;
import PSG.backEnd.repository.recovery.RecoverySupplierConfigRepository;
import PSG.backEnd.repository.treasury.CashBoxRepository;
import PSG.backEnd.service.implementation.treasury.CashBoxService;
import PSG.backEnd.service.port.ICashBoxService;
import PSG.backEnd.service.port.IRecoveryService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the hidden Value Recovery feature (Feature 18).
 *
 * <p>This service is the single owner of the recovery ledger:
 * <ul>
 *   <li>Generates positive cash-box movements when a Factura A is associated to the
 *       hidden recovery sector and its supplier is configured.</li>
 *   <li>Reverses or regenerates events when the underlying document is edited or
 *       soft-deleted.</li>
 *   <li>Issues proportional adjustments when a credit note is registered against an
 *       invoice that already produced a recovery event.</li>
 * </ul>
 *
 * <p>Snapshots of the percentage and target cash box are frozen on every event so
 * later edits to the {@link RecoverySupplierConfig} never affect historic recoveries.
 *
 * <p>All operations participate in the caller's transaction (Spring's default
 * propagation): if the originating document save is rolled back, the event is rolled
 * back too — there is no dual-write inconsistency.
 */
@Service
@RequiredArgsConstructor
public class RecoveryService implements IRecoveryService {

    private final RecoverySupplierConfigRepository configRepository;
    private final RecoveryEventRepository eventRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final SupplierRepository supplierRepository;
    private final CashBoxRepository cashBoxRepository;
    private final RecoveryAssembler recoveryAssembler;
    private final ICashBoxService cashBoxService;
    private final MessageSourceHelper messages;

    // ── Public API ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Optional<RecoveryEvent> generateForDocument(TransactionalDocument document) {
        if (document == null || !isInRecoverySector(document)) {
            return Optional.empty();
        }
        // Hard validations: Factura A + supplier present + supplier configured.
        if (document.getDocumentType() != DocumentType.BILL_A) {
            throw new IllegalArgumentException(messages.getMessage(
                    "recovery.document.invalidType",
                    document.getDocumentType() != null ? document.getDocumentType().getDisplayName() : "—"));
        }
        Supplier supplier = document.getSupplier();
        if (supplier == null) {
            throw new IllegalArgumentException(messages.getMessage("recovery.document.noSupplier"));
        }
        ProjectArea sector = document.getProjectArea();
        RecoverySupplierConfig config = configRepository
                .findByProjectAreaIdAndSupplierIdAndDeletedFalse(sector.getId(), supplier.getId())
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .orElseThrow(() -> new IllegalArgumentException(messages.getMessage(
                        "recovery.supplier.notConfigured", supplier.getLegalName())));

        BigDecimal net = nullToZero(document.getNetTotal());
        BigDecimal iva = nullToZero(document.getIvaTotal());
        BigDecimal recovered = computeRecoveredAmount(net, iva, config.getRecoveryPercentage());

        if (recovered.signum() == 0) {
            // Edge case: zero-value invoice. Skip movement to avoid noise.
            return Optional.empty();
        }

        CashBox box = config.getCashBox();
        CashBoxMovement movement = ((CashBoxService) cashBoxService).applySignedMovement(
                box,
                CashBoxMovementType.RECUPERO_AUTOMATICO,
                recovered,
                document.getDate(),
                messages.getMessageOrDefault(
                        "recovery.event.movement.generated",
                        "Recupero automatico — " + documentReference(document),
                        documentReference(document)));

        RecoveryEvent event = RecoveryEvent.builder()
                .eventType(RecoveryEventType.GENERATED)
                .transactionalDocument(document)
                .creditNoteDocument(null)
                .reversesEvent(null)
                .supplierConfig(config)
                .snapshotPercentage(config.getRecoveryPercentage())
                .snapshotCashBox(box)
                .documentNet(net)
                .documentIva(iva)
                .recoveredAmount(recovered)
                .cashBoxMovement(movement)
                .occurredAt(LocalDateTime.now())
                .triggeredByUserId(currentUserIdOrSystem())
                .build();
        return Optional.of(eventRepository.save(event));
    }

    @Override
    @Transactional
    public Optional<RecoveryEvent> reverseForDocument(TransactionalDocument document) {
        if (document == null) return Optional.empty();
        Optional<RecoveryEvent> last = eventRepository.findLastActiveGeneratedForDocument(document.getId());
        if (last.isEmpty()) return Optional.empty();
        return Optional.of(buildReverseFromGenerated(last.get(), RecoveryEventType.REVERSED, null));
    }

    @Override
    @Transactional
    public Optional<RecoveryEvent> regenerateIfNeeded(TransactionalDocument document) {
        reverseForDocument(document);
        if (document == null) return Optional.empty();
        if (!isInRecoverySector(document)) return Optional.empty();
        // The eligibility / config validations live in generateForDocument and will throw
        // if the document was moved into the recovery sector with an invalid combination.
        try {
            return generateForDocument(document);
        } catch (IllegalArgumentException ex) {
            // The caller already accepted the document in its current state; we should not
            // block downstream flows when, for instance, the supplier was deconfigured.
            // Simply skip regeneration — the reverse stays applied.
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void adjustForCreditNote(TransactionalDocument creditNote) {
        if (creditNote == null) return;
        if (!isCreditNote(creditNote.getDocumentType())) return;
        if (creditNote.getCreditNoteApplications() == null || creditNote.getCreditNoteApplications().isEmpty()) {
            return;
        }
        BigDecimal cnTotal = nullToZero(creditNote.getTotal());
        if (cnTotal.signum() == 0) return;

        BigDecimal cnNet = nullToZero(creditNote.getNetTotal());
        BigDecimal cnIva = nullToZero(creditNote.getIvaTotal());

        for (CreditNoteApplication app : creditNote.getCreditNoteApplications()) {
            TransactionalDocument invoice = app.getInvoice();
            if (invoice == null) continue;
            Optional<RecoveryEvent> originalOpt = eventRepository.findLastActiveGeneratedForDocument(invoice.getId());
            if (originalOpt.isEmpty()) continue;
            RecoveryEvent original = originalOpt.get();

            // Distribute the credit note's net + iva proportionally to this application's amount.
            BigDecimal fraction = nullToZero(app.getAmountApplied())
                    .divide(cnTotal, 10, RoundingMode.HALF_UP);
            BigDecimal portionNet = cnNet.multiply(fraction);
            BigDecimal portionIva = cnIva.multiply(fraction);
            BigDecimal adjustment = computeRecoveredAmount(portionNet, portionIva, original.getSnapshotPercentage())
                    .negate();
            if (adjustment.signum() == 0) continue;

            CashBoxMovement movement = ((CashBoxService) cashBoxService).applySignedMovement(
                    original.getSnapshotCashBox(),
                    CashBoxMovementType.RECUPERO_REVERSO,
                    adjustment,
                    creditNote.getDate(),
                    messages.getMessageOrDefault(
                            "recovery.event.movement.adjusted",
                            "Ajuste por nota de credito — " + documentReference(creditNote),
                            documentReference(creditNote)));

            RecoveryEvent event = RecoveryEvent.builder()
                    .eventType(RecoveryEventType.ADJUSTED_CREDIT_NOTE)
                    .transactionalDocument(invoice)
                    .creditNoteDocument(creditNote)
                    .reversesEvent(original)
                    .supplierConfig(original.getSupplierConfig())
                    .snapshotPercentage(original.getSnapshotPercentage())
                    .snapshotCashBox(original.getSnapshotCashBox())
                    .documentNet(portionNet.setScale(2, RoundingMode.HALF_UP))
                    .documentIva(portionIva.setScale(2, RoundingMode.HALF_UP))
                    .recoveredAmount(adjustment)
                    .cashBoxMovement(movement)
                    .occurredAt(LocalDateTime.now())
                    .triggeredByUserId(currentUserIdOrSystem())
                    .build();
            eventRepository.save(event);
        }
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private RecoveryEvent buildReverseFromGenerated(RecoveryEvent original,
                                                    RecoveryEventType reverseType,
                                                    TransactionalDocument creditNoteContext) {
        BigDecimal reverseAmount = original.getRecoveredAmount().negate();
        CashBoxMovement movement = ((CashBoxService) cashBoxService).applySignedMovement(
                original.getSnapshotCashBox(),
                CashBoxMovementType.RECUPERO_REVERSO,
                reverseAmount,
                java.time.LocalDate.now(),
                messages.getMessageOrDefault(
                        "recovery.event.movement.reversed",
                        "Reversa de recupero — " + documentReference(original.getTransactionalDocument()),
                        documentReference(original.getTransactionalDocument())));

        RecoveryEvent reverse = RecoveryEvent.builder()
                .eventType(reverseType)
                .transactionalDocument(original.getTransactionalDocument())
                .creditNoteDocument(creditNoteContext)
                .reversesEvent(original)
                .supplierConfig(original.getSupplierConfig())
                .snapshotPercentage(original.getSnapshotPercentage())
                .snapshotCashBox(original.getSnapshotCashBox())
                .documentNet(original.getDocumentNet())
                .documentIva(original.getDocumentIva())
                .recoveredAmount(reverseAmount)
                .cashBoxMovement(movement)
                .occurredAt(LocalDateTime.now())
                .triggeredByUserId(currentUserIdOrSystem())
                .build();
        return eventRepository.save(reverse);
    }

    private static boolean isInRecoverySector(TransactionalDocument document) {
        ProjectArea sector = document.getProjectArea();
        return sector != null && Boolean.TRUE.equals(sector.getIsRecoverySector())
                && !Boolean.TRUE.equals(sector.getDeleted());
    }

    private static boolean isCreditNote(DocumentType type) {
        return type == DocumentType.CREDIT_NOTE_A
                || type == DocumentType.CREDIT_NOTE_B
                || type == DocumentType.CREDIT_NOTE_C;
    }

    /**
     * Recovery formula: {@code recovered = (net * percentage / 100) + iva}.
     * Result is rounded HALF_UP to 2 decimals to match cash-box scale.
     */
    private static BigDecimal computeRecoveredAmount(BigDecimal net, BigDecimal iva, BigDecimal percentage) {
        BigDecimal pct = nullToZero(percentage)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return nullToZero(net).multiply(pct)
                .add(nullToZero(iva))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String documentReference(TransactionalDocument doc) {
        if (doc == null) return "";
        String type = doc.getDocumentType() != null ? doc.getDocumentType().getDisplayName() : "Comprobante";
        return type + " " + nullSafe(doc.getBranchCode()) + "-" + nullSafe(doc.getDocumentNumber());
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    private static Long currentUserIdOrSystem() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u && u.getId() != null) {
            return u.getId();
        }
        // Fallback when invoked from a non-authenticated context (e.g. background hooks).
        // The column is NOT NULL — a sentinel of 0 keeps the historical record traceable.
        return 0L;
    }

    /**
     * Suppress unused warning for the helper method that may be invoked by future hooks.
     */
    @SuppressWarnings("unused")
    private NotFoundException notFound(String key, Object... args) {
        return new NotFoundException(messages.getMessage(key, args));
    }

    // ── Management API (configs + read models) ─────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findActiveRecoverySectorId() {
        return projectAreaRepository.findFirstByIsRecoverySectorTrueAndDeletedFalse()
                .map(ProjectArea::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecoverySupplierConfigResponseDTO> listConfigsForArea(Long projectAreaId) {
        requireRecoverySector(projectAreaId);
        return configRepository
                .findByProjectAreaIdAndDeletedFalseOrderBySupplier_LegalNameAsc(projectAreaId)
                .stream()
                .map(recoveryAssembler::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecoverySupplierConfigResponseDTO createConfig(Long projectAreaId, RecoverySupplierConfigDTO dto) {
        ProjectArea sector = requireRecoverySector(projectAreaId);
        Supplier supplier = supplierRepository.findByIdAndDeletedFalse(dto.supplierId())
                .orElseThrow(() -> new NotFoundException(messages.getMessage(
                        "recovery.supplier.notConfigured", String.valueOf(dto.supplierId()))));
        if (configRepository.existsByProjectAreaIdAndSupplierIdAndDeletedFalse(projectAreaId, supplier.getId())) {
            throw new IllegalArgumentException(messages.getMessage(
                    "recovery.supplier.alreadyConfigured", supplier.getLegalName()));
        }
        validatePercentage(dto.recoveryPercentage());
        CashBox cashBox = requireActiveCashBox(dto.cashBoxId());

        RecoverySupplierConfig config = RecoverySupplierConfig.builder()
                .projectArea(sector)
                .supplier(supplier)
                .cashBox(cashBox)
                .recoveryPercentage(dto.recoveryPercentage())
                .active(dto.active() == null ? Boolean.TRUE : dto.active())
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return recoveryAssembler.toResponse(configRepository.save(config));
    }

    @Override
    @Transactional
    public RecoverySupplierConfigResponseDTO updateConfig(Long configId, RecoverySupplierConfigDTO dto) {
        RecoverySupplierConfig config = configRepository.findByIdAndDeletedFalse(configId)
                .orElseThrow(() -> new NotFoundException(messages.getMessage(
                        "recovery.config.notFound", String.valueOf(configId))));

        if (dto.recoveryPercentage() != null) {
            validatePercentage(dto.recoveryPercentage());
            config.setRecoveryPercentage(dto.recoveryPercentage());
        }
        if (dto.cashBoxId() != null) {
            config.setCashBox(requireActiveCashBox(dto.cashBoxId()));
        }
        if (dto.active() != null) {
            config.setActive(dto.active());
        }
        config.setUpdatedAt(LocalDateTime.now());
        return recoveryAssembler.toResponse(configRepository.save(config));
    }

    @Override
    @Transactional
    public void deleteConfig(Long configId) {
        RecoverySupplierConfig config = configRepository.findByIdAndDeletedFalse(configId)
                .orElseThrow(() -> new NotFoundException(messages.getMessage(
                        "recovery.config.notFound", String.valueOf(configId))));
        // Soft-delete keeps historical events queryable; the snapshots on each event keep
        // the cash box / supplier references frozen so reports remain correct.
        config.setDeleted(true);
        config.setActive(false);
        config.setUpdatedAt(LocalDateTime.now());
        configRepository.save(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecoveryEventResponseDTO> listEventsForDocument(Long documentId) {
        return eventRepository
                .findByTransactionalDocumentIdOrderByOccurredAtAscIdAsc(documentId)
                .stream()
                .map(recoveryAssembler::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecoveryEventResponseDTO> listEventsForConfig(Long configId) {
        return eventRepository
                .findBySupplierConfigIdOrderByOccurredAtAscIdAsc(configId)
                .stream()
                .map(recoveryAssembler::toResponse)
                .toList();
    }

    private ProjectArea requireRecoverySector(Long projectAreaId) {
        ProjectArea sector = projectAreaRepository.findById(projectAreaId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new NotFoundException(messages.getMessage(
                        "recovery.notRecoverySector", String.valueOf(projectAreaId))));
        if (!Boolean.TRUE.equals(sector.getIsRecoverySector())) {
            throw new IllegalArgumentException(messages.getMessage(
                    "recovery.notRecoverySector", sector.getName()));
        }
        return sector;
    }

    private CashBox requireActiveCashBox(Long cashBoxId) {
        if (cashBoxId == null) {
            throw new IllegalArgumentException(messages.getMessage("recovery.cashBox.required"));
        }
        CashBox box = cashBoxRepository.findByIdAndDeletedFalse(cashBoxId)
                .orElseThrow(() -> new NotFoundException(messages.getMessage(
                        "recovery.cashBox.required", String.valueOf(cashBoxId))));
        if (!Boolean.TRUE.equals(box.getActive())) {
            throw new IllegalArgumentException(messages.getMessage(
                    "recovery.cashBox.inactive", box.getName()));
        }
        return box;
    }

    private void validatePercentage(BigDecimal pct) {
        if (pct == null
                || pct.compareTo(BigDecimal.ZERO) < 0
                || pct.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(messages.getMessage("recovery.percentage.range"));
        }
    }
}
