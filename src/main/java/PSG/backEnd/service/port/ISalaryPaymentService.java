package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.SalaryPaymentBatchDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentFilterDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ISalaryPaymentService {
    SalaryPaymentResponseDTO createSalaryPayment(SalaryPaymentDTO salaryPaymentDTO);
    List<SalaryPaymentResponseDTO> createBatchSalaryPayments(SalaryPaymentBatchDTO batchDTO);
    SalaryPaymentResponseDTO getSalaryPaymentById(Long id);
    SalaryPaymentResponseDTO updateSalaryPayment(Long id, SalaryPaymentDTO salaryPaymentDTO);
    void deleteSalaryPayment(Long id);
    Page<SalaryPaymentResponseDTO> getAllSalaryPayments(SalaryPaymentFilterDTO filterDTO, Pageable pageable);
    int countExistingPayments(Long employeeId, SalaryFrecuency frequency, LocalDate paymentDate, Long excludePaymentId);
}

