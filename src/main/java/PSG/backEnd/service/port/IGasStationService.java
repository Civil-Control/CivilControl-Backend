package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.gasStation.GasStationDTO;
import PSG.backEnd.model.dto.gasStation.GasStationFilterDTO;
import PSG.backEnd.model.dto.gasStation.GasStationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IGasStationService {
    GasStationResponseDTO createGasStation(GasStationDTO gasStationDTO);
    GasStationResponseDTO getGasStationById(Long id);
    Page<GasStationResponseDTO> getAllGasStations(GasStationFilterDTO filterDTO, Pageable pageable);
    GasStationResponseDTO updateGasStation(Long id, GasStationDTO gasStationDTO);
    void deleteGasStation(Long id);
}
