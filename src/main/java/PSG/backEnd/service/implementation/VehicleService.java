package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.vehicle.VehicleAlreadyExistsException;
import PSG.backEnd.exception.vehicle.VehicleDataConflictException;
import PSG.backEnd.exception.vehicle.VehicleNotFoundException;
import PSG.backEnd.exception.vehicle.VehicleNotValidException;
import PSG.backEnd.exception.vehicle.VehicleTypeNotFoundException;
import PSG.backEnd.exception.vehicle.ProjectAreaNotValidException;
import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.dto.vehicle.VehicleFilterDTO;
import PSG.backEnd.model.dto.vehicle.VehicleResponseDTO;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.mapper.VehicleMapper;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.repository.VehicleTypeRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.service.port.IVehicleService;
import PSG.backEnd.service.util.MessageSourceHelper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService implements IVehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final ProjectAreaRepository projectAreaRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public VehicleResponseDTO createVehicle(VehicleDTO vehicleDTO) {
        validateVehicleTypeRequired(vehicleDTO.vehicleTypeId());
        validateNewVehicle(vehicleDTO);
        validateProjectAreaExists(vehicleDTO.projectAreaId());
        validateVehicleTypeExists(vehicleDTO.vehicleTypeId());

        Optional<Vehicle> deletedVehicle = findDeletedVehicle(vehicleDTO);

        if (deletedVehicle.isPresent()) {
            return reactivateVehicle(deletedVehicle.get(), vehicleDTO);
        }

        return createNewVehicle(vehicleDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponseDTO> getAllVehicles(VehicleFilterDTO filterDTO, Pageable pageable) {
        return vehicleRepository.findAllWithFilters(
                filterDTO.licensePlate(),
                filterDTO.brand(),
                filterDTO.model(),
                filterDTO.year(),
                filterDTO.color(),
                filterDTO.nickName(),
                filterDTO.vehicleType(),
                filterDTO.projectAreaName(),
                filterDTO.buildingName(),
                filterDTO.vtvExpirationDate(),
                filterDTO.jurisdictionType(),
                filterDTO.truckEquipment(),
                filterDTO.includeInactive(),
                pageable
        ).map(vehicleMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getVehicleById(Long id) {
        return vehicleRepository.findByIdAndDeletedFalse(id)
                .map(vehicleMapper::toResponseDto)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateVehicle(Long id, VehicleDTO vehicleDTO) {
        Vehicle existingVehicle = vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));

        if (vehicleDTO.projectAreaId() != null) {
            validateProjectAreaExists(vehicleDTO.projectAreaId());
        }

        if (vehicleDTO.vehicleTypeId() != null) {
            validateVehicleTypeExists(vehicleDTO.vehicleTypeId());
        }

        validateUniqueFieldsForUpdate(vehicleDTO, existingVehicle);

        try {
            vehicleMapper.partialUpdate(vehicleDTO, existingVehicle);
            Vehicle updatedVehicle = vehicleRepository.save(existingVehicle);
            return vehicleMapper.toResponseDto(updatedVehicle);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, vehicleDTO);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));

        vehicle.setDeleted(true);
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO activateVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        vehicle.setActive(true);
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponseDTO deactivateVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        vehicle.setActive(false);
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return vehicleRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Vehicle getEntityById(Long id) {
        return vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    private void validateVehicleTypeRequired(Long vehicleTypeId) {
        if (vehicleTypeId == null) {
            throw new VehicleNotValidException(messageSourceHelper.getMessage("vehicle.vehicleType.required"));
        }
    }

    private void validateNewVehicle(VehicleDTO vehicleDTO) {
        if (vehicleRepository.existsByLicensePlateAndDeletedFalse(vehicleDTO.licensePlate())) {
            throw new VehicleAlreadyExistsException(messageSourceHelper.getMessage("vehicle.licensePlate.alreadyExists", vehicleDTO.licensePlate()));
        }
    }

    private void validateProjectAreaExists(Long projectAreaId) {
        if (projectAreaId != null && !projectAreaRepository.existsById(projectAreaId)) {
            throw new ProjectAreaNotValidException(projectAreaId);
        }
    }

    private void validateVehicleTypeExists(Long vehicleTypeId) {
        if (vehicleTypeId != null && !vehicleTypeRepository.existsById(vehicleTypeId)) {
            throw new VehicleTypeNotFoundException(vehicleTypeId);
        }
    }

    private Optional<Vehicle> findDeletedVehicle(VehicleDTO vehicleDTO) {
        return vehicleRepository.findByLicensePlateAndDeletedTrue(vehicleDTO.licensePlate());
    }

    private VehicleResponseDTO reactivateVehicle(Vehicle vehicle, VehicleDTO vehicleDTO) {
        vehicleMapper.partialUpdate(vehicleDTO, vehicle);
        vehicle.setDeleted(false);
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    private VehicleResponseDTO createNewVehicle(VehicleDTO vehicleDTO) {
        Vehicle vehicle = vehicleMapper.toEntity(vehicleDTO);
        vehicle.setDeleted(false);
        vehicle.setActive(true);
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    private void validateUniqueFieldsForUpdate(VehicleDTO vehicleDTO, Vehicle existingVehicle) {
        // Validar licensePlate si está siendo actualizado
        if (vehicleDTO.licensePlate() != null && !vehicleDTO.licensePlate().equals(existingVehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlateAndDeletedFalse(vehicleDTO.licensePlate())) {
                throw new VehicleAlreadyExistsException(messageSourceHelper.getMessage("vehicle.licensePlate.exists", vehicleDTO.licensePlate()));
            }
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, VehicleDTO vehicleDTO) {
        String errorMessage = e.getMessage().toLowerCase();

        // Detectar violación de constraint de licensePlate
        if (errorMessage.contains("license_plate") || errorMessage.contains("uk_") && errorMessage.contains("license")) {
            throw new VehicleDataConflictException(
                messageSourceHelper.getMessage("vehicle.update.conflict.licensePlate", vehicleDTO.licensePlate()),
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new VehicleDataConflictException(
            messageSourceHelper.getMessage("vehicle.update.conflict.generic"),
            e
        );
    }
}
