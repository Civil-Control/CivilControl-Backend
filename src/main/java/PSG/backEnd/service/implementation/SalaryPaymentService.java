package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.projectarea.ProjectAreaNotFoundException;
import PSG.backEnd.exception.salaryPayment.DuplicateSalaryPaymentException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotFoundException;
import PSG.backEnd.exception.salaryPayment.SalaryPaymentNotValidException;
import PSG.backEnd.model.dto.employee.SalaryPaymentBatchDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentFilterDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.mapper.SalaryPaymentMapper;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.ProjectAreaRepository;
import PSG.backEnd.repository.SalaryPaymentRepository;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.port.ISalaryPaymentService;
import PSG.backEnd.service.implementation.DocumentTotalRecalculator;
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
    private final ProjectAreaRepository projectAreaRepository;
    private final SalaryPaymentMapper salaryPaymentMapper;
    private final MessageSourceHelper messageSourceHelper;
    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final DocumentTotalRecalculator documentTotalRecalculator;

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
        salaryPayment.setProjectArea(resolveProjectArea(salaryPaymentDTO.projectAreaId()));
        salaryPayment.setTransactionalDocument(resolveDocument(salaryPaymentDTO.transactionalDocumentId()));

        SalaryPayment savedSalaryPayment = salaryPaymentRepository.save(salaryPayment);
        documentTotalRecalculator.recalculateDocumentTotals(salaryPaymentDTO.transactionalDocumentId());
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
                filterDTO.transactionalDocumentId(),
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

        Long oldDocumentId = existingSalaryPayment.getTransactionalDocument() != null
                ? existingSalaryPayment.getTransactionalDocument().getId() : null;

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
        // Only update projectArea on full entity updates (employeeId present signals a full form submission).
        // This prevents clearing the project area on minimal PATCH operations like document linking/unlinking.
        if (salaryPaymentDTO.employeeId() != null) {
            existingSalaryPayment.setProjectArea(resolveProjectArea(salaryPaymentDTO.projectAreaId()));
        }
        existingSalaryPayment.setTransactionalDocument(resolveDocument(salaryPaymentDTO.transactionalDocumentId()));
        SalaryPayment updatedSalaryPayment = salaryPaymentRepository.save(existingSalaryPayment);

        // Recalculate old document if the link changed
        if (oldDocumentId != null && !oldDocumentId.equals(salaryPaymentDTO.transactionalDocumentId())) {
            documentTotalRecalculator.recalculateDocumentTotals(oldDocumentId);
        }
        documentTotalRecalculator.recalculateDocumentTotals(salaryPaymentDTO.transactionalDocumentId());

        return salaryPaymentMapper.toResponseDto(updatedSalaryPayment);
    }

    @Override
    @Transactional
    public void deleteSalaryPayment(Long id) {
        SalaryPayment salaryPayment = salaryPaymentRepository.findByIdAndEmployeeDeletedFalse(id)
                .orElseThrow(() -> new SalaryPaymentNotFoundException(id));

        Long docId = salaryPayment.getTransactionalDocument() != null
                ? salaryPayment.getTransactionalDocument().getId() : null;
        salaryPaymentRepository.delete(salaryPayment);
        documentTotalRecalculator.recalculateDocumentTotals(docId);
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

    private ProjectArea resolveProjectArea(Long projectAreaId) {
        if (projectAreaId == null) return null;
        return projectAreaRepository.findByIdAndDeletedFalse(projectAreaId)
                .orElseThrow(() -> new ProjectAreaNotFoundException(projectAreaId));
    }

    private TransactionalDocument resolveDocument(Long documentId) {
        if (documentId == null) return null;
        return transactionalDocumentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(documentId));
    }
}

