package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.gasStation.GasStationNotFoundException;
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
        // Verificar que el supplier existe
        if (!supplierRepository.existsByIdAndDeletedFalse(gasStationDTO.supplierId())) {
            throw new RuntimeException("Supplier not found with id: " + gasStationDTO.supplierId());
        }

        // Crear la estación CON los precios usando MapStruct - mucho más simple ahora
        GasStation gasStation = gasStationMapper.toEntity(gasStationDTO);

        // Guardar una sola vez - los precios se guardan automáticamente como componentes embebidos
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
                filterDTO.fuelTypes(),
                pageable
        );

        return gasStations.map(gasStationMapper::toResponseDto);
    }

    @Override
    public GasStationResponseDTO updateGasStation(Long id, GasStationDTO gasStationDTO) {
        GasStation gasStation = gasStationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GasStationNotFoundException(id));

        // Verificar que el supplier existe si se está actualizando
        if (gasStationDTO.supplierId() != null &&
            !supplierRepository.existsByIdAndDeletedFalse(gasStationDTO.supplierId())) {
            throw new RuntimeException("Supplier not found with id: " + gasStationDTO.supplierId());
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
