package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.building.BuildingAlreadyExistsException;
import PSG.backEnd.exception.building.BuildingNotFoundException;
import PSG.backEnd.exception.building.BuildingNotValidException;
import PSG.backEnd.model.dto.building.BuildingDTO;
import PSG.backEnd.model.dto.building.BuildingFilterDTO;
import PSG.backEnd.model.dto.building.BuildingResponseDTO;
import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.mapper.BuildingMapper;
import PSG.backEnd.repository.BuildingRepository;
import PSG.backEnd.service.port.IBuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuildingService implements IBuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;

    @Override
    @Transactional
    public BuildingResponseDTO createBuilding(BuildingDTO buildingDTO) {
        validateNewBuilding(buildingDTO);
        Optional<Building> deletedBuilding = findDeletedBuilding(buildingDTO);

        if (deletedBuilding.isPresent()) {
            return reactivateBuilding(deletedBuilding.get(), buildingDTO);
        }

        return createNewBuilding(buildingDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BuildingResponseDTO> getAllBuildings(BuildingFilterDTO filterDTO, Pageable pageable) {
        return buildingRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.code(),
                filterDTO.buildingType(),
                filterDTO.active(),
                pageable
        ).map(buildingMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public BuildingResponseDTO getBuildingById(Long id) {
        return buildingRepository.findByIdAndDeletedFalse(id)
                .map(buildingMapper::toResponseDto)
                .orElseThrow(() -> new BuildingNotFoundException(id));
    }

    @Override
    @Transactional
    public BuildingResponseDTO updateBuilding(Long id, BuildingDTO buildingDTO) {
        Building existingBuilding = buildingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BuildingNotFoundException(id));

        validateBuildingUpdate(id, buildingDTO);
        buildingMapper.partialUpdate(buildingDTO, existingBuilding);
        Building updatedBuilding = buildingRepository.save(existingBuilding);
        return buildingMapper.toResponseDto(updatedBuilding);
    }

    @Override
    @Transactional
    public void deleteBuilding(Long id) {
        Building building = buildingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BuildingNotFoundException(id));

        // Validate that building can be deleted (e.g., no active stock items)
        if (building.getStockItems() != null && !building.getStockItems().isEmpty()) {
            long activeStockCount = building.getStockItems().stream()
                    .filter(stock -> !stock.getDeleted())
                    .count();
            if (activeStockCount > 0) {
                throw new BuildingNotValidException(
                        "Cannot delete building with active stock items. Please reassign or delete stock items first.");
            }
        }

        building.setDeleted(true);
        building.setActive(false);
        buildingRepository.save(building);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return buildingRepository.existsByIdAndDeletedFalse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Building getEntityById(Long id) {
        return buildingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BuildingNotFoundException(id));
    }

    // Private validation methods
    private void validateNewBuilding(BuildingDTO buildingDTO) {
        if (buildingDTO.name() == null || buildingDTO.name().trim().isEmpty()) {
            throw new BuildingNotValidException("Building name cannot be empty");
        }
        if (buildingDTO.code() == null || buildingDTO.code().trim().isEmpty()) {
            throw new BuildingNotValidException("Building code cannot be empty");
        }
        if (buildingRepository.existsByCodeAndDeletedFalse(buildingDTO.code())) {
            throw new BuildingAlreadyExistsException(
                    "There is already an active building with the code: " + buildingDTO.code());
        }
        if (buildingDTO.address() == null) {
            throw new BuildingNotValidException("Building address cannot be null");
        }
        if (buildingDTO.buildingType() == null) {
            throw new BuildingNotValidException("Building type cannot be null");
        }
        validateAddress(buildingDTO);
    }

    private void validateBuildingUpdate(Long id, BuildingDTO buildingDTO) {
        // Validate code uniqueness if code is being updated
        if (buildingDTO.code() != null && !buildingDTO.code().trim().isEmpty()) {
            // Check if code exists in ANY building (active or deleted) to prevent DB constraint violation
            buildingRepository.findByCode(buildingDTO.code())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            // If the existing building with same code is deleted, provide specific error message
                            if (existing.getDeleted()) {
                                throw new BuildingAlreadyExistsException(
                                        "Cannot use code '" + buildingDTO.code() +
                                        "' because it belongs to a deleted building (ID: " + existing.getId() +
                                        "). Please use a different code or permanently remove the deleted building.");
                            } else {
                                throw new BuildingAlreadyExistsException(
                                        "There is already another active building with the code: " + buildingDTO.code());
                            }
                        }
                    });
        }
        // Validate name is not empty if provided
        if (buildingDTO.name() != null && buildingDTO.name().trim().isEmpty()) {
            throw new BuildingNotValidException("Building name cannot be empty");
        }
        // Validate address if provided
        if (buildingDTO.address() != null) {
            validateAddress(buildingDTO);
        }
    }

    private void validateAddress(BuildingDTO buildingDTO) {
        if (buildingDTO.address() == null) {
            return;
        }
        if (buildingDTO.address().street() == null || buildingDTO.address().street().trim().isEmpty()) {
            throw new BuildingNotValidException("Street cannot be empty");
        }
        if (buildingDTO.address().number() == null || buildingDTO.address().number() <= 0) {
            throw new BuildingNotValidException("Street number must be positive");
        }
        if (buildingDTO.address().city() == null || buildingDTO.address().city().trim().isEmpty()) {
            throw new BuildingNotValidException("City cannot be empty");
        }
        if (buildingDTO.address().state() == null || buildingDTO.address().state().trim().isEmpty()) {
            throw new BuildingNotValidException("State/Province cannot be empty");
        }
        if (buildingDTO.address().country() == null || buildingDTO.address().country().trim().isEmpty()) {
            throw new BuildingNotValidException("Country cannot be empty");
        }
        if (buildingDTO.address().zipCode() == null || buildingDTO.address().zipCode().trim().isEmpty()) {
            throw new BuildingNotValidException("Zip code cannot be empty");
        }
    }

    private Optional<Building> findDeletedBuilding(BuildingDTO buildingDTO) {
        return buildingRepository.findByCodeAndDeletedTrue(buildingDTO.code());
    }

    private BuildingResponseDTO reactivateBuilding(Building building, BuildingDTO buildingDTO) {
        buildingMapper.partialUpdate(buildingDTO, building);
        building.setDeleted(false);
        building.setActive(buildingDTO.active() != null ? buildingDTO.active() : true);
        return buildingMapper.toResponseDto(buildingRepository.save(building));
    }

    private BuildingResponseDTO createNewBuilding(BuildingDTO buildingDTO) {
        Building building = buildingMapper.toEntity(buildingDTO);
        building.setDeleted(false);
        // The mapper sets active with default value
        return buildingMapper.toResponseDto(buildingRepository.save(building));
    }
}

