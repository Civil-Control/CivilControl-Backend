package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskResponseDTO;
import PSG.backEnd.model.entity.ProjectAreaTask;

import java.util.List;

public interface IProjectAreaTaskService {

    ProjectAreaTaskResponseDTO createTask(ProjectAreaTaskDTO dto);

    ProjectAreaTaskResponseDTO updateTask(Long id, ProjectAreaTaskDTO dto);

    void deleteTask(Long id);

    ProjectAreaTaskResponseDTO getTaskById(Long id);

    List<ProjectAreaTaskResponseDTO> getTasksByProjectArea(Long projectAreaId);

    ProjectAreaTask getEntityById(Long id);
}
