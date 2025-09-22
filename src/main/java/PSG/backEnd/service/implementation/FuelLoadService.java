package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.gasStation.FuelLoadNotFoundException;
import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadFilterDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadResponseDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchResponseDTO;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.mapper.FuelLoadMapper;
import PSG.backEnd.repository.FuelLoadRepository;
import PSG.backEnd.service.implementation.fuelload.FuelLoadFactory;
import PSG.backEnd.service.implementation.fuelload.FuelLoadBatchProcessor;
import PSG.backEnd.service.port.IFuelLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FuelLoadService implements IFuelLoadService {

    private final FuelLoadRepository fuelLoadRepository;
    private final FuelLoadMapper fuelLoadMapper;

    private final FuelLoadFactory fuelLoadFactory;
    private final FuelLoadBatchProcessor batchProcessor;

    @Override
    public FuelLoadResponseDTO createFuelLoad(FuelLoadDTO fuelLoadDTO) {
        FuelLoad fuelLoad = fuelLoadFactory.createFuelLoad(fuelLoadDTO);
        FuelLoad savedFuelLoad = fuelLoadRepository.save(fuelLoad);
        return fuelLoadMapper.toResponseDto(savedFuelLoad);
    }

    @Override
    public FuelLoadBatchResponseDTO createFuelLoadBatch(FuelLoadBatchDTO fuelLoadBatchDTO) {
        return batchProcessor.processBatch(fuelLoadBatchDTO, this::createFuelLoad);
    }

    @Override
    @Transactional(readOnly = true)
    public FuelLoadResponseDTO getFuelLoadById(Long id) {
        FuelLoad fuelLoad = findFuelLoadById(id);
        return fuelLoadMapper.toResponseDto(fuelLoad);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FuelLoadResponseDTO> getAllFuelLoads(FuelLoadFilterDTO filterDTO, Pageable pageable) {
        Page<FuelLoad> fuelLoads = fuelLoadRepository.findAllWithFilters(
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.branchCode(),
                filterDTO.ticketNumber(),
                filterDTO.fuelType(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.projectAreaId(),
                filterDTO.projectAreaName(),
                filterDTO.gasStationId(),
                pageable
        );

        return fuelLoads.map(fuelLoadMapper::toResponseDto);
    }

    @Override
    public FuelLoadResponseDTO updateFuelLoad(Long id, FuelLoadDTO fuelLoadDTO) {
        FuelLoad existingFuelLoad = findFuelLoadById(id);
        fuelLoadFactory.updateFuelLoad(existingFuelLoad, fuelLoadDTO);
        FuelLoad updatedFuelLoad = fuelLoadRepository.save(existingFuelLoad);
        return fuelLoadMapper.toResponseDto(updatedFuelLoad);
    }

    @Override
    public void deleteFuelLoad(Long id) {
        FuelLoad fuelLoad = findFuelLoadById(id);
        fuelLoadRepository.delete(fuelLoad);
    }

    /**
     * Private method that encapsulates search and exception handling.
     * Follows the DRY (Don't Repeat Yourself) principle.
     */
    private FuelLoad findFuelLoadById(Long id) {
        return fuelLoadRepository.findById(id)
                .orElseThrow(() -> new FuelLoadNotFoundException(id));
    }
}
