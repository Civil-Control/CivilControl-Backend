package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException;
import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaTaskResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.ProjectAreaTask;
import PSG.backEnd.model.mapper.ProjectAreaTaskMapper;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.ProjectAreaTaskRepository;
import PSG.backEnd.service.port.IProjectAreaTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectAreaTaskService implements IProjectAreaTaskService {

    private final ProjectAreaTaskRepository taskRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaTaskMapper taskMapper;

    @Override
    @Transactional
    public ProjectAreaTaskResponseDTO createTask(ProjectAreaTaskDTO dto) {
        ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(dto.projectAreaId())
                .orElseThrow(() -> new ProjectAreaNotFoundException(dto.projectAreaId()));

        if (taskRepository.existsByProjectAreaIdAndNameAndDeletedFalse(dto.projectAreaId(), dto.name())) {
            throw new IllegalArgumentException("Ya existe una sub-tarea con ese nombre en este sector");
        }

        ProjectAreaTask task = taskMapper.toEntity(dto);
        task.setProjectArea(projectArea);
        task.setDeleted(false);

        ProjectAreaTask saved = taskRepository.save(task);
        return taskMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public ProjectAreaTaskResponseDTO updateTask(Long id, ProjectAreaTaskDTO dto) {
        ProjectAreaTask task = taskRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub-tarea no encontrada con id: " + id));

        if (dto.name() != null && !dto.name().equals(task.getName())) {
            Long areaId = dto.projectAreaId() != null ? dto.projectAreaId() : task.getProjectArea().getId();
            if (taskRepository.existsByProjectAreaIdAndNameAndDeletedFalse(areaId, dto.name())) {
                throw new IllegalArgumentException("Ya existe una sub-tarea con ese nombre en este sector");
            }
        }

        taskMapper.partialUpdate(dto, task);

        if (dto.projectAreaId() != null && !dto.projectAreaId().equals(task.getProjectArea().getId())) {
            ProjectArea newArea = projectAreaRepository.findByIdAndDeletedFalse(dto.projectAreaId())
                    .orElseThrow(() -> new ProjectAreaNotFoundException(dto.projectAreaId()));
            task.setProjectArea(newArea);
        }

        ProjectAreaTask updated = taskRepository.save(task);
        return taskMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        ProjectAreaTask task = taskRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub-tarea no encontrada con id: " + id));
        task.setDeleted(true);
        taskRepository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAreaTaskResponseDTO getTaskById(Long id) {
        return taskRepository.findByIdAndDeletedFalse(id)
                .map(taskMapper::toResponseDto)
                .orElseThrow(() -> new IllegalArgumentException("Sub-tarea no encontrada con id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectAreaTaskResponseDTO> getTasksByProjectArea(Long projectAreaId) {
        return taskRepository.findByProjectAreaIdAndDeletedFalseOrderByNameAsc(projectAreaId)
                .stream()
                .map(taskMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAreaTask getEntityById(Long id) {
        return taskRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub-tarea no encontrada con id: " + id));
    }
}
