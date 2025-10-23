package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.EmployeeDTO;
import PSG.backEnd.model.dto.employee.EmployeeFilterDTO;
import PSG.backEnd.model.dto.employee.EmployeeResponseDTO;
import PSG.backEnd.model.entity.employee.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IEmployeeService {
    EmployeeResponseDTO createEmployee(EmployeeDTO employeeDTO);
    EmployeeResponseDTO getEmployeeById(Long id);
    EmployeeResponseDTO updateEmployee(Long id, EmployeeDTO employeeDTO);
    void deleteEmployee(Long id);
    Page<EmployeeResponseDTO> getAllEmployees(EmployeeFilterDTO filterDTO, Pageable pageable);
    Employee getEntityById(Long id);
    boolean existsById(Long id);
}

