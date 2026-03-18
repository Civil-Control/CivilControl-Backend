package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.security.RoleRequestDTO;
import PSG.backEnd.model.dto.security.RoleResponseDTO;
import PSG.backEnd.model.dto.security.RoleSimpleDTO;
import PSG.backEnd.model.entity.security.Role;
import org.mapstruct.*;

/**
 * Mapper for Role entity.
 */
@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "active", ignore = true)
    Role toEntity(RoleRequestDTO requestDTO);

    RoleResponseDTO toResponseDto(Role role);

    RoleSimpleDTO toSimpleDto(Role role);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void partialUpdate(RoleRequestDTO updateDTO, @MappingTarget Role role);
}

