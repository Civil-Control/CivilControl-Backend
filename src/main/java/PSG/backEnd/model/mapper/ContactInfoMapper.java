package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.contactInfo.ContactInfoDTO;
import PSG.backEnd.model.dto.contactInfo.ContactInfoResponseDTO;
import PSG.backEnd.model.entity.ContactInfo;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ContactInfoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    ContactInfo toEntity(ContactInfoDTO contactInfoDTO);

    ContactInfoResponseDTO toResponseDto(ContactInfo contactInfo);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    void partialUpdate(ContactInfoDTO updateDTO, @MappingTarget ContactInfo contactInfo);

    // Filters out blank/empty strings so the frontend can send [] or [""] without errors
    default List<String> filterBlanks(List<String> list) {
        if (list == null) return null;
        List<String> filtered = list.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        return filtered.isEmpty() ? null : filtered;
    }

    @AfterMapping
    default void handleCollections(@MappingTarget ContactInfo contactInfo, ContactInfoDTO dto) {
        contactInfo.setEmail(filterBlanks(dto.email()));
        contactInfo.setPhoneNumber(filterBlanks(dto.phoneNumber()));
    }
}
