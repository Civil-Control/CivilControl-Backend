package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.employee.EmployeeAlreadyExistsException;
import PSG.backEnd.exception.employee.EmployeeDataConflictException;
import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.employee.EmployeeNotValidException;
import PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException;
import PSG.backEnd.model.dto.employee.EmployeeDTO;
import PSG.backEnd.model.dto.employee.EmployeeFilterDTO;
import PSG.backEnd.model.dto.employee.EmployeeResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.mapper.EmployeeMapper;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.service.port.IEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService implements IEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final ProjectAreaRepository projectAreaRepository;

    @Override
    @Transactional
    public EmployeeResponseDTO createEmployee(EmployeeDTO employeeDTO) {
        validateNewEmployee(employeeDTO);
        validateProjectAreaExists(employeeDTO.projectAreaId());
        validateBusinessRules(employeeDTO);

        Optional<Employee> deletedEmployee = findDeletedEmployee(employeeDTO);

        if (deletedEmployee.isPresent()) {
            return reactivateEmployee(deletedEmployee.get(), employeeDTO);
        }

        return createNewEmployee(employeeDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> getAllEmployees(EmployeeFilterDTO filterDTO, Pageable pageable) {
        return employeeRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.lastName(),
                filterDTO.dni(),
                filterDTO.cuil(),
                filterDTO.projectAreaId(),
                filterDTO.city(),
                filterDTO.employmentType(),
                filterDTO.employeeStatus(),
                filterDTO.employeeRole(),
                filterDTO.hireDateFrom(),
                filterDTO.hireDateTo(),
                pageable
        ).map(employeeMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployeeById(Long id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .map(employeeMapper::toResponseDto)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Override
    @Transactional
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeDTO employeeDTO) {
        Employee existingEmployee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        if (employeeDTO.projectAreaId() != null) {
            validateProjectAreaExists(employeeDTO.projectAreaId());
        }

        validateBusinessRulesForUpdate(employeeDTO, existingEmployee);
        validateUniqueFieldsForUpdate(employeeDTO, existingEmployee);

        try {
            employeeMapper.partialUpdate(employeeDTO, existingEmployee);
            Employee updatedEmployee = employeeRepository.save(existingEmployee);
            return employeeMapper.toResponseDto(updatedEmployee);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, employeeDTO);
            throw e; // This line won't be reached but is needed for compilation
        }
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));

        employee.setDeleted(true);
        employee.setEndDate(LocalDate.now());
        employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEntityById(Long id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return employeeRepository.existsByIdAndDeletedFalse(id);
    }

    private void validateNewEmployee(EmployeeDTO employeeDTO) {
        if (employeeRepository.existsByDniAndDeletedFalse(employeeDTO.dni())) {
            throw new EmployeeAlreadyExistsException("There is already an active employee with the DNI: " + employeeDTO.dni());
        }
        if (employeeRepository.existsByCuilAndDeletedFalse(employeeDTO.cuil())) {
            throw new EmployeeAlreadyExistsException("There is already an active employee with the CUIL: " + employeeDTO.cuil());
        }
    }

    private void validateProjectAreaExists(Long projectAreaId) {
        if (!projectAreaRepository.existsByIdAndDeletedFalse(projectAreaId)) {
            throw new ProjectAreaNotFoundException(projectAreaId);
        }
    }

    private void validateBusinessRules(EmployeeDTO employeeDTO) {
        if (employeeDTO.hireDate().isAfter(LocalDate.now())) {
            throw new EmployeeNotValidException("Hire date cannot be in the future");
        }

        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        if (employeeDTO.birthDate().isAfter(eighteenYearsAgo)) {
            throw new EmployeeNotValidException("Employee must be at least 18 years old");
        }

        if (employeeDTO.endDate() != null && employeeDTO.endDate().isBefore(employeeDTO.hireDate())) {
            throw new EmployeeNotValidException("End date cannot be before hire date");
        }

        String cuilDigits = employeeDTO.cuil().replaceAll("-", "");
        String dniFromCuil = cuilDigits.substring(2, cuilDigits.length() - 1);
        if (!dniFromCuil.equals(employeeDTO.dni())) {
            throw new EmployeeNotValidException("CUIL and DNI do not match. The DNI in CUIL must match the provided DNI");
        }
    }

    private void validateBusinessRulesForUpdate(EmployeeDTO employeeDTO, Employee existingEmployee) {
        if (employeeDTO.hireDate() != null && employeeDTO.hireDate().isAfter(LocalDate.now())) {
            throw new EmployeeNotValidException("Hire date cannot be in the future");
        }

        if (employeeDTO.birthDate() != null) {
            LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
            if (employeeDTO.birthDate().isAfter(eighteenYearsAgo)) {
                throw new EmployeeNotValidException("Employee must be at least 18 years old");
            }
        }

        if (employeeDTO.endDate() != null) {
            LocalDate hireDate = employeeDTO.hireDate() != null ? employeeDTO.hireDate() : existingEmployee.getHireDate();
            if (employeeDTO.endDate().isBefore(hireDate)) {
                throw new EmployeeNotValidException("End date cannot be before hire date");
            }
        }

        if (employeeDTO.cuil() != null || employeeDTO.dni() != null) {
            String cuil = employeeDTO.cuil() != null ? employeeDTO.cuil() : existingEmployee.getCuil();
            String dni = employeeDTO.dni() != null ? employeeDTO.dni() : existingEmployee.getDni();

            String cuilDigits = cuil.replaceAll("-", "");
            String dniFromCuil = cuilDigits.substring(2, cuilDigits.length() - 1);
            if (!dniFromCuil.equals(dni)) {
                throw new EmployeeNotValidException("CUIL and DNI do not match. The DNI in CUIL must match the provided DNI");
            }
        }
    }

    private Optional<Employee> findDeletedEmployee(EmployeeDTO employeeDTO) {
        Optional<Employee> deletedEmployee = employeeRepository.findByDniAndDeletedTrue(employeeDTO.dni());
        if (deletedEmployee.isEmpty()) {
            deletedEmployee = employeeRepository.findByCuilAndDeletedTrue(employeeDTO.cuil());
        }
        return deletedEmployee;
    }

    private EmployeeResponseDTO reactivateEmployee(Employee employee, EmployeeDTO employeeDTO) {
        ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(employeeDTO.projectAreaId())
                .orElseThrow(() -> new ProjectAreaNotFoundException(employeeDTO.projectAreaId()));

        employee.setProjectArea(projectArea);
        employeeMapper.partialUpdate(employeeDTO, employee);
        employee.setDeleted(false);
        employee.setEndDate(null);
        return employeeMapper.toResponseDto(employeeRepository.save(employee));
    }

    private EmployeeResponseDTO createNewEmployee(EmployeeDTO employeeDTO) {
        Employee employee = employeeMapper.toEntity(employeeDTO);

        ProjectArea projectArea = projectAreaRepository.findByIdAndDeletedFalse(employeeDTO.projectAreaId())
                .orElseThrow(() -> new ProjectAreaNotFoundException(employeeDTO.projectAreaId()));

        employee.setProjectArea(projectArea);
        employee.setDeleted(false);
        return employeeMapper.toResponseDto(employeeRepository.save(employee));
    }

    private void validateUniqueFieldsForUpdate(EmployeeDTO employeeDTO, Employee existingEmployee) {
        // Validar DNI si está siendo actualizado
        if (employeeDTO.dni() != null && !employeeDTO.dni().equals(existingEmployee.getDni())) {
            if (employeeRepository.existsByDniAndDeletedFalse(employeeDTO.dni())) {
                throw new EmployeeAlreadyExistsException("Cannot update employee: There is already an active employee with the DNI: " + employeeDTO.dni());
            }
        }

        // Validar CUIL si está siendo actualizado
        if (employeeDTO.cuil() != null && !employeeDTO.cuil().equals(existingEmployee.getCuil())) {
            if (employeeRepository.existsByCuilAndDeletedFalse(employeeDTO.cuil())) {
                throw new EmployeeAlreadyExistsException("Cannot update employee: There is already an active employee with the CUIL: " + employeeDTO.cuil());
            }
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, EmployeeDTO employeeDTO) {
        String errorMessage = e.getMessage().toLowerCase();

        // Detectar violación de constraint de DNI
        if (errorMessage.contains("dni") || errorMessage.contains("uk_") && errorMessage.contains("dni")) {
            throw new EmployeeDataConflictException(
                "Cannot update employee: DNI '" + employeeDTO.dni() + "' is already in use by another employee",
                e
            );
        }

        // Detectar violación de constraint de CUIL
        if (errorMessage.contains("cuil") || errorMessage.contains("uk_") && errorMessage.contains("cuil")) {
            throw new EmployeeDataConflictException(
                "Cannot update employee: CUIL '" + employeeDTO.cuil() + "' is already in use by another employee",
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new EmployeeDataConflictException(
            "Cannot update employee due to a data integrity violation. Please verify that all unique fields (DNI, CUIL) are not already in use",
            e
        );
    }
}

