package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.security.UserRequestDTO;
import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.model.entity.security.User;
import org.mapstruct.*;

/**
 * Mapper for User entity.
 */
@Mapper(componentModel = "spring", uses = {RoleMapper.class, CredentialsMapper.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "credentials", ignore = true) // Credentials are handled in service
    User toEntity(UserRequestDTO requestDTO);

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserResponseDTO toResponseDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "credentials", ignore = true) // Credentials are handled in service
    void partialUpdate(UserRequestDTO updateDTO, @MappingTarget User user);
}

