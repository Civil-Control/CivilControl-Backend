package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.exception.item.ItemAlreadyExistsException;
import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemFilterDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.mapper.ItemMapper;
import PSG.backEnd.repository.ItemRepository;
import PSG.backEnd.service.port.IItemService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Validated
public class ItemService implements IItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final MessageSourceHelper messageSourceHelper;

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
        Item item = itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(messageSourceHelper.getMessage("item.notFound", id)));
        item.setDeleted(true);
        itemRepository.save(item);
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
