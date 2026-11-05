package PSG.backEnd.model.mapper;

import PSG.backEnd.model.dto.gasStation.GasStationDTO;
import PSG.backEnd.model.dto.gasStation.GasStationResponseDTO;
import PSG.backEnd.model.dto.gasStation.GasStationPriceResponseDTO;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.entity.gasStation.GasStationPrice;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.repository.SupplierRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {GasStationPriceMapper.class})
public abstract class GasStationMapper {

    @Autowired
    protected SupplierRepository supplierRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "prices", source = "prices")
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToSupplier")
    public abstract GasStation toEntity(GasStationDTO gasStationDTO);

    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.legalName")
    @Mapping(target = "supplierTradeName", source = "supplier.tradeName")
    @Mapping(target = "supplierCuit", source = "supplier.cuit")
    @Mapping(target = "supplierActive", source = "supplier.active")
    @Mapping(target = "fuelTypes", source = "prices", qualifiedByName = "extractFuelTypesFromPrices")
    @Mapping(target = "prices", source = ".", qualifiedByName = "mapPricesToResponseDTO")
    public abstract GasStationResponseDTO toResponseDto(GasStation gasStation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "fuelLoads", ignore = true)
    @Mapping(target = "prices", source = "prices")
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "supplierIdToSupplier")
    public abstract void partialUpdate(GasStationDTO updateDTO, @MappingTarget GasStation gasStation);

    @Named("supplierIdToSupplier")
    protected Supplier supplierIdToSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId).orElse(null);
    }

    @Named("extractFuelTypesFromPrices")
    protected List<String> extractFuelTypesFromPrices(List<GasStationPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return List.of();
        }
        return prices.stream()
                .filter(price -> price.getFuelType() != null)
                .map(GasStationPrice::getFuelType)
                .distinct()
                .collect(Collectors.toList());
    }

    @Named("mapPricesToResponseDTO")
    protected List<GasStationPriceResponseDTO> mapPricesToResponseDTO(GasStation gasStation) {
        if (gasStation.getPrices() == null || gasStation.getPrices().isEmpty()) {
            return List.of();
        }
        String supplierName = gasStation.getSupplier() != null
            ? gasStation.getSupplier().getLegalName()
            : "Supplier not found";
        return gasStation.getPrices().stream()
                .filter(price -> price.getFuelType() != null && price.getPrice() != null)
                .map(price -> new GasStationPriceResponseDTO(
                    supplierName,
                    price.getFuelType(),
                    price.getPrice().doubleValue()
                ))
                .collect(Collectors.toList());
    }
}
