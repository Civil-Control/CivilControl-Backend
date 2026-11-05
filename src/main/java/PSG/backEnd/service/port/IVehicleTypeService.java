package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.vehicle.VehicleTypeDTO;
import PSG.backEnd.model.dto.vehicle.VehicleTypeResponseDTO;
import PSG.backEnd.model.entity.vehicle.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IVehicleTypeService {
    VehicleTypeResponseDTO createVehicleType(VehicleTypeDTO vehicleTypeDTO);
    VehicleTypeResponseDTO getVehicleTypeById(Long id);
    Page<VehicleTypeResponseDTO> getAllVehicleTypes(String name, Boolean requiresTruckEquipment, Pageable pageable);
    VehicleTypeResponseDTO updateVehicleType(Long id, VehicleTypeDTO vehicleTypeDTO);
    void deleteVehicleType(Long id);
    VehicleType getEntityById(Long id);
}

