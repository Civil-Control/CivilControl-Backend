package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IItemService {

    ItemResponseDTO create(ItemDTO dto);

    ItemResponseDTO update(Long id, ItemDTO dto);

    void delete(Long id);

    ItemResponseDTO getById(Long id);

    Page<ItemResponseDTO> list(String nameFilter, Pageable pageable);
}
