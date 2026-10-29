package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemFilterDTO;
import PSG.backEnd.model.dto.item.ItemLinkedDocumentsResponseDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IItemService {

    ItemResponseDTO create(ItemDTO dto);

    ItemResponseDTO update(Long id, ItemDTO dto);

    void delete(Long id);

    /**
     * Soft-deletes the item. When {@code unlinkDocuments} is true, every line using the item is
     * also removed from all active purchase and sales documents, whose totals are recalculated.
     */
    void delete(Long id, boolean unlinkDocuments);

    ItemLinkedDocumentsResponseDTO getLinkedDocuments(Long id);

    ItemResponseDTO getById(Long id);

    Page<ItemResponseDTO> list(ItemFilterDTO filterDTO, Pageable pageable);
}
