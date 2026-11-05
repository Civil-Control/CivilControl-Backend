package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.exception.item.ItemAlreadyExistsException;
import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemFilterDTO;
import PSG.backEnd.model.dto.item.ItemLinkedDocumentDTO;
import PSG.backEnd.model.dto.item.ItemLinkedDocumentsResponseDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.entity.ItemDetail;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.entity.sales.SalesItemDetail;
import PSG.backEnd.model.mapper.ItemMapper;
import PSG.backEnd.repository.ItemDetailRepository;
import PSG.backEnd.repository.ItemRepository;
import PSG.backEnd.repository.SalesItemDetailRepository;
import PSG.backEnd.service.port.IItemService;
import PSG.backEnd.service.port.ITransactionalDocumentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class ItemService implements IItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final MessageSourceHelper messageSourceHelper;
    private final ItemDetailRepository itemDetailRepository;
    private final SalesItemDetailRepository salesItemDetailRepository;
    private final DocumentTotalRecalculator documentTotalRecalculator;
    private final SalesDocumentTotalRecalculator salesDocumentTotalRecalculator;
    private final ITransactionalDocumentService transactionalDocumentService;

    @Override
    @Transactional
    public ItemResponseDTO create(ItemDTO dto) {
        if (itemRepository.existsByNameAndDeletedFalse(dto.name())) {
            throw new ItemAlreadyExistsException(messageSourceHelper.getMessage("item.name.exists"));
        }

        Optional<Item> deletedItem = itemRepository.findByNameAndDeletedTrue(dto.name());
        if (deletedItem.isPresent()) {
            return reactivate(deletedItem.get(), dto);
        }

        Item item = itemMapper.toEntity(dto);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toResponse(savedItem);
    }

    @Override
    @Transactional
    public ItemResponseDTO update(Long id, ItemDTO dto) {
        Item existingItem = itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", id)));

        // Update fields from DTO
        if (dto.name() != null && !dto.name().equals(existingItem.getName())) {
            if (itemRepository.existsByNameAndDeletedFalse(dto.name())) {
                throw new ItemAlreadyExistsException(messageSourceHelper.getMessage("item.name.exists"));
            }
            existingItem.setName(dto.name());
        }
        if (dto.description() != null) {
            existingItem.setDescription(dto.description());
        }
        if (dto.itemTypes() != null && !dto.itemTypes().isEmpty()) {
            existingItem.getItemTypes().clear();
            existingItem.getItemTypes().addAll(dto.itemTypes());
        }

        Item updatedItem = itemRepository.save(existingItem);
        return itemMapper.toResponse(updatedItem);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        delete(id, false);
    }

    @Override
    @Transactional
    public void delete(Long id, boolean unlinkDocuments) {
        Item item = itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", id)));
        if (unlinkDocuments) {
            unlinkFromPurchaseDocuments(id);
            unlinkFromSalesDocuments(id);
        }
        item.setDeleted(true);
        itemRepository.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemLinkedDocumentsResponseDTO getLinkedDocuments(Long id) {
        itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", id)));

        List<ItemLinkedDocumentDTO> purchases = summarize(
                itemDetailRepository.findActiveByItemId(id),
                ItemDetail::getDocument,
                ItemDetail::getTotalAmount,
                doc -> new DocumentInfo(doc.getId(),
                        purchaseLabel(doc),
                        doc.getSupplier() != null
                                ? firstNonBlank(doc.getSupplier().getTradeName(), doc.getSupplier().getLegalName())
                                : null,
                        doc.getDate(), doc.getTotal(), doc.getPaid()));

        List<ItemLinkedDocumentDTO> sales = summarize(
                salesItemDetailRepository.findActiveByItemId(id),
                SalesItemDetail::getSalesDocument,
                SalesItemDetail::getTotalAmount,
                doc -> new DocumentInfo(doc.getId(),
                        salesLabel(doc),
                        doc.getClient() != null
                                ? firstNonBlank(doc.getClient().getBusinessName(), doc.getClient().getTradeName())
                                : null,
                        doc.getDate(), doc.getTotal(), doc.getPaid()));

        return new ItemLinkedDocumentsResponseDTO(purchases, sales);
    }

    private void unlinkFromPurchaseDocuments(Long itemId) {
        Map<Long, TransactionalDocument> touched = new LinkedHashMap<>();
        for (ItemDetail detail : itemDetailRepository.findActiveByItemId(itemId)) {
            TransactionalDocument doc = detail.getDocument();
            doc.getItems().remove(detail);
            touched.putIfAbsent(doc.getId(), doc);
        }
        touched.values().forEach(doc -> {
            documentTotalRecalculator.recalculateAndRecover(doc.getId());
            transactionalDocumentService.recomputePaidStatus(doc.getId());
        });
    }

    private void unlinkFromSalesDocuments(Long itemId) {
        Map<Long, SalesDocument> touched = new LinkedHashMap<>();
        for (SalesItemDetail detail : salesItemDetailRepository.findActiveByItemId(itemId)) {
            SalesDocument doc = detail.getSalesDocument();
            doc.getItems().remove(detail);
            touched.putIfAbsent(doc.getId(), doc);
        }
        touched.keySet().forEach(salesDocumentTotalRecalculator::recalculate);
    }

    private record DocumentInfo(Long id, String label, String counterpart, LocalDate date, BigDecimal total, Boolean paid) {}

    private static <L, D> List<ItemLinkedDocumentDTO> summarize(List<L> lines, Function<L, D> documentOf,
                                                               Function<L, BigDecimal> lineTotalOf,
                                                               Function<D, DocumentInfo> infoOf) {
        Map<D, List<L>> byDocument = new LinkedHashMap<>();
        for (L line : lines) {
            byDocument.computeIfAbsent(documentOf.apply(line), k -> new ArrayList<>()).add(line);
        }
        List<ItemLinkedDocumentDTO> result = new ArrayList<>();
        byDocument.forEach((doc, docLines) -> {
            DocumentInfo info = infoOf.apply(doc);
            BigDecimal linesTotal = docLines.stream()
                    .map(lineTotalOf)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new ItemLinkedDocumentDTO(info.id(), info.label(), info.counterpart(), info.date(),
                    docLines.size(), linesTotal, info.total(), Boolean.TRUE.equals(info.paid())));
        });
        return result;
    }

    private static String purchaseLabel(TransactionalDocument doc) {
        String type = doc.getDocumentType() != null ? doc.getDocumentType().getDisplayName() : "";
        return (type + " " + nullToEmpty(doc.getBranchCode()) + "-" + nullToEmpty(doc.getDocumentNumber())).trim();
    }

    private static String salesLabel(SalesDocument doc) {
        String type = "";
        if (doc.getDocumentType() != null) {
            String[] parts = doc.getDocumentType().name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(part.length() == 1 ? part : part.charAt(0) + part.substring(1).toLowerCase());
            }
            type = sb.toString();
        }
        return (type + " " + nullToEmpty(doc.getBranchCode()) + "-" + nullToEmpty(doc.getDocumentNumber())).trim();
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDTO getById(Long id) {
        Item item = itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", id)));
        return itemMapper.toResponse(item);
    }

    private ItemResponseDTO reactivate(Item item, ItemDTO dto) {
        item.setName(dto.name());
        item.setDescription(dto.description());
        item.getItemTypes().clear();
        item.getItemTypes().addAll(dto.itemTypes());
        item.setDeleted(false);
        return itemMapper.toResponse(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponseDTO> list(ItemFilterDTO filterDTO, Pageable pageable) {
        Page<Item> items = itemRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.description(),
                filterDTO.search(),
                filterDTO.itemType(),
                pageable
        );

        return items.map(itemMapper::toResponse);
    }
}
