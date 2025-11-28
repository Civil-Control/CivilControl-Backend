package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.gasStation.GasStationNotFoundException;
import PSG.backEnd.model.dto.gasStation.GasStationDTO;
import PSG.backEnd.model.dto.gasStation.GasStationFilterDTO;
import PSG.backEnd.model.dto.gasStation.GasStationResponseDTO;
import PSG.backEnd.model.dto.gasStation.GasStationPriceResponseDTO;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.mapper.GasStationMapper;
import PSG.backEnd.repository.GasStationRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.service.port.IGasStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GasStationService implements IGasStationService {

    private final GasStationRepository gasStationRepository;
    private final SupplierRepository supplierRepository;
    private final GasStationMapper gasStationMapper;

    @Override
    public GasStationResponseDTO createGasStation(GasStationDTO gasStationDTO) {
        // Verify that the supplier exists
        if (!supplierRepository.existsByIdAndDeletedFalse(gasStationDTO.supplierId())) {
            throw new RuntimeException("Supplier not found with id: " + gasStationDTO.supplierId());
        }

        // Create the station WITH prices using MapStruct - much simpler now
        GasStation gasStation = gasStationMapper.toEntity(gasStationDTO);

        // Save once - prices are saved automatically as embedded components
        GasStation savedGasStation = gasStationRepository.save(gasStation);

        return enrichResponseWithSupplierName(gasStationMapper.toResponseDto(savedGasStation));
    }

    @Override
    @Transactional(readOnly = true)
    public GasStationResponseDTO getGasStationById(Long id) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GasStationNotFoundException(id));

        return enrichResponseWithSupplierName(gasStationMapper.toResponseDto(gasStation));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GasStationResponseDTO> getAllGasStations(GasStationFilterDTO filterDTO, Pageable pageable) {
        Page<GasStation> gasStations = gasStationRepository.findAllWithFilters(
                filterDTO.supplierId(),
                filterDTO.fuelTypes(),
                pageable
        );

        return gasStations.map(gasStation -> enrichResponseWithSupplierName(gasStationMapper.toResponseDto(gasStation)));
    }

    @Override
    public GasStationResponseDTO updateGasStation(Long id, GasStationDTO gasStationDTO) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GasStationNotFoundException(id));

        // Verify that the supplier exists if being updated
        if (gasStationDTO.supplierId() != null &&
            !supplierRepository.existsByIdAndDeletedFalse(gasStationDTO.supplierId())) {
            throw new RuntimeException("Supplier not found with id: " + gasStationDTO.supplierId());
        }

        gasStationMapper.partialUpdate(gasStationDTO, gasStation);
        GasStation updatedGasStation = gasStationRepository.save(gasStation);

        return enrichResponseWithSupplierName(gasStationMapper.toResponseDto(updatedGasStation));
    }

    @Override
    public void deleteGasStation(Long id) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GasStationNotFoundException(id));

        gasStation.setDeleted(true);
        gasStationRepository.save(gasStation);
    }

    /**
     * Enriches the response with the actual supplier name instead of generic text
     */
    private GasStationResponseDTO enrichResponseWithSupplierName(GasStationResponseDTO responseDTO) {
        String supplierName = getSupplierNameById(responseDTO.supplierId());

        // Create new price list with the actual supplier name
        List<GasStationPriceResponseDTO> enrichedPrices = responseDTO.prices().stream()
                .map(price -> new GasStationPriceResponseDTO(
                    supplierName,
                    price.fuelType(),
                    price.price()
                ))
                .collect(Collectors.toList());

        // Create new DTO with the actual supplier name
        return new GasStationResponseDTO(
            responseDTO.id(),
            responseDTO.supplierId(),
            supplierName,
            responseDTO.fuelTypes(),
            enrichedPrices,
            false
        );
    }

    /**
     * Gets the supplier's legal name by its ID
     */
    private String getSupplierNameById(Long supplierId) {
        if (supplierId == null) {
            return "Supplier not specified";
        }

        return supplierRepository.findByIdAndDeletedFalse(supplierId)
                .map(Supplier::getLegalName)
                .orElse("Supplier not found");
    }
}
