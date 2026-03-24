package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.contracts.CertificationDTO;
import PSG.backEnd.model.dto.contracts.CertificationResponseDTO;
import PSG.backEnd.model.entity.contracts.Certification;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CertificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "certificationNumber", ignore = true)
    @Mapping(source = "workContractId", target = "contract.id")
    @Mapping(source = "salesDocumentId", target = "salesDocument.id")
    Certification toEntity(CertificationDTO dto);

    @Mapping(source = "contract.id", target = "workContractId")
    @Mapping(source = "contract.contractNumber", target = "contractNumber")
    @Mapping(source = "salesDocument.id", target = "salesDocumentId")
    @Mapping(target = "salesDocumentLabel", ignore = true)
    CertificationResponseDTO toResponseDto(Certification entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "certificationNumber", ignore = true)
    @Mapping(source = "workContractId", target = "contract.id")
    @Mapping(source = "salesDocumentId", target = "salesDocument.id")
    void partialUpdate(CertificationDTO dto, @MappingTarget Certification entity);
}
