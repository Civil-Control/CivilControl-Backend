package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.EmployeeVacationDTO;
import PSG.backEnd.model.dto.employee.EmployeeVacationFilterDTO;
import PSG.backEnd.model.dto.employee.EmployeeVacationResponseDTO;
import PSG.backEnd.model.entity.employee.EmployeeVacation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IEmployeeVacationService {
    EmployeeVacationResponseDTO createEmployeeVacation(EmployeeVacationDTO employeeVacationDTO);
    EmployeeVacationResponseDTO getEmployeeVacationById(Long id);
    EmployeeVacationResponseDTO updateEmployeeVacation(Long id, EmployeeVacationDTO employeeVacationDTO);
    void deleteEmployeeVacation(Long id);
    Page<EmployeeVacationResponseDTO> getAllEmployeeVacations(EmployeeVacationFilterDTO filterDTO, Pageable pageable);
    EmployeeVacation getEntityById(Long id);
    boolean existsById(Long id);
}

