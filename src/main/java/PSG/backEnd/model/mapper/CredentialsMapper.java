package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.security.CredentialsDTO;
import PSG.backEnd.model.entity.security.Credentials;
import org.mapstruct.*;

/**
 * Mapper for Credentials entity.
 */
@Mapper(componentModel = "spring")
public interface CredentialsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "password", ignore = true) // Password is handled in service with BCrypt
    @Mapping(target = "deleted", ignore = true) // Deleted flag is handled in service
    Credentials toEntity(CredentialsDTO credentialsDTO);

    // Note: We don't map to DTO to avoid exposing credentials in responses
}

