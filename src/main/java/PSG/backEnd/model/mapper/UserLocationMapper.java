package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.security.UserLocationDTO;
import PSG.backEnd.model.entity.UserLocation;
import org.mapstruct.*;

/**
 * MapStruct mapper for UserLocation ↔ UserLocationDTO conversions.
 */
@Mapper(componentModel = "spring")
public interface UserLocationMapper {

    UserLocationDTO toDto(UserLocation location);

    UserLocation toEntity(UserLocationDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(UserLocationDTO dto, @MappingTarget UserLocation location);
}
