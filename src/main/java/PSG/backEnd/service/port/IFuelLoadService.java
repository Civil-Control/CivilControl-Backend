package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadFilterDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadResponseDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IFuelLoadService {
    FuelLoadResponseDTO createFuelLoad(FuelLoadDTO fuelLoadDTO);
    FuelLoadBatchResponseDTO createFuelLoadBatch(FuelLoadBatchDTO fuelLoadBatchDTO);
    FuelLoadResponseDTO getFuelLoadById(Long id);
    Page<FuelLoadResponseDTO> getAllFuelLoads(FuelLoadFilterDTO filterDTO, Pageable pageable);
    FuelLoadResponseDTO updateFuelLoad(Long id, FuelLoadDTO fuelLoadDTO);
    void deleteFuelLoad(Long id);
}
