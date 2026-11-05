package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.purchaseOrder.PurchaseOrderNotFoundException;
import PSG.backEnd.exception.purchaseOrder.PurchaseOrderNotValidException;
import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.purchaseOrder.*;
import PSG.backEnd.model.entity.PurchaseOrder;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.enums.PurchaseOrderStatus;
import PSG.backEnd.model.mapper.PurchaseOrderMapper;
import PSG.backEnd.repository.PurchaseOrderRepository;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.port.IPurchaseOrderService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService implements IPurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final TransactionalDocumentRepository transactionalDocumentRepository;

    private static final Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> VALID_TRANSITIONS = Map.of(
        PurchaseOrderStatus.PENDIENTE,   Set.of(PurchaseOrderStatus.EN_REVISION),
        PurchaseOrderStatus.EN_REVISION, Set.of(PurchaseOrderStatus.APROBADA, PurchaseOrderStatus.PENDIENTE),
        PurchaseOrderStatus.APROBADA,    Set.of(PurchaseOrderStatus.COMPRADA, PurchaseOrderStatus.EN_REVISION),
        PurchaseOrderStatus.COMPRADA,    Set.of()
    );

    @Override
    @Transactional
    public PurchaseOrderResponseDTO createPurchaseOrder(PurchaseOrderRequestDTO dto) {
        PurchaseOrder order = purchaseOrderMapper.toEntity(dto);
        order.setStatus(PurchaseOrderStatus.PENDIENTE);
        order.setCreatedByUser(getCurrentUser());
        return purchaseOrderMapper.toResponseDto(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDTO getPurchaseOrderById(Long id) {
        PurchaseOrder order = findOrderById(id);
        validateOwnership(order);
        return purchaseOrderMapper.toResponseDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDTO> getAllPurchaseOrders(PurchaseOrderFilterDTO filterDTO, Pageable pageable) {
        return purchaseOrderRepository.findAllWithFilters(filterDTO, pageable)
                .map(purchaseOrderMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDTO> getMyPurchaseOrders(PurchaseOrderFilterDTO filterDTO, Pageable pageable) {
        Long userId = getCurrentUser().getId();
        return purchaseOrderRepository.findAllByCreatedByUserWithFilters(userId, filterDTO, pageable)
                .map(purchaseOrderMapper::toResponseDto);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDTO updatePurchaseOrder(Long id, PurchaseOrderRequestDTO dto) {
        PurchaseOrder order = findOrderById(id);
        validateOwnership(order);
        validatePendingStatus(order);
        purchaseOrderMapper.partialUpdate(order, dto);
        return purchaseOrderMapper.toResponseDto(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder order = findOrderById(id);
        validateOwnership(order);
        order.setDeleted(true);
        purchaseOrderRepository.save(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDTO changeStatus(Long id, PurchaseOrderStatusDTO statusDTO) {
        PurchaseOrder order = findOrderById(id);
        Set<PurchaseOrderStatus> allowed = VALID_TRANSITIONS.get(order.getStatus());
        if (!allowed.contains(statusDTO.status())) {
            throw new PurchaseOrderNotValidException(
                    MessageSourceHelper.getMessageStatic("purchaseOrder.invalidStatusTransition",
                            order.getStatus().name(), statusDTO.status().name())
            );
        }
        order.setStatus(statusDTO.status());
        return purchaseOrderMapper.toResponseDto(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDTO linkTransactionalDocument(Long id, PurchaseOrderLinkDocumentDTO dto) {
        PurchaseOrder order = findOrderById(id);
        TransactionalDocument doc = transactionalDocumentRepository.findByIdAndDeletedFalse(dto.transactionalDocumentId())
                .orElseThrow(() -> new PurchaseOrderNotValidException(
                        MessageSourceHelper.getMessageStatic("purchaseOrder.transactionalDocumentNotFound", dto.transactionalDocumentId())
                ));
        order.setTransactionalDocument(doc);
        order.setStatus(PurchaseOrderStatus.COMPRADA);
        return purchaseOrderMapper.toResponseDto(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDTO linkTransactionalDocumentFromDoc(Long purchaseOrderId, Long transactionalDocumentId) {
        PurchaseOrder order = findOrderById(purchaseOrderId);
        TransactionalDocument doc = transactionalDocumentRepository.findByIdAndDeletedFalse(transactionalDocumentId)
                .orElseThrow(() -> new PurchaseOrderNotValidException(
                        MessageSourceHelper.getMessageStatic("purchaseOrder.transactionalDocumentNotFound", transactionalDocumentId)
                ));
        order.setTransactionalDocument(doc);
        order.setStatus(PurchaseOrderStatus.COMPRADA);
        return purchaseOrderMapper.toResponseDto(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getNextOrderNumber() {
        return purchaseOrderRepository.findNextOrderNumber();
    }

    // ── Private helpers ──────────────────────────────────────────────

    private PurchaseOrder findOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .filter(o -> Boolean.FALSE.equals(o.getDeleted()))
                .orElseThrow(() -> new PurchaseOrderNotFoundException(id));
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean currentUserHasReadPermission() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(AppPermissions.PURCHASE_ORDER_READ)
                            || a.getAuthority().equals(AppPermissions.PURCHASE_ORDER_WRITE));
    }

    private void validateOwnership(PurchaseOrder order) {
        if (!currentUserHasReadPermission()) {
            User current = getCurrentUser();
            if (!order.getCreatedByUser().getId().equals(current.getId())) {
                throw new PurchaseOrderNotValidException(
                        MessageSourceHelper.getMessageStatic("purchaseOrder.notOwnedByUser")
                );
            }
        }
    }

    private void validatePendingStatus(PurchaseOrder order) {
        if (order.getStatus() != PurchaseOrderStatus.PENDIENTE) {
            throw new PurchaseOrderNotValidException(
                    MessageSourceHelper.getMessageStatic("purchaseOrder.editOnlyPending")
            );
        }
    }
}
