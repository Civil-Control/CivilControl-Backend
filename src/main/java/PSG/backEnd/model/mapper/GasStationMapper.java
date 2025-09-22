package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.gasStation.GasStationDTO;
import PSG.backEnd.model.dto.gasStation.GasStationResponseDTO;
import PSG.backEnd.model.dto.gasStation.GasStationPriceResponseDTO;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.entity.gasStation.GasStationPrice;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {GasStationPriceMapper.class})
public interface GasStationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "prices", source = "prices")
    @Mapping(target = "supplierId", source = "supplierId")
    GasStation toEntity(GasStationDTO gasStationDTO);

    @Mapping(target = "supplierName", source = "supplierId", qualifiedByName = "supplierIdToName")
    @Mapping(target = "fuelTypes", source = "prices", qualifiedByName = "extractFuelTypesFromPrices")
    @Mapping(target = "prices", source = ".", qualifiedByName = "mapPricesToResponseDTO")
    GasStationResponseDTO toResponseDto(GasStation gasStation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "prices", source = "prices")
    void partialUpdate(GasStationDTO updateDTO, @MappingTarget GasStation gasStation);

    @Named("supplierIdToName")
    default String supplierIdToName(Long supplierId) {
        return "Supplier " + supplierId;
    }

    @Named("extractFuelTypesFromPrices")
    default List<String> extractFuelTypesFromPrices(List<GasStationPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return List.of();
        }
        return prices.stream()
                .map(price -> price.getFuelType().getDisplayName())
                .distinct()
                .collect(Collectors.toList());
    }

    @Named("mapPricesToResponseDTO")
    default List<GasStationPriceResponseDTO> mapPricesToResponseDTO(GasStation gasStation) {
        if (gasStation.getPrices() == null || gasStation.getPrices().isEmpty()) {
            return List.of();
        }
        String supplierName = "Supplier " + gasStation.getSupplierId();
        return gasStation.getPrices().stream()
                .map(price -> new GasStationPriceResponseDTO(
                    supplierName,
                    price.getFuelType().getDisplayName(),
                    price.getPrice().doubleValue()
                ))
                .collect(Collectors.toList());
    }
}
