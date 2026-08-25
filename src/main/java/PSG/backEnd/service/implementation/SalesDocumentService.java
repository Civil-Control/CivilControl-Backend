package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.client.ClientNotFoundException;
import PSG.backEnd.exception.client.SalesDocumentAlreadyActiveException;
import PSG.backEnd.exception.client.SalesDocumentNotFoundException;
import PSG.backEnd.model.dto.sales.SalesDocumentDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentFilterDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentResponseDTO;
import PSG.backEnd.model.dto.sales.SalesItemDetailDTO;
import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
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
import PSG.backEnd.repository.SalesDocumentRepository;
import PSG.backEnd.service.port.ISalesDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
    private final CertificationRepository certificationRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaTaskRepository projectAreaTaskRepository;
    private final SalesDocumentMapper salesDocumentMapper;

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

        recalculateTotals(document);

        return salesDocumentMapper.toResponseDto(salesDocumentRepository.save(document));
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
        ).map(salesDocumentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesDocumentResponseDTO getSalesDocumentById(Long id) {
        return salesDocumentRepository.findByIdAndDeletedFalse(id)
                .map(salesDocumentMapper::toResponseDto)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
    }

    @Override
    @Transactional
    public SalesDocumentResponseDTO updateSalesDocument(Long id, SalesDocumentDTO dto) {
        SalesDocument existing = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));

        updateDocumentFromDTO(existing, dto);

        recalculateTotals(existing);

        return salesDocumentMapper.toResponseDto(salesDocumentRepository.save(existing));
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

        return salesDocumentMapper.toResponseDto(salesDocumentRepository.save(document));
    }

    @Override
    @Transactional
    public void deleteSalesDocument(Long id) {
        SalesDocument document = salesDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new SalesDocumentNotFoundException(id));
        document.setDeleted(true);
        salesDocumentRepository.save(document);
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
}
