package PSG.backEnd.model.dto.item;

import PSG.backEnd.model.enums.ItemType;

public record ItemFilterDTO(
    String name,
    String description,
    String search,
    ItemType itemType
) {}
