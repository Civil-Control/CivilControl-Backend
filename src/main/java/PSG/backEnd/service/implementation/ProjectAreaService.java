package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.projectarea.ProjectAreaAlreadyExistsException;
import PSG.backEnd.exception.projectarea.ProjectAreaDataConflictException;
import PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException;
import PSG.backEnd.model.dto.projectArea.ProjectAreaDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaFilterDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.mapper.ProjectAreaMapper;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.service.port.IProjectAreaService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectAreaService implements IProjectAreaService {

    private final ProjectAreaRepository projectAreaRepository;
    private final ProjectAreaMapper projectAreaMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public ProjectAreaResponseDTO createProjectArea(ProjectAreaDTO projectAreaDTO) {
        validateNewProjectArea(projectAreaDTO);

        Optional<ProjectArea> deletedProjectArea = findDeletedProjectArea(projectAreaDTO);

        if (deletedProjectArea.isPresent()) {
            return reactivateProjectArea(deletedProjectArea.get(), projectAreaDTO);
        }

        return createNewProjectArea(projectAreaDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectAreaResponseDTO> getAllProjectAreas(ProjectAreaFilterDTO filterDTO, Pageable pageable) {
        return projectAreaRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.active(),
                pageable)
                .map(projectAreaMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAreaResponseDTO getProjectAreaById(Long id) {
        return projectAreaRepository.findByIdAndDeletedFalse(id)
                .map(projectAreaMapper::toResponseDto)
                .orElseThrow(() -> new ProjectAreaNotFoundException(id));
    }

    @Override
    @Transactional
    public ProjectAreaResponseDTO updateProjectArea(Long id, ProjectAreaDTO projectAreaDTO) {
        ProjectArea existingProjectArea = getEntityById(id);

        validateUniqueFieldsForUpdate(projectAreaDTO, existingProjectArea);

        try {
            projectAreaMapper.partialUpdate(projectAreaDTO, existingProjectArea);
            ProjectArea updatedProjectArea = projectAreaRepository.save(existingProjectArea);
            return projectAreaMapper.toResponseDto(updatedProjectArea);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, projectAreaDTO);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteProjectArea(Long id) {
        ProjectArea projectArea = getEntityById(id);

        projectArea.setDeleted(true);
        projectAreaRepository.save(projectArea);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectArea getEntityById(Long id) {
        return projectAreaRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProjectAreaNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return projectAreaRepository.existsById(id);
    }

    private void validateNewProjectArea(ProjectAreaDTO projectAreaDTO) {
        if (projectAreaRepository.existsByNameAndDeletedFalse(projectAreaDTO.name())) {
            throw new ProjectAreaAlreadyExistsException(projectAreaDTO.name());
        }
    }

    private Optional<ProjectArea> findDeletedProjectArea(ProjectAreaDTO projectAreaDTO) {
        return projectAreaRepository.findByNameAndDeletedTrue(projectAreaDTO.name());
    }

    private ProjectAreaResponseDTO createNewProjectArea(ProjectAreaDTO projectAreaDTO) {
        ProjectArea projectArea = projectAreaMapper.toEntity(projectAreaDTO);
        ProjectArea savedProjectArea = projectAreaRepository.save(projectArea);
        return projectAreaMapper.toResponseDto(savedProjectArea);
    }

    private ProjectAreaResponseDTO reactivateProjectArea(ProjectArea deletedProjectArea, ProjectAreaDTO projectAreaDTO) {
        deletedProjectArea.setDeleted(false);
        deletedProjectArea.setActive(projectAreaDTO.active() != null ? projectAreaDTO.active() : true);
        deletedProjectArea.setDescription(projectAreaDTO.description());
        deletedProjectArea.setColor(projectAreaDTO.color());

        ProjectArea reactivatedProjectArea = projectAreaRepository.save(deletedProjectArea);
        return projectAreaMapper.toResponseDto(reactivatedProjectArea);
    }

    private void validateUniqueFieldsForUpdate(ProjectAreaDTO projectAreaDTO, ProjectArea existingProjectArea) {
        // Validar name si está siendo actualizado
        if (projectAreaDTO.name() != null && !projectAreaDTO.name().equals(existingProjectArea.getName())) {
            if (projectAreaRepository.existsByNameAndDeletedFalse(projectAreaDTO.name())) {
                throw new ProjectAreaAlreadyExistsException(messageSourceHelper.getMessage("projectArea.name.exists", projectAreaDTO.name()));
            }
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, ProjectAreaDTO projectAreaDTO) {
        String errorMessage = e.getMessage().toLowerCase();

        // Detectar violación de constraint de name
        if (errorMessage.contains("name") || errorMessage.contains("uk_") && errorMessage.contains("name")) {
            throw new ProjectAreaDataConflictException(
                messageSourceHelper.getMessage("projectArea.update.conflict.name", projectAreaDTO.name()),
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new ProjectAreaDataConflictException(
            messageSourceHelper.getMessage("projectArea.update.conflict.generic"),
            e
        );
    }
}
