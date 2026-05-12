package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.disciplinaryAction.DisciplinaryActionNotFoundException;
import PSG.backEnd.exception.disciplinaryAction.DisciplinaryActionNotValidException;
import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.laborIncident.LaborIncidentNotFoundException;
import PSG.backEnd.model.entity.employee.LaborIncident;
import PSG.backEnd.repository.LaborIncidentRepository;
import PSG.backEnd.model.dto.employee.DisciplinaryActionDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionFilterDTO;
import PSG.backEnd.model.dto.employee.DisciplinaryActionResponseDTO;
import PSG.backEnd.model.entity.employee.DisciplinaryAction;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.enums.employee.ActionType;
import PSG.backEnd.model.mapper.DisciplinaryActionMapper;
import PSG.backEnd.repository.DisciplinaryActionRepository;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.service.port.IDisciplinaryActionService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DisciplinaryActionService implements IDisciplinaryActionService {

    private final DisciplinaryActionRepository disciplinaryActionRepository;
    private final EmployeeRepository employeeRepository;
    private final LaborIncidentRepository laborIncidentRepository;
    private final DisciplinaryActionMapper disciplinaryActionMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public DisciplinaryActionResponseDTO createDisciplinaryAction(DisciplinaryActionDTO disciplinaryActionDTO) {
        validateEmployeeExists(disciplinaryActionDTO.employeeId());
        validateBusinessRules(disciplinaryActionDTO);

        Employee employee = employeeRepository.findByIdAndDeletedFalse(disciplinaryActionDTO.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(disciplinaryActionDTO.employeeId()));

        DisciplinaryAction disciplinaryAction = disciplinaryActionMapper.toEntity(disciplinaryActionDTO);
        disciplinaryAction.setEmployee(employee);

        if (disciplinaryActionDTO.laborIncidentId() != null) {
            LaborIncident incident = laborIncidentRepository.findById(disciplinaryActionDTO.laborIncidentId())
                    .orElseThrow(() -> new LaborIncidentNotFoundException(disciplinaryActionDTO.laborIncidentId()));
            disciplinaryAction.setLaborIncident(incident);
        }

        DisciplinaryAction savedDisciplinaryAction = disciplinaryActionRepository.save(disciplinaryAction);
        return disciplinaryActionMapper.toResponseDto(savedDisciplinaryAction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DisciplinaryActionResponseDTO> getAllDisciplinaryActions(DisciplinaryActionFilterDTO filterDTO, Pageable pageable) {
        return disciplinaryActionRepository.findAllWithFilters(
                filterDTO.employeeId(),
                filterDTO.employeeSearch(),
                filterDTO.actionType(),
                filterDTO.actionDateFrom(),
                filterDTO.actionDateTo(),
                filterDTO.endDateFrom(),
                filterDTO.endDateTo(),
                pageable
        ).map(disciplinaryActionMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public DisciplinaryActionResponseDTO getDisciplinaryActionById(Long id) {
        return disciplinaryActionRepository.findById(id)
                .map(disciplinaryActionMapper::toResponseDto)
                .orElseThrow(() -> new DisciplinaryActionNotFoundException(id));
    }

    @Override
    @Transactional
    public DisciplinaryActionResponseDTO updateDisciplinaryAction(Long id, DisciplinaryActionDTO disciplinaryActionDTO) {
        DisciplinaryAction existingDisciplinaryAction = disciplinaryActionRepository.findById(id)
                .orElseThrow(() -> new DisciplinaryActionNotFoundException(id));

        if (disciplinaryActionDTO.employeeId() != null) {
            validateEmployeeExists(disciplinaryActionDTO.employeeId());
        }

        validateBusinessRulesForUpdate(disciplinaryActionDTO, existingDisciplinaryAction);

        if (disciplinaryActionDTO.employeeId() != null &&
            !existingDisciplinaryAction.getEmployee().getId().equals(disciplinaryActionDTO.employeeId())) {
            Employee employee = employeeRepository.findByIdAndDeletedFalse(disciplinaryActionDTO.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(disciplinaryActionDTO.employeeId()));
            existingDisciplinaryAction.setEmployee(employee);
        }

        if (disciplinaryActionDTO.laborIncidentId() != null) {
            LaborIncident incident = laborIncidentRepository.findById(disciplinaryActionDTO.laborIncidentId())
                    .orElseThrow(() -> new LaborIncidentNotFoundException(disciplinaryActionDTO.laborIncidentId()));
            existingDisciplinaryAction.setLaborIncident(incident);
        }

        disciplinaryActionMapper.partialUpdate(disciplinaryActionDTO, existingDisciplinaryAction);
        DisciplinaryAction updatedDisciplinaryAction = disciplinaryActionRepository.save(existingDisciplinaryAction);
        return disciplinaryActionMapper.toResponseDto(updatedDisciplinaryAction);
    }

    @Override
    @Transactional
    public void deleteDisciplinaryAction(Long id) {
        DisciplinaryAction disciplinaryAction = disciplinaryActionRepository.findById(id)
                .orElseThrow(() -> new DisciplinaryActionNotFoundException(id));

        disciplinaryActionRepository.delete(disciplinaryAction);
    }

    private void validateEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsByIdAndDeletedFalse(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }

    private void validateBusinessRules(DisciplinaryActionDTO disciplinaryActionDTO) {
        // Validar que la fecha de acción no sea futura
        if (disciplinaryActionDTO.actionDate().isAfter(LocalDate.now())) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.actionDate.future"));
        }

        // Validar que si hay endDate, debe ser una suspensión
        if (disciplinaryActionDTO.endDate() != null) {
            if (disciplinaryActionDTO.actionType() != ActionType.SUSPENSION) {
                throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.onlySuspension"));
            }

            // Validar que endDate sea posterior a actionDate
            if (disciplinaryActionDTO.endDate().isBefore(disciplinaryActionDTO.actionDate())) {
                throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.afterActionDate"));
            }

            // Validar que endDate no sea más de 1 año en el futuro
            LocalDate maxEndDate = disciplinaryActionDTO.actionDate().plusYears(1);
            if (disciplinaryActionDTO.endDate().isAfter(maxEndDate)) {
                throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.maxDuration"));
            }
        }

        // Validar que las suspensiones tengan endDate
        if (disciplinaryActionDTO.actionType() == ActionType.SUSPENSION && disciplinaryActionDTO.endDate() == null) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.required"));
        }

        // Validar que el motivo tenga un mínimo de caracteres significativos
        if (disciplinaryActionDTO.reason() != null && disciplinaryActionDTO.reason().trim().length() < 1) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.reason.minLength"));
        }
    }

    private void validateBusinessRulesForUpdate(DisciplinaryActionDTO disciplinaryActionDTO, DisciplinaryAction existingAction) {
        // Validar fecha de acción si se está actualizando
        if (disciplinaryActionDTO.actionDate() != null && disciplinaryActionDTO.actionDate().isAfter(LocalDate.now())) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.actionDate.future"));
        }

        // Obtener los valores actuales o los nuevos
        ActionType actionType = disciplinaryActionDTO.actionType() != null ?
                disciplinaryActionDTO.actionType() : existingAction.getActionType();

        LocalDate actionDate = disciplinaryActionDTO.actionDate() != null ?
                disciplinaryActionDTO.actionDate() : existingAction.getActionDate();

        LocalDate endDate = disciplinaryActionDTO.endDate() != null ?
                disciplinaryActionDTO.endDate() : existingAction.getEndDate();

        // Validar que si hay endDate, debe ser una suspensión
        if (endDate != null && actionType != ActionType.SUSPENSION) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.onlySuspension"));
        }

        // Validar que endDate sea posterior a actionDate
        if (endDate != null && endDate.isBefore(actionDate)) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.afterActionDate"));
        }

        // Validar que endDate no sea más de 1 año en el futuro
        if (endDate != null) {
            LocalDate maxEndDate = actionDate.plusYears(1);
            if (endDate.isAfter(maxEndDate)) {
                throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.maxDuration"));
            }
        }

        // Validar que las suspensiones tengan endDate
        if (actionType == ActionType.SUSPENSION && endDate == null) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.endDate.required"));
        }

        // Validar longitud del motivo si se está actualizando
        if (disciplinaryActionDTO.reason() != null && disciplinaryActionDTO.reason().trim().length() < 1) {
            throw new DisciplinaryActionNotValidException(messageSourceHelper.getMessage("disciplinaryAction.reason.minLength"));
        }
    }
}

