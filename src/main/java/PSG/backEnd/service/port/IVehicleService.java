package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.dto.vehicle.VehicleFilterDTO;
import PSG.backEnd.model.dto.vehicle.VehicleResponseDTO;
import PSG.backEnd.model.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IVehicleService {
    VehicleResponseDTO createVehicle(VehicleDTO vehicleDTO);
    VehicleResponseDTO getVehicleById(Long id);
    VehicleResponseDTO updateVehicle(Long id, VehicleDTO vehicleDTO);
    void deleteVehicle(Long id);
    Page<VehicleResponseDTO> getAllVehicles(VehicleFilterDTO filterDTO, Pageable pageable);
    Vehicle getEntityById(Long id);
    boolean existsById(Long id);
}
