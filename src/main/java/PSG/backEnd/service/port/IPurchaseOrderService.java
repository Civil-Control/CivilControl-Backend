package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.purchaseOrder.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPurchaseOrderService {

    PurchaseOrderResponseDTO createPurchaseOrder(PurchaseOrderRequestDTO dto);

    PurchaseOrderResponseDTO getPurchaseOrderById(Long id);

    Page<PurchaseOrderResponseDTO> getAllPurchaseOrders(PurchaseOrderFilterDTO filterDTO, Pageable pageable);

    Page<PurchaseOrderResponseDTO> getMyPurchaseOrders(PurchaseOrderFilterDTO filterDTO, Pageable pageable);

    PurchaseOrderResponseDTO updatePurchaseOrder(Long id, PurchaseOrderRequestDTO dto);

    void deletePurchaseOrder(Long id);

    PurchaseOrderResponseDTO changeStatus(Long id, PurchaseOrderStatusDTO statusDTO);

    PurchaseOrderResponseDTO linkTransactionalDocument(Long id, PurchaseOrderLinkDocumentDTO dto);

    PurchaseOrderResponseDTO linkTransactionalDocumentFromDoc(Long purchaseOrderId, Long transactionalDocumentId);

    Long getNextOrderNumber();
}
