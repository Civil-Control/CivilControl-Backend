package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.item.ItemDetailDTO;
import PSG.backEnd.model.dto.item.ItemDetailResponseDTO;
import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.entity.ItemDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface ItemDetailMapper {

    @Mapping(source = "itemId", target = "item")
    @Mapping(target = "document", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "id", ignore = true)
    ItemDetail toEntity(ItemDetailDTO dto);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.name", target = "itemName")
    ItemDetailResponseDTO toResponse(ItemDetail entity);

    @ObjectFactory
    default Item createItem(Long itemId) {
        if (itemId == null) {
            return null;
        }
        return Item.builder().id(itemId).build();
    }

    // Método requerido por MapStruct para mapear Long a Item
    default Item map(Long itemId) {
        return createItem(itemId);
    }
}
