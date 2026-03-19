package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.gasStation.GasStationPriceDTO;
import PSG.backEnd.model.dto.gasStation.GasStationPriceResponseDTO;
import PSG.backEnd.model.entity.gasStation.GasStationPrice;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface GasStationPriceMapper {

    GasStationPrice toEntity(GasStationPriceDTO gasStationPriceDTO);

    @Mapping(target = "supplierName", ignore = true)
    @Mapping(target = "fuelType", source = "fuelType")
    GasStationPriceResponseDTO toResponseDto(GasStationPrice gasStationPrice);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void partialUpdate(GasStationPriceDTO updateDTO, @MappingTarget GasStationPrice gasStationPrice);
}
