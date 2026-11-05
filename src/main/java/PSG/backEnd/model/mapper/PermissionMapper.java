package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.security.PermissionDTO;
import PSG.backEnd.model.entity.security.Permission;
import org.mapstruct.Mapper;

/**
 * Mapper for Permission entity.
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionDTO toDto(Permission permission);
}

