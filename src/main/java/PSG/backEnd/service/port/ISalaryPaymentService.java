package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentFilterDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISalaryPaymentService {
    SalaryPaymentResponseDTO createSalaryPayment(SalaryPaymentDTO salaryPaymentDTO);
    SalaryPaymentResponseDTO getSalaryPaymentById(Long id);
    SalaryPaymentResponseDTO updateSalaryPayment(Long id, SalaryPaymentDTO salaryPaymentDTO);
    void deleteSalaryPayment(Long id);
    Page<SalaryPaymentResponseDTO> getAllSalaryPayments(SalaryPaymentFilterDTO filterDTO, Pageable pageable);
}

