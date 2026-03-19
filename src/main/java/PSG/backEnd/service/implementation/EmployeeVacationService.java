package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.employeeVacation.EmployeeVacationNotFoundException;
import PSG.backEnd.exception.employeeVacation.EmployeeVacationNotValidException;
import PSG.backEnd.model.dto.employee.EmployeeVacationDTO;
import PSG.backEnd.model.dto.employee.EmployeeVacationFilterDTO;
import PSG.backEnd.model.dto.employee.EmployeeVacationResponseDTO;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.employee.EmployeeVacation;
import PSG.backEnd.model.mapper.EmployeeVacationMapper;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.EmployeeVacationRepository;
import PSG.backEnd.service.port.IEmployeeVacationService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class EmployeeVacationService implements IEmployeeVacationService {

    private final EmployeeVacationRepository employeeVacationRepository;
    private final EmployeeVacationMapper employeeVacationMapper;
    private final EmployeeRepository employeeRepository;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public EmployeeVacationResponseDTO createEmployeeVacation(EmployeeVacationDTO employeeVacationDTO) {
        validateEmployeeExists(employeeVacationDTO.employeeId());
        validateBusinessRules(employeeVacationDTO, null);

        return createNewEmployeeVacation(employeeVacationDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeVacationResponseDTO> getAllEmployeeVacations(EmployeeVacationFilterDTO filterDTO, Pageable pageable) {
        return employeeVacationRepository.findAllWithFilters(
                filterDTO.employeeId(),
                filterDTO.employeeLastName(),
                filterDTO.startDateFrom(),
                filterDTO.startDateTo(),
                filterDTO.endDateFrom(),
                filterDTO.endDateTo(),
                filterDTO.minTotalDays(),
                filterDTO.maxTotalDays(),
                pageable
        ).map(employeeVacationMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeVacationResponseDTO getEmployeeVacationById(Long id) {
        return employeeVacationRepository.findByIdAndDeletedFalse(id)
                .map(employeeVacationMapper::toResponseDto)
                .orElseThrow(() -> new EmployeeVacationNotFoundException(id));
    }

    @Override
    @Transactional
    public EmployeeVacationResponseDTO updateEmployeeVacation(Long id, EmployeeVacationDTO employeeVacationDTO) {
        EmployeeVacation existingVacation = employeeVacationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EmployeeVacationNotFoundException(id));

        if (employeeVacationDTO.employeeId() != null) {
            validateEmployeeExists(employeeVacationDTO.employeeId());
        }

        validateBusinessRulesForUpdate(employeeVacationDTO, existingVacation);

        employeeVacationMapper.partialUpdate(employeeVacationDTO, existingVacation);

        // Set employee if changed
        if (employeeVacationDTO.employeeId() != null &&
            !employeeVacationDTO.employeeId().equals(existingVacation.getEmployee().getId())) {
            Employee employee = employeeRepository.findByIdAndDeletedFalse(employeeVacationDTO.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(employeeVacationDTO.employeeId()));
            existingVacation.setEmployee(employee);
        }

        EmployeeVacation updatedVacation = employeeVacationRepository.save(existingVacation);
        return employeeVacationMapper.toResponseDto(updatedVacation);
    }

    @Override
    @Transactional
    public void deleteEmployeeVacation(Long id) {
        EmployeeVacation vacation = employeeVacationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EmployeeVacationNotFoundException(id));

        vacation.setDeleted(true);
        employeeVacationRepository.save(vacation);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeVacation getEntityById(Long id) {
        return employeeVacationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EmployeeVacationNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return employeeVacationRepository.findByIdAndDeletedFalse(id).isPresent();
    }

    private void validateEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsByIdAndDeletedFalse(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }

    private void validateBusinessRules(EmployeeVacationDTO employeeVacationDTO, Long excludeVacationId) {
        // Validar que la fecha de inicio no sea anterior a hace más de 2 años
        LocalDate twoYearsAgo = LocalDate.now().minusYears(2);
        if (employeeVacationDTO.startDate().isBefore(twoYearsAgo)) {
            throw new EmployeeVacationNotValidException(messageSourceHelper.getMessage("employeeVacation.startDate.tooOld"));
        }

        // Validar que la fecha de fin no sea anterior a la fecha de inicio
        if (employeeVacationDTO.endDate().isBefore(employeeVacationDTO.startDate())) {
            throw new EmployeeVacationNotValidException(messageSourceHelper.getMessage("employeeVacation.endDate.beforeStartDate"));
        }

        // Calcular días entre las fechas
        long daysBetween = ChronoUnit.DAYS.between(employeeVacationDTO.startDate(), employeeVacationDTO.endDate()) + 1;

        // Validar que totalDays no sea mayor que los días entre las fechas
        if (employeeVacationDTO.totalDays() > daysBetween) {
            throw new EmployeeVacationNotValidException(
                    messageSourceHelper.getMessage("employeeVacation.totalDays.exceedsPeriod",
                            employeeVacationDTO.totalDays(), daysBetween)
            );
        }

        // Validar que totalDays no sea menor a 1
        if (employeeVacationDTO.totalDays() < 1) {
            throw new EmployeeVacationNotValidException(messageSourceHelper.getMessage("employeeVacation.totalDays.minimum"));
        }

        // Validar que no existan vacaciones superpuestas para el mismo empleado
        boolean hasOverlap = employeeVacationRepository.existsOverlappingVacation(
                employeeVacationDTO.employeeId(),
                employeeVacationDTO.startDate(),
                employeeVacationDTO.endDate(),
                excludeVacationId
        );

        if (hasOverlap) {
            throw new EmployeeVacationNotValidException(messageSourceHelper.getMessage("employeeVacation.overlap"));
        }

        // Validar que la fecha de fin no sea demasiado lejana (máximo 2 años en el futuro)
        LocalDate twoYearsFromNow = LocalDate.now().plusYears(2);
        if (employeeVacationDTO.endDate().isAfter(twoYearsFromNow)) {
            throw new EmployeeVacationNotValidException(messageSourceHelper.getMessage("employeeVacation.endDate.tooFuture"));
        }
    }

    private void validateBusinessRulesForUpdate(EmployeeVacationDTO employeeVacationDTO, EmployeeVacation existingVacation) {
        LocalDate startDate = employeeVacationDTO.startDate() != null ?
                employeeVacationDTO.startDate() : existingVacation.getStartDate();
        LocalDate endDate = employeeVacationDTO.endDate() != null ?
                employeeVacationDTO.endDate() : existingVacation.getEndDate();
        Integer totalDays = employeeVacationDTO.totalDays() != null ?
                employeeVacationDTO.totalDays() : existingVacation.getTotalDays();
        Long employeeId = employeeVacationDTO.employeeId() != null ?
                employeeVacationDTO.employeeId() : existingVacation.getEmployee().getId();

        // Crear DTO temporal con todos los valores para validar
        EmployeeVacationDTO tempDTO = new EmployeeVacationDTO(
                employeeId,
                startDate,
                endDate,
                totalDays,
                employeeVacationDTO.observations()
        );

        validateBusinessRules(tempDTO, existingVacation.getId());
    }

    private EmployeeVacationResponseDTO createNewEmployeeVacation(EmployeeVacationDTO employeeVacationDTO) {
        EmployeeVacation vacation = employeeVacationMapper.toEntity(employeeVacationDTO);

        Employee employee = employeeRepository.findByIdAndDeletedFalse(employeeVacationDTO.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(employeeVacationDTO.employeeId()));

        vacation.setEmployee(employee);
        vacation.setDeleted(false);
        return employeeVacationMapper.toResponseDto(employeeVacationRepository.save(vacation));
    }
}

