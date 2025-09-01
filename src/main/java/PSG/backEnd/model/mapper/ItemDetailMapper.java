package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.item.ItemDetailDTO;
import PSG.backEnd.model.dto.item.ItemDetailResponseDTO;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.entity.ItemDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface ItemDetailMapper {

    // To create new ItemDetail (without id)
    @Mapping(source = "itemId", target = "item")
    @Mapping(target = "document", ignore = true)
    @Mapping(target = "id", ignore = true)
    ItemDetail toEntity(ItemDetailDTO dto);

    // To update existing ItemDetail (with id)
    @Mapping(source = "itemId", target = "item")
    @Mapping(target = "document", ignore = true)
    ItemDetail toEntityWithId(ItemDetailDTO dto);

    // To update specific attributes in existing ItemDetail
    @Mapping(source = "itemId", target = "item")
    @Mapping(target = "document", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(ItemDetailDTO dto, @MappingTarget ItemDetail entity);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.name", target = "itemName")
    ItemDetailResponseDTO toResponse(ItemDetail entity);

    // Factory method to create Item entity from itemId
    @ObjectFactory
    default Item createItem(Long itemId) {
        if (itemId == null) {
            return null;
        }
        return Item.builder().id(itemId).build();
    }

    // Helper method to map itemId to Item entity
    default Item map(Long itemId) {
        return createItem(itemId);
    }
}
