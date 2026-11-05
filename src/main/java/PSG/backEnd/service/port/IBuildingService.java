package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.building.BuildingDTO;
import PSG.backEnd.model.dto.building.BuildingFilterDTO;
import PSG.backEnd.model.dto.building.BuildingResponseDTO;
import PSG.backEnd.model.entity.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IBuildingService {
    BuildingResponseDTO createBuilding(BuildingDTO buildingDTO);
    BuildingResponseDTO getBuildingById(Long id);
    BuildingResponseDTO updateBuilding(Long id, BuildingDTO buildingDTO);
    void deleteBuilding(Long id);
    Page<BuildingResponseDTO> getAllBuildings(BuildingFilterDTO filterDTO, Pageable pageable);
    Building getEntityById(Long id);
    boolean existsById(Long id);
}

