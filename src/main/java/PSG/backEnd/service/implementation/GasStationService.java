package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.gasStation.GasStationNotFoundException;
import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.model.dto.gasStation.GasStationDTO;
import PSG.backEnd.model.dto.gasStation.GasStationFilterDTO;
import PSG.backEnd.model.dto.gasStation.GasStationResponseDTO;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.mapper.GasStationMapper;
import PSG.backEnd.repository.GasStationRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.service.port.IGasStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new SupplierNotFoundException(gasStationDTO.supplierId());
        }

        // Create the station WITH prices using MapStruct - much simpler now
        GasStation gasStation = gasStationMapper.toEntity(gasStationDTO);

        // Save once - prices are saved automatically as embedded components
        GasStation savedGasStation = gasStationRepository.save(gasStation);

        return gasStationMapper.toResponseDto(savedGasStation);
    }

    @Override
    @Transactional(readOnly = true)
    public GasStationResponseDTO getGasStationById(Long id) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GasStationNotFoundException(id));

        return gasStationMapper.toResponseDto(gasStation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GasStationResponseDTO> getAllGasStations(GasStationFilterDTO filterDTO, Pageable pageable) {
        Page<GasStation> gasStations = gasStationRepository.findAllWithFilters(
                filterDTO.supplierId(),
                filterDTO.supplierName(),
                filterDTO.fuelTypes(),
                filterDTO.supplierTradeName(),
                filterDTO.supplierCuit(),
                filterDTO.supplierActive(),
                pageable
        );

        return gasStations.map(gasStationMapper::toResponseDto);
    }

    @Override
    public GasStationResponseDTO updateGasStation(Long id, GasStationDTO gasStationDTO) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GasStationNotFoundException(id));

        // Verify that the supplier exists if being updated
        if (gasStationDTO.supplierId() != null &&
            !supplierRepository.existsByIdAndDeletedFalse(gasStationDTO.supplierId())) {
            throw new SupplierNotFoundException(gasStationDTO.supplierId());
        }

        // Flush price deletions before inserting new ones to avoid unique constraint
        // violations on the (gas_station_id, fuel_type) index in @ElementCollection.
        if (gasStationDTO.prices() != null) {
            gasStation.getPrices().clear();
            gasStationRepository.saveAndFlush(gasStation);
        }

        gasStationMapper.partialUpdate(gasStationDTO, gasStation);
        GasStation updatedGasStation = gasStationRepository.save(gasStation);

        return gasStationMapper.toResponseDto(updatedGasStation);
    }

    @Override
    public void deleteGasStation(Long id) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GasStationNotFoundException(id));

        gasStation.setDeleted(true);
        gasStationRepository.save(gasStation);
    }
}
