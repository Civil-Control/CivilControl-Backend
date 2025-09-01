package PSG.backEnd.controller;

import PSG.backEnd.model.dto.projectArea.ProjectAreaDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaFilterDTO;
import PSG.backEnd.model.dto.projectArea.ProjectAreaResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IProjectAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/project-areas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectAreaController {

    private final IProjectAreaService iProjectAreaService;

    @PostMapping
    public ResponseEntity<ProjectAreaResponseDTO> createProjectArea(
            @Validated(OnCreate.class) @RequestBody ProjectAreaDTO projectAreaDTO) {
        ProjectAreaResponseDTO createdProjectArea = iProjectAreaService.createProjectArea(projectAreaDTO);
        return new ResponseEntity<>(createdProjectArea, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ProjectAreaResponseDTO>> getProjectAreas(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        ProjectAreaFilterDTO filterDTO = new ProjectAreaFilterDTO(name, active);

        return ResponseEntity.ok(iProjectAreaService.getAllProjectAreas(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectAreaResponseDTO> getProjectAreaById(@PathVariable Long id) {
        return ResponseEntity.ok(iProjectAreaService.getProjectAreaById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectAreaResponseDTO> updateProjectArea(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody ProjectAreaDTO projectAreaDTO) {
        return ResponseEntity.ok(iProjectAreaService.updateProjectArea(id, projectAreaDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProjectArea(@PathVariable Long id) {
        iProjectAreaService.deleteProjectArea(id);
        return ResponseEntity.noContent().build();
    }
}
