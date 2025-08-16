package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.ContactInfoDTO;
import PSG.backEnd.model.dto.ContactInfoResponseDTO;
import PSG.backEnd.model.entity.ContactInfo;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ContactInfoMapper {

    @Mapping(target = "id", ignore = true)
    ContactInfo toEntity(ContactInfoDTO contactInfoDTO);

    ContactInfoResponseDTO toResponseDto(ContactInfo contactInfo);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void partialUpdate(ContactInfoDTO updateDTO, @MappingTarget ContactInfo contactInfo);

    @AfterMapping
    default void handleCollections(@MappingTarget ContactInfo contactInfo, ContactInfoDTO updateDTO) {
        if (updateDTO.email() != null && !updateDTO.email().isEmpty()) {
            contactInfo.setEmail(updateDTO.email());
        }
        if (updateDTO.phoneNumber() != null && !updateDTO.phoneNumber().isEmpty()) {
            contactInfo.setPhoneNumber(updateDTO.phoneNumber());
        }
    }
}
