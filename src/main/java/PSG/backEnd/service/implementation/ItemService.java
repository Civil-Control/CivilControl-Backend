package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemFilterDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.mapper.ItemMapper;
import PSG.backEnd.repository.ItemRepository;
import PSG.backEnd.service.port.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class ItemService implements IItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemResponseDTO create(ItemDTO dto) {
        Item item = itemMapper.toEntity(dto);
        Item savedItem = itemRepository.save(item);
        return itemMapper.toResponse(savedItem);
    }

    @Override
    @Transactional
    public ItemResponseDTO update(Long id, ItemDTO dto) {
        Item existingItem = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));

        // Update fields from DTO
        if (dto.name() != null) {
            existingItem.setName(dto.name());
        }
        if (dto.description() != null) {
            existingItem.setDescription(dto.description());
        }

        Item updatedItem = itemRepository.save(existingItem);
        return itemMapper.toResponse(updatedItem);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new NotFoundException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDTO getById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + id));
        return itemMapper.toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponseDTO> list(ItemFilterDTO filterDTO, Pageable pageable) {
        Page<Item> items = itemRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.description(),
                pageable
        );

        return items.map(itemMapper::toResponse);
    }
}
