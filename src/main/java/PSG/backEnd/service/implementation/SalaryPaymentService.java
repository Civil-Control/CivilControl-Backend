package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.salaryPayment.DuplicateSalaryPaymentException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotFoundException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotValidException;
import PSG.backEnd.model.dto.employee.SalaryPaymentBatchDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentFilterDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.mapper.SalaryPaymentMapper;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.SalaryPaymentRepository;
import PSG.backEnd.service.port.ISalaryPaymentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryPaymentService implements ISalaryPaymentService {

    private final SalaryPaymentRepository salaryPaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryPaymentMapper salaryPaymentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public SalaryPaymentResponseDTO createSalaryPayment(SalaryPaymentDTO salaryPaymentDTO) {
        validateEmployeeExists(salaryPaymentDTO.employeeId());
        validateDuplicatePayment(salaryPaymentDTO.employeeId(), salaryPaymentDTO.salaryFrequency(),
                                 salaryPaymentDTO.paymentDate(), null);
        validateBusinessRules(salaryPaymentDTO);

        Employee employee = employeeRepository.findByIdAndDeletedFalse(salaryPaymentDTO.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(salaryPaymentDTO.employeeId()));

        SalaryPayment salaryPayment = salaryPaymentMapper.toEntity(salaryPaymentDTO);
        salaryPayment.setEmployee(employee);

        SalaryPayment savedSalaryPayment = salaryPaymentRepository.save(salaryPayment);
        return salaryPaymentMapper.toResponseDto(savedSalaryPayment);
    }

    @Override
    @Transactional
    public List<SalaryPaymentResponseDTO> createBatchSalaryPayments(SalaryPaymentBatchDTO batchDTO) {
        return batchDTO.payments().stream()
                .map(this::createSalaryPayment)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalaryPaymentResponseDTO> getAllSalaryPayments(SalaryPaymentFilterDTO filterDTO, Pageable pageable) {
        return salaryPaymentRepository.findAllWithFilters(
                filterDTO.employeeId(),
                filterDTO.firstName(),
                filterDTO.lastName(),
                filterDTO.salaryFrequency(),
                filterDTO.projectAreaId(),
                filterDTO.paymentDateFrom(),
                filterDTO.paymentDateTo(),
                filterDTO.minAmount(),
                filterDTO.maxAmount(),
                filterDTO.paymentMethod(),
                filterDTO.search(),
                pageable
        ).map(salaryPaymentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryPaymentResponseDTO getSalaryPaymentById(Long id) {
        return salaryPaymentRepository.findByIdAndEmployeeDeletedFalse(id)
                .map(salaryPaymentMapper::toResponseDto)
                .orElseThrow(() -> new SalaryPaymentNotFoundException(id));
    }

    @Override
    @Transactional
    public SalaryPaymentResponseDTO updateSalaryPayment(Long id, SalaryPaymentDTO salaryPaymentDTO) {
        SalaryPayment existingSalaryPayment = salaryPaymentRepository.findByIdAndEmployeeDeletedFalse(id)
                .orElseThrow(() -> new SalaryPaymentNotFoundException(id));

        if (salaryPaymentDTO.employeeId() != null) {
            validateEmployeeExists(salaryPaymentDTO.employeeId());
        }

        validateBusinessRulesForUpdate(salaryPaymentDTO, existingSalaryPayment);

        if (salaryPaymentDTO.employeeId() != null &&
            !existingSalaryPayment.getEmployee().getId().equals(salaryPaymentDTO.employeeId())) {
            Employee employee = employeeRepository.findByIdAndDeletedFalse(salaryPaymentDTO.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(salaryPaymentDTO.employeeId()));
            existingSalaryPayment.setEmployee(employee);
        }

        salaryPaymentMapper.partialUpdate(salaryPaymentDTO, existingSalaryPayment);
        SalaryPayment updatedSalaryPayment = salaryPaymentRepository.save(existingSalaryPayment);
        return salaryPaymentMapper.toResponseDto(updatedSalaryPayment);
    }

    @Override
    @Transactional
    public void deleteSalaryPayment(Long id) {
        SalaryPayment salaryPayment = salaryPaymentRepository.findByIdAndEmployeeDeletedFalse(id)
                .orElseThrow(() -> new SalaryPaymentNotFoundException(id));

        salaryPaymentRepository.delete(salaryPayment);
    }

    private void validateEmployeeExists(Long employeeId) {
        if (!employeeRepository.existsByIdAndDeletedFalse(employeeId)) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }

    private void validateBusinessRules(SalaryPaymentDTO salaryPaymentDTO) {
        if (salaryPaymentDTO.paymentDate().isAfter(LocalDate.now())) {
            throw new SalaryPaymentNotValidException(messageSourceHelper.getMessage("salaryPayment.paymentDate.future"));
        }

        if (salaryPaymentDTO.amount().signum() <= 0) {
            throw new SalaryPaymentNotValidException(messageSourceHelper.getMessage("salaryPayment.amount.positive"));
        }

        if (salaryPaymentDTO.amount().compareTo(java.math.BigDecimal.valueOf(100000000)) > 0) {
            throw new SalaryPaymentNotValidException(messageSourceHelper.getMessage("salaryPayment.amount.exceedsMaximum"));
        }
    }

    private void validateBusinessRulesForUpdate(SalaryPaymentDTO salaryPaymentDTO, SalaryPayment existingSalaryPayment) {
        if (salaryPaymentDTO.paymentDate() != null && salaryPaymentDTO.paymentDate().isAfter(LocalDate.now())) {
            throw new SalaryPaymentNotValidException(messageSourceHelper.getMessage("salaryPayment.paymentDate.future"));
        }

        if (salaryPaymentDTO.amount() != null) {
            if (salaryPaymentDTO.amount().signum() <= 0) {
                throw new SalaryPaymentNotValidException(messageSourceHelper.getMessage("salaryPayment.amount.positive"));
            }

            if (salaryPaymentDTO.amount().compareTo(java.math.BigDecimal.valueOf(100000000)) > 0) {
                throw new SalaryPaymentNotValidException(messageSourceHelper.getMessage("salaryPayment.amount.exceedsMaximum"));
            }
        }
    }

    private void validateDuplicatePayment(Long employeeId, PSG.backEnd.model.enums.employee.SalaryFrecuency frequency,
                                          LocalDate paymentDate, Long excludePaymentId) {
        int year = paymentDate.getYear();
        int month = paymentDate.getMonthValue();

        switch (frequency) {
            case MENSUAL:
                boolean monthlyPaymentExists = salaryPaymentRepository.existsMonthlyPaymentForEmployeeInMonth(
                        employeeId, year, month, excludePaymentId);

                if (monthlyPaymentExists) {
                    throw new DuplicateSalaryPaymentException(
                            String.format("Employee already has a monthly payment registered for %d-%02d", year, month));
                }
                break;

            case QUINCENAL:
                long biweeklyCount = salaryPaymentRepository.countBiweeklyPaymentsForEmployeeInMonth(
                        employeeId, year, month, excludePaymentId);

                if (biweeklyCount >= 2) {
                    throw new DuplicateSalaryPaymentException(
                            String.format("Employee already has 2 biweekly payments registered for %d-%02d. Maximum allowed is 2 per month",
                                    year, month));
                }
                break;

            case SEMANAL:
                break;
        }
    }
}

