package PSG.backEnd.controller;

import PSG.backEnd.model.dto.employee.DisciplinaryActionDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionFilterDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionResponseDTO;
import PSG.backEnd.model.enums.employee.ActionType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IDisciplinaryActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/disciplinary-actions")
@RequiredArgsConstructor
public class DisciplinaryActionController {

    private final IDisciplinaryActionService iDisciplinaryActionService;

    @PostMapping
    public ResponseEntity<DisciplinaryActionResponseDTO> createDisciplinaryAction(
            @Validated(OnCreate.class) @RequestBody DisciplinaryActionDTO disciplinaryActionDTO) {
        DisciplinaryActionResponseDTO createdAction = iDisciplinaryActionService.createDisciplinaryAction(disciplinaryActionDTO);
        return new ResponseEntity<>(createdAction, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<DisciplinaryActionResponseDTO>> getDisciplinaryActions(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) ActionType actionType,
            @RequestParam(required = false) LocalDate actionDateFrom,
            @RequestParam(required = false) LocalDate actionDateTo,
            @RequestParam(required = false) LocalDate endDateFrom,
            @RequestParam(required = false) LocalDate endDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "actionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        DisciplinaryActionFilterDTO filterDTO = new DisciplinaryActionFilterDTO(
                employeeId, actionType, actionDateFrom, actionDateTo,
                endDateFrom, endDateTo
        );

        return ResponseEntity.ok(iDisciplinaryActionService.getAllDisciplinaryActions(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaryActionResponseDTO> getDisciplinaryActionById(@PathVariable Long id) {
        return ResponseEntity.ok(iDisciplinaryActionService.getDisciplinaryActionById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DisciplinaryActionResponseDTO> updateDisciplinaryAction(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody DisciplinaryActionDTO disciplinaryActionDTO) {
        return ResponseEntity.ok(iDisciplinaryActionService.updateDisciplinaryAction(id, disciplinaryActionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDisciplinaryAction(@PathVariable Long id) {
        iDisciplinaryActionService.deleteDisciplinaryAction(id);
        return ResponseEntity.noContent().build();
    }
}

