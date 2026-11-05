package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.projectArea.ProjectAreaDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaFilterDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IProjectAreaService {
    ProjectAreaResponseDTO createProjectArea(ProjectAreaDTO projectAreaDTO);
    ProjectAreaResponseDTO getProjectAreaById(Long id);
    ProjectAreaResponseDTO updateProjectArea(Long id, ProjectAreaDTO projectAreaDTO);
    void deleteProjectArea(Long id);
    Page<ProjectAreaResponseDTO> getAllProjectAreas(ProjectAreaFilterDTO filterDTO, Pageable pageable);
    ProjectArea getEntityById(Long id);
    boolean existsById(Long id);
}
