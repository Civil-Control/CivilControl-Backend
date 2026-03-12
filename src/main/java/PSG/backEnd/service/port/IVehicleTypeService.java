package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.vehicle.VehicleTypeDTO;
import PSG.backEnd.model.dto.vehicle.VehicleTypeResponseDTO;
import PSG.backEnd.model.entity.vehicle.VehicleType;

import java.util.List;

public interface IVehicleTypeService {
    VehicleTypeResponseDTO createVehicleType(VehicleTypeDTO vehicleTypeDTO);
    VehicleTypeResponseDTO getVehicleTypeById(Long id);
    List<VehicleTypeResponseDTO> getAllVehicleTypes();
    VehicleTypeResponseDTO updateVehicleType(Long id, VehicleTypeDTO vehicleTypeDTO);
    void deleteVehicleType(Long id);
    VehicleType getEntityById(Long id);
}

