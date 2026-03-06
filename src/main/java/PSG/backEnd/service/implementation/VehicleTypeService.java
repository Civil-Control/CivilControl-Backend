package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.vehicle.VehicleTypeAlreadyExistsException;
import PSG.backEnd.exception.vehicle.VehicleTypeNotFoundException;
import PSG.backEnd.model.dto.vehicle.VehicleTypeDTO;
import PSG.backEnd.model.dto.vehicle.VehicleTypeResponseDTO;
import PSG.backEnd.model.entity.vehicle.VehicleType;
import PSG.backEnd.model.mapper.VehicleTypeMapper;
import PSG.backEnd.repository.VehicleTypeRepository;
import PSG.backEnd.service.port.IVehicleTypeService;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleTypeService implements IVehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleTypeMapper vehicleTypeMapper;

    @Override
    @Transactional
    public VehicleTypeResponseDTO createVehicleType(VehicleTypeDTO vehicleTypeDTO) {
        Long tenantId = TenantContext.getCurrentTenant();
        if (vehicleTypeRepository.existsByNameAndTenantId(vehicleTypeDTO.name(), tenantId)) {
            throw new VehicleTypeAlreadyExistsException(vehicleTypeDTO.name());
        }
        VehicleType vehicleType = vehicleTypeMapper.toEntity(vehicleTypeDTO);
        return vehicleTypeMapper.toResponseDto(vehicleTypeRepository.save(vehicleType));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeResponseDTO getVehicleTypeById(Long id) {
        return vehicleTypeRepository.findById(id)
                .map(vehicleTypeMapper::toResponseDto)
                .orElseThrow(() -> new VehicleTypeNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponseDTO> getAllVehicleTypes() {
        return vehicleTypeRepository.findAll()
                .stream()
                .map(vehicleTypeMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public VehicleTypeResponseDTO updateVehicleType(Long id, VehicleTypeDTO vehicleTypeDTO) {
        VehicleType existing = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new VehicleTypeNotFoundException(id));

        if (vehicleTypeDTO.name() != null && !vehicleTypeDTO.name().equals(existing.getName())) {
            Long tenantId = TenantContext.getCurrentTenant();
            if (vehicleTypeRepository.existsByNameAndTenantIdAndIdNot(vehicleTypeDTO.name(), tenantId, id)) {
                throw new VehicleTypeAlreadyExistsException(vehicleTypeDTO.name());
            }
        }

        vehicleTypeMapper.partialUpdate(vehicleTypeDTO, existing);
        return vehicleTypeMapper.toResponseDto(vehicleTypeRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteVehicleType(Long id) {
        if (!vehicleTypeRepository.existsById(id)) {
            throw new VehicleTypeNotFoundException(id);
        }
        vehicleTypeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleType getEntityById(Long id) {
        return vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new VehicleTypeNotFoundException(id));
    }
}

