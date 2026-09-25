package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.client.ClientNotFoundException;
import PSG.backEnd.exception.client.SalesDocumentAlreadyActiveException;
import PSG.backEnd.exception.client.SalesDocumentNotFoundException;
import PSG.backEnd.model.dto.sales.SalesCreditNoteApplicationInputDTO;
import PSG.backEnd.model.dto.sales.SalesCreditNoteApplicationResponseDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentFilterDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentResponseDTO;
import PSG.backEnd.model.dto.sales.SalesItemDetailDTO;
import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.entity.SalesCreditNoteApplication;
import PSG.backEnd.model.entity.contracts.Certification;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.entity.sales.SalesItemDetail;
import PSG.backEnd.model.enums.contracts.CertificationStatus;
import PSG.backEnd.model.enums.documents.SalesDocumentType;
import PSG.backEnd.model.mapper.SalesDocumentMapper;
import PSG.backEnd.repository.CertificationRepository;
import PSG.backEnd.repository.ClientRepository;
import PSG.backEnd.repository.ItemRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ProjectAreaTaskRepository;
import PSG.backEnd.repository.SalesCreditNoteApplicationRepository;
import PSG.backEnd.repository.SalesDocumentRepository;
import PSG.backEnd.service.port.IClientLedgerService;
import PSG.backEnd.service.port.ISalesDocumentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesDocumentService implements ISalesDocumentService {

    private final SalesDocumentRepository salesDocumentRepository;
    private final SalesCreditNoteApplicationRepository creditNoteApplicationRepository;
    private final CertificationRepository certificationRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaTaskRepository projectAreaTaskRepository;
    private final SalesDocumentMapper salesDocumentMapper;
    private final IClientLedgerService clientLedgerService;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public SalesDocumentResponseDTO createSalesDocument(SalesDocumentDTO dto) {
        // Reject creating a duplicate active document (same branch+number for the same client).
        if (salesDocumentRepository.existsByBranchCodeAndDocumentNumberAndClientIdAndDeletedFalse(
                dto.branchCode(), dto.documentNumber(), dto.clientId())) {
            throw new SalesDocumentAlreadyActiveException(dto.branchCode(), dto.documentNumber(), dto.clientId());
        }

        // If a soft-deleted document with the same branch+number exists, reactivate it instead
        // of creating a brand-new row (mirrors the purchases/transactional-document flow).
        Optional<SalesDocument> deletedDocument = salesDocumentRepository
                .findByBranchCodeAndDocumentNumberAndDeletedTrue(dto.branchCode(), dto.documentNumber());

        SalesDocument document = deletedDocument
                .map(existing -> reactivateExistingDocument(existing, dto))
                .orElseGet(() -> createNewDocument(dto));

        document = salesDocumentRepository.save(document);

        // Sync credit-note applications (if any) AFTER initial save so the document has an id.
        if (isCreditNote(document.getDocumentType())) {
            syncCreditApplications(document, dto.creditApplications());
            document = salesDocumentRepository.save(document);
        }

        recalculateTotals(document);
        SalesDocument saved = salesDocumentRepository.save(document);

        clientLedgerService.recordDocumentMovement(saved);
        if (isCreditNote(saved.getDocumentType())) {
            clientLedgerService.syncCreditNoteApplicationImputations(saved);
        }

        return enrichResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalesDocumentResponseDTO> getAllSalesDocuments(SalesDocumentFilterDTO filterDTO, Pageable pageable) {
        return salesDocumentRepository.findAllWithFilters(
                filterDTO.clientId(), filterDTO.documentType(), filterDTO.documentNumber(),
                filterDTO.dateFrom(), filterDTO.dateTo(),
                filterDTO.minTotal(), filterDTO.maxTotal(),
                filterDTO.paid(), filterDTO.projectAreaId(), filterDTO.search(),
                pageable
        ).map(this::enrichResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesDocumentResponseDTO getSalesDocumentById(Long id) {
        return salesDocumentRepository.findByIdAndDeletedFalse(id)
                .map(this::enrichResponse)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
    }

    @Override
    @Transactional
    public SalesDocumentResponseDTO updateSalesDocument(Long id, SalesDocumentDTO dto) {
        SalesDocument existing = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));

        updateDocumentFromDTO(existing, dto);

        if (isCreditNote(existing.getDocumentType())) {
            existing.setPaid(false);
        }

        SalesDocument saved = salesDocumentRepository.save(existing);

        // Re-sync credit-note applications when this is a credit note. Always re-sync (even if
        // the client did not send the field) so removing all chips actually clears the relationship.
        if (isCreditNote(saved.getDocumentType())) {
            syncCreditApplications(saved, dto.creditApplications());
            saved = salesDocumentRepository.save(saved);
        }

        recalculateTotals(saved);
        saved = salesDocumentRepository.save(saved);

        clientLedgerService.syncDocumentMovement(saved);
        if (isCreditNote(saved.getDocumentType())) {
            clientLedgerService.syncCreditNoteApplicationImputations(saved);
        }

        return enrichResponse(saved);
    }

    @Override
    @Transactional
    public SalesDocumentResponseDTO markAsPaid(Long id, boolean paid) {
        SalesDocument document = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
        document.setPaid(paid);

        // Propagate paid status to linked certifications
        List<Certification> linkedCertifications = certificationRepository.findBySalesDocumentIdAndDeletedFalse(id);
        for (Certification cert : linkedCertifications) {
            if (paid) {
                cert.setStatus(CertificationStatus.COBRADO);
            } else if (cert.getStatus() == CertificationStatus.COBRADO) {
                cert.setStatus(CertificationStatus.FACTURADO);
            }
        }
        certificationRepository.saveAll(linkedCertifications);

        return enrichResponse(salesDocumentRepository.save(document));
    }

    @Override
    @Transactional
    public void deleteSalesDocument(Long id) {
        SalesDocument document = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
        document.setDeleted(true);
        salesDocumentRepository.save(document);
        clientLedgerService.recordDocumentReversal(document);
    }

    /**
     * Marks a credit note as manually applied. The client-balance impact was already subtracted
     * at creation time, so it is not touched here. Only the {@code manuallyApplied} flag flips,
     * which moves its status to APPLIED and removes it from "available credit" listings.
     * Mirrors TransactionalDocumentService.markCreditNoteApplied.
     */
    @Override
    @Transactional
    public SalesDocumentResponseDTO markCreditNoteApplied(Long id, boolean applied) {
        SalesDocument doc = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
        if (!isCreditNote(doc.getDocumentType())) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage(
                    "salesDocument.markApplied.notCreditNote", id));
        }
        if (applied && doc.getCreditNoteApplications() != null && !doc.getCreditNoteApplications().isEmpty()) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage(
                    "salesDocument.markApplied.hasApplications", id));
        }
        doc.setManuallyApplied(applied);
        SalesDocument saved = salesDocumentRepository.save(doc);
        return enrichResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Recomputes netTotal/ivaTotal/ivaExemptTotal/total from the document's items,
     * discountPercentage and otherTaxes, mirroring the calculation performed client-side
     * in the sales document form. Keeps totals authoritative and self-healing instead of
     * trusting whatever the client happened to send.
     */
    private void recalculateTotals(SalesDocument document) {
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        BigDecimal exempt = BigDecimal.ZERO;

        for (SalesItemDetail item : document.getItems()) {
            BigDecimal unitAmount = item.getUnitAmount();
            Integer quantity = item.getQuantity();
            if (unitAmount == null || quantity == null) continue;

            BigDecimal rawSubtotal = unitAmount.multiply(BigDecimal.valueOf(quantity));
            BigDecimal ivaPct = item.getIvaPercentage();
            if (ivaPct != null && ivaPct.compareTo(BigDecimal.ZERO) > 0) {
                net = net.add(rawSubtotal.setScale(2, RoundingMode.HALF_UP));
                BigDecimal ivaAmount = rawSubtotal.multiply(ivaPct)
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                        .setScale(2, RoundingMode.HALF_UP);
                iva = iva.add(ivaAmount);
            } else {
                exempt = exempt.add(rawSubtotal.setScale(2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal netTotal = net.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaTotal = iva.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaExemptTotal = exempt.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountPct = document.getDiscountPercentage() != null ? document.getDiscountPercentage() : BigDecimal.ZERO;
        BigDecimal otherTaxes = document.getOtherTaxes() != null ? document.getOtherTaxes() : BigDecimal.ZERO;
        // Normalize back onto the entity: a PATCH that omits these leaves them null via the
        // mapper's SET_TO_NULL strategy, which would violate the NOT NULL columns on save.
        document.setDiscountPercentage(discountPct);
        document.setOtherTaxes(otherTaxes);

        BigDecimal subtotal = netTotal.add(ivaTotal).add(ivaExemptTotal);
        BigDecimal discountFactor = BigDecimal.ONE.subtract(
                discountPct.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal total = subtotal.multiply(discountFactor).add(otherTaxes).setScale(2, RoundingMode.HALF_UP);

        document.setNetTotal(netTotal);
        document.setIvaTotal(ivaTotal);
        document.setIvaExemptTotal(ivaExemptTotal);
        document.setTotal(total);
    }

    private List<SalesItemDetail> buildItemDetails(List<SalesItemDetailDTO> dtos, SalesDocument document) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
        List<SalesItemDetail> result = new ArrayList<>();
        for (SalesItemDetailDTO detailDto : dtos) {
            result.add(buildSingleItemDetail(detailDto, document));
        }
        return result;
    }

    private SalesItemDetail buildSingleItemDetail(SalesItemDetailDTO detailDto, SalesDocument document) {
        Item item = resolveSalesItem(detailDto.itemId());
        SalesItemDetail detail = salesDocumentMapper.toItemDetailEntity(detailDto);
        detail.setItem(item);
        detail.setSalesDocument(document);
        // Tipo B/C: forzar IVA a 0 (defensa server-side; el front ya lo bloquea).
        if (isIvaExemptType(document.getDocumentType())) {
            detail.setIvaPercentage(BigDecimal.ZERO);
        }
        return detail;
    }

    /**
     * Tipos B y C no dan derecho a cómputo de crédito fiscal de IVA para quien los recibe:
     * <ul>
     *   <li>Tipo C (FACTURA_C / NOTA_DEBITO_C / NOTA_CREDITO_C): emisor monotributista o
     *       exento — no hay IVA en absoluto.</li>
     *   <li>Tipo B (FACTURA_B / NOTA_DEBITO_B / NOTA_CREDITO_B): el IVA está incluido en el
     *       precio pero no se discrimina en el comprobante (a diferencia del Tipo A), así que
     *       a efectos contables de este sistema se trata igual que el Tipo C: la alícuota se
     *       fuerza a 0% y todo el monto es neto.</li>
     * </ul>
     * Mirrors TransactionalDocumentService.isIvaExemptType for purchase documents.
     */
    private boolean isIvaExemptType(SalesDocumentType type) {
        return type == SalesDocumentType.FACTURA_B
                || type == SalesDocumentType.NOTA_DEBITO_B
                || type == SalesDocumentType.NOTA_CREDITO_B
                || type == SalesDocumentType.FACTURA_C
                || type == SalesDocumentType.NOTA_DEBITO_C
                || type == SalesDocumentType.NOTA_CREDITO_C;
    }

    private Item resolveSalesItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        if (!item.getItemTypes().contains(PSG.backEnd.model.enums.ItemType.VENTA)) {
            throw new IllegalArgumentException(
                    "El item \"" + item.getName() + "\" no está habilitado para ventas.");
        }
        return item;
    }

    /**
     * Applies header + item changes from the DTO onto an existing SalesDocument. Shared by
     * both a live update and a create-over-a-soft-deleted-document reactivation, mirroring
     * TransactionalDocumentService.updateDocumentFromDTO.
     */
    private void updateDocumentFromDTO(SalesDocument document, SalesDocumentDTO dto) {
        salesDocumentMapper.partialUpdate(dto, document);

        if (dto.clientId() != null) {
            Client client = clientRepository.findByIdAndDeletedFalse(dto.clientId())
                    .orElseThrow(() -> new ClientNotFoundException(dto.clientId()));
            document.setClient(client);
        }

        if (dto.projectAreaId() != null) {
            ProjectArea pa = projectAreaRepository.findById(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException("ProjectArea not found: " + dto.projectAreaId()));
            document.setProjectArea(pa);
        } else {
            document.setProjectArea(null);
        }

        if (dto.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(dto.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException("ProjectAreaTask not found: " + dto.projectAreaTaskId()));
            document.setProjectAreaTask(task);
        } else {
            document.setProjectAreaTask(null);
        }

        if (dto.items() != null) {
            updateItemDetails(document, dto.items());
        }
    }

    /**
     * Updates items in place by id, only adding/removing what actually changed instead of
     * clearing and recreating the whole collection on every save. Mirrors
     * TransactionalDocumentService.updateItemDetails.
     */
    private void updateItemDetails(SalesDocument document, List<SalesItemDetailDTO> newItemDetailDTOs) {
        List<SalesItemDetail> existingItems = document.getItems();
        boolean forceExempt = isIvaExemptType(document.getDocumentType());

        Map<Long, SalesItemDetail> existingItemsMap = existingItems.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(SalesItemDetail::getId, item -> item));

        Set<Long> itemsToKeep = new HashSet<>();

        for (SalesItemDetailDTO detailDto : newItemDetailDTOs) {
            if (detailDto.id() != null && existingItemsMap.containsKey(detailDto.id())) {
                SalesItemDetail existingItem = existingItemsMap.get(detailDto.id());
                existingItem.setItem(resolveSalesItem(detailDto.itemId()));
                existingItem.setUnitAmount(detailDto.unitAmount());
                existingItem.setQuantity(detailDto.quantity());
                // Tipo B/C: forzar IVA a 0 (defensa server-side; el front ya lo bloquea).
                existingItem.setIvaPercentage(forceExempt ? BigDecimal.ZERO : detailDto.ivaPercentage());
                itemsToKeep.add(detailDto.id());
            } else {
                existingItems.add(buildSingleItemDetail(detailDto, document));
            }
        }

        existingItems.removeIf(item -> item.getId() != null && !itemsToKeep.contains(item.getId()));
    }

    private SalesDocument createNewDocument(SalesDocumentDTO dto) {
        SalesDocument document = salesDocumentMapper.toEntity(dto);
        document.setDeleted(false);

        if (dto.clientId() != null) {
            Client client = clientRepository.findByIdAndDeletedFalse(dto.clientId())
                    .orElseThrow(() -> new ClientNotFoundException(dto.clientId()));
            document.setClient(client);
        }

        if (dto.projectAreaId() != null) {
            ProjectArea pa = projectAreaRepository.findById(dto.projectAreaId())
                    .orElseThrow(() -> new RuntimeException("ProjectArea not found: " + dto.projectAreaId()));
            document.setProjectArea(pa);
        }

        if (dto.projectAreaTaskId() != null) {
            ProjectAreaTask task = projectAreaTaskRepository.findByIdAndDeletedFalse(dto.projectAreaTaskId())
                    .orElseThrow(() -> new RuntimeException("ProjectAreaTask not found: " + dto.projectAreaTaskId()));
            document.setProjectAreaTask(task);
        }

        List<SalesItemDetail> items = buildItemDetails(dto.items(), document);
        document.setItems(items);
        document.setPaid(Boolean.TRUE.equals(dto.paid()));

        return document;
    }

    private SalesDocument reactivateExistingDocument(SalesDocument document, SalesDocumentDTO dto) {
        updateDocumentFromDTO(document, dto);
        document.setDeleted(false);
        document.setPaid(Boolean.TRUE.equals(dto.paid()));
        return document;
    }

    // ============================================================================================
    // Credit-note applications: validation, sync, derived state. Mirrors
    // TransactionalDocumentService's equivalent section (purchases side).
    // ============================================================================================

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PARTIALLY_CREDITED = "PARTIALLY_CREDITED";
    private static final String STATUS_CREDITED = "CREDITED";
    private static final String STATUS_APPLIED = "APPLIED";
    private static final String STATUS_UNAPPLIED = "UNAPPLIED";
    private static final String STATUS_NEUTRAL = "NEUTRAL";

    private boolean isInvoice(SalesDocumentType type) {
        return type == SalesDocumentType.FACTURA_A
                || type == SalesDocumentType.FACTURA_B
                || type == SalesDocumentType.FACTURA_C;
    }

    private boolean isDebitNote(SalesDocumentType type) {
        return type == SalesDocumentType.NOTA_DEBITO_A
                || type == SalesDocumentType.NOTA_DEBITO_B
                || type == SalesDocumentType.NOTA_DEBITO_C;
    }

    private boolean isCreditNote(SalesDocumentType type) {
        return type == SalesDocumentType.NOTA_CREDITO_A
                || type == SalesDocumentType.NOTA_CREDITO_B
                || type == SalesDocumentType.NOTA_CREDITO_C;
    }

    /**
     * Replaces the set of credit-note applications attached to {@code creditNote} with the
     * entries described by {@code dtos}. Validates business rules (positive amounts, same
     * client, target is invoice/debit-note, sums do not exceed totals). If {@code dtos} is null
     * or empty, all existing applications are removed. Mirrors
     * TransactionalDocumentService.syncCreditApplications.
     */
    private void syncCreditApplications(SalesDocument creditNote, List<SalesCreditNoteApplicationInputDTO> dtos) {
        if (!isCreditNote(creditNote.getDocumentType())) {
            return;
        }

        if (dtos == null || dtos.isEmpty()) {
            creditNote.getCreditNoteApplications().clear();
            return;
        }

        BigDecimal totalApplied = BigDecimal.ZERO;
        Set<Long> seenInvoiceIds = new HashSet<>();
        for (SalesCreditNoteApplicationInputDTO dto : dtos) {
            if (dto.invoiceId() == null || dto.amountApplied() == null || dto.amountApplied().signum() <= 0) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage("salesDocument.creditApplication.invalidAmount"));
            }
            if (!seenInvoiceIds.add(dto.invoiceId())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage("salesDocument.creditApplication.duplicateInvoice"));
            }
            totalApplied = totalApplied.add(dto.amountApplied());
        }

        if (creditNote.getTotal() != null && totalApplied.compareTo(creditNote.getTotal()) > 0) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage(
                    "salesDocument.creditApplication.exceedsCreditNoteTotal",
                    totalApplied, creditNote.getTotal()));
        }

        Long clientId = creditNote.getClient() != null ? creditNote.getClient().getId() : null;

        Set<SalesCreditNoteApplication> nextApplications = new HashSet<>();
        for (SalesCreditNoteApplicationInputDTO dto : dtos) {
            SalesDocument invoice = salesDocumentRepository.findByIdAndDeletedFalse(dto.invoiceId())
                    .orElseThrow(() -> new SalesDocumentNotFoundException(dto.invoiceId()));

            if (!isInvoice(invoice.getDocumentType()) && !isDebitNote(invoice.getDocumentType())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage(
                        "salesDocument.creditApplication.targetMustBeInvoice", dto.invoiceId()));
            }

            if (clientId == null
                    || invoice.getClient() == null
                    || !clientId.equals(invoice.getClient().getId())) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage(
                        "salesDocument.creditApplication.clientMismatch", dto.invoiceId()));
            }

            BigDecimal alreadyAppliedFromOthers = BigDecimal.ZERO;
            BigDecimal sumOnInvoice = creditNoteApplicationRepository.sumAppliedToInvoice(invoice.getId());
            if (sumOnInvoice != null) {
                alreadyAppliedFromOthers = sumOnInvoice;
            }
            for (SalesCreditNoteApplication existing : creditNote.getCreditNoteApplications()) {
                if (existing.getInvoice() != null && invoice.getId().equals(existing.getInvoice().getId())) {
                    alreadyAppliedFromOthers = alreadyAppliedFromOthers.subtract(
                            existing.getAmountApplied() != null ? existing.getAmountApplied() : BigDecimal.ZERO);
                }
            }
            BigDecimal projectedTotal = alreadyAppliedFromOthers.add(dto.amountApplied());
            if (invoice.getTotal() != null && projectedTotal.compareTo(invoice.getTotal()) > 0) {
                throw new IllegalArgumentException(messageSourceHelper.getMessage(
                        "salesDocument.creditApplication.exceedsInvoiceTotal",
                        invoice.getId(), projectedTotal, invoice.getTotal()));
            }

            SalesCreditNoteApplication app = SalesCreditNoteApplication.builder()
                    .creditNote(creditNote)
                    .invoice(invoice)
                    .amountApplied(dto.amountApplied())
                    .build();
            nextApplications.add(app);
        }

        creditNote.getCreditNoteApplications().clear();
        creditNote.getCreditNoteApplications().addAll(nextApplications);
    }

    private String formatDocumentLabel(SalesDocument doc) {
        return doc.getDocumentType().name() + " " + doc.getBranchCode() + "-" + doc.getDocumentNumber();
    }

    private SalesCreditNoteApplicationResponseDTO toApplicationDto(SalesCreditNoteApplication app) {
        return new SalesCreditNoteApplicationResponseDTO(
                app.getId(),
                app.getCreditNote() != null ? app.getCreditNote().getId() : null,
                app.getCreditNote() != null ? formatDocumentLabel(app.getCreditNote()) : null,
                app.getInvoice() != null ? app.getInvoice().getId() : null,
                app.getInvoice() != null ? formatDocumentLabel(app.getInvoice()) : null,
                app.getInvoice() != null ? app.getInvoice().getTotal() : null,
                app.getAmountApplied());
    }

    /** Builds the enriched response DTO with derived business status. Mirrors TransactionalDocumentService.enrichResponse. */
    private SalesDocumentResponseDTO enrichResponse(SalesDocument doc) {
        SalesDocumentType type = doc.getDocumentType();
        BigDecimal creditApplied = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal remainingBalance = BigDecimal.ZERO;
        List<SalesCreditNoteApplicationResponseDTO> creditApplications = Collections.emptyList();
        List<SalesCreditNoteApplicationResponseDTO> appliedCredits = Collections.emptyList();
        String status;

        if (isCreditNote(type)) {
            boolean hasApplications = doc.getCreditNoteApplications() != null && !doc.getCreditNoteApplications().isEmpty();
            boolean manuallyApplied = Boolean.TRUE.equals(doc.getManuallyApplied());
            status = (hasApplications || manuallyApplied) ? STATUS_APPLIED : STATUS_UNAPPLIED;
            if (doc.getCreditNoteApplications() != null) {
                creditApplications = doc.getCreditNoteApplications().stream()
                        .map(this::toApplicationDto)
                        .toList();
            }
        } else if (isInvoice(type) || isDebitNote(type)) {
            BigDecimal sumApplied = creditNoteApplicationRepository.sumAppliedToInvoice(doc.getId());
            creditApplied = sumApplied != null ? sumApplied : BigDecimal.ZERO;
            remainingBalance = clientLedgerService.getRemainingBalance(doc);

            if (Boolean.TRUE.equals(doc.getPaid())) {
                status = STATUS_PAID;
                pendingAmount = BigDecimal.ZERO;
            } else if (remainingBalance.signum() <= 0 && creditApplied.signum() > 0) {
                status = STATUS_CREDITED;
                pendingAmount = BigDecimal.ZERO;
            } else if (creditApplied.signum() > 0) {
                status = STATUS_PARTIALLY_CREDITED;
                pendingAmount = remainingBalance;
            } else {
                status = STATUS_PENDING;
                pendingAmount = remainingBalance;
            }

            if (doc.getAppliedCredits() != null) {
                appliedCredits = doc.getAppliedCredits().stream()
                        .filter(app -> app.getCreditNote() != null && !Boolean.TRUE.equals(app.getCreditNote().getDeleted()))
                        .map(this::toApplicationDto)
                        .toList();
            }
        } else {
            status = STATUS_NEUTRAL;
        }

        return salesDocumentMapper.toEnrichedResponseDto(
                doc, status, creditApplied, pendingAmount, remainingBalance, creditApplications, appliedCredits);
    }
}
