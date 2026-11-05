package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.employee.EmployeeNotFoundException;
import PSG.backEnd.exception.laborIncident.LaborIncidentNotFoundException;
import PSG.backEnd.exception.laborIncident.LaborIncidentNotValidException;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentDTO;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentFilterDTO;
import PSG.backEnd.model.dto.laborIncident.LaborIncidentResponseDTO;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.employee.LaborIncident;
import PSG.backEnd.model.enums.employee.LaborIncidentStatus;
import PSG.backEnd.model.mapper.LaborIncidentMapper;
import PSG.backEnd.repository.DisciplinaryActionRepository;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.LaborIncidentRepository;
import PSG.backEnd.service.port.ILaborIncidentService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaborIncidentService implements ILaborIncidentService {

    private final LaborIncidentRepository laborIncidentRepository;
    private final EmployeeRepository employeeRepository;
    private final DisciplinaryActionRepository disciplinaryActionRepository;
    private final LaborIncidentMapper laborIncidentMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public LaborIncidentResponseDTO createLaborIncident(LaborIncidentDTO dto) {
        validateBusinessRules(dto);

        List<Employee> employees = fetchAndValidateEmployees(dto.employeeIds());

        LaborIncident entity = laborIncidentMapper.toEntity(dto);
        entity.setEmployees(employees);

        if (dto.status() == LaborIncidentStatus.RESUELTO && dto.resolvedDate() == null) {
            entity.setResolvedDate(LocalDate.now());
        }

        return laborIncidentMapper.toResponseDto(laborIncidentRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public LaborIncidentResponseDTO getLaborIncidentById(Long id) {
        return laborIncidentRepository.findById(id)
                .map(laborIncidentMapper::toResponseDto)
                .orElseThrow(() -> new LaborIncidentNotFoundException(id));
    }

    @Override
    @Transactional
    public LaborIncidentResponseDTO updateLaborIncident(Long id, LaborIncidentDTO dto) {
        LaborIncident existing = laborIncidentRepository.findById(id)
                .orElseThrow(() -> new LaborIncidentNotFoundException(id));

        validateBusinessRulesForUpdate(dto, existing);

        if (dto.employeeIds() != null) {
            existing.setEmployees(fetchAndValidateEmployees(dto.employeeIds()));
        }

        LaborIncidentStatus effectiveStatus = dto.status() != null ? dto.status() : existing.getStatus();
        LocalDate effectiveResolvedDate = dto.resolvedDate() != null ? dto.resolvedDate() : existing.getResolvedDate();

        laborIncidentMapper.partialUpdate(dto, existing);

        if (effectiveStatus == LaborIncidentStatus.RESUELTO && effectiveResolvedDate == null) {
            existing.setResolvedDate(LocalDate.now());
        }

        return laborIncidentMapper.toResponseDto(laborIncidentRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteLaborIncident(Long id) {
        LaborIncident existing = laborIncidentRepository.findById(id)
                .orElseThrow(() -> new LaborIncidentNotFoundException(id));

        disciplinaryActionRepository.nullifyLaborIncident(id);
        laborIncidentRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LaborIncidentResponseDTO> getAllLaborIncidents(LaborIncidentFilterDTO filterDTO, Pageable pageable) {
        return laborIncidentRepository.findAllWithFilters(
                filterDTO.employeeId(),
                filterDTO.employeeSearch(),
                filterDTO.incidentType(),
                filterDTO.status(),
                filterDTO.incidentDateFrom(),
                filterDTO.incidentDateTo(),
                filterDTO.hasFinancialImpact(),
                pageable
        ).map(laborIncidentMapper::toResponseDto);
    }

    private List<Employee> fetchAndValidateEmployees(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.employees.required"));
        }
        return employeeIds.stream()
                .map(empId -> employeeRepository.findByIdAndDeletedFalse(empId)
                        .orElseThrow(() -> new EmployeeNotFoundException(empId)))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void validateBusinessRules(LaborIncidentDTO dto) {
        if (dto.incidentDate().isAfter(LocalDate.now())) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.incidentDate.future"));
        }

        if (dto.financialImpact() != null && dto.financialImpact().signum() < 0) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.financialImpact.negative"));
        }

        if (dto.resolvedDate() != null && dto.status() != LaborIncidentStatus.RESUELTO) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.resolvedDate.onlyResolved"));
        }

        if (dto.resolvedDate() != null && dto.resolvedDate().isBefore(dto.incidentDate())) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.resolvedDate.afterIncidentDate"));
        }
    }

    private void validateBusinessRulesForUpdate(LaborIncidentDTO dto, LaborIncident existing) {
        LocalDate effectiveIncidentDate = dto.incidentDate() != null ? dto.incidentDate() : existing.getIncidentDate();
        LaborIncidentStatus effectiveStatus = dto.status() != null ? dto.status() : existing.getStatus();
        LocalDate effectiveResolvedDate = dto.resolvedDate() != null ? dto.resolvedDate() : existing.getResolvedDate();

        if (dto.incidentDate() != null && dto.incidentDate().isAfter(LocalDate.now())) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.incidentDate.future"));
        }

        if (dto.financialImpact() != null && dto.financialImpact().signum() < 0) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.financialImpact.negative"));
        }

        if (effectiveResolvedDate != null && effectiveStatus != LaborIncidentStatus.RESUELTO) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.resolvedDate.onlyResolved"));
        }

        if (effectiveResolvedDate != null && effectiveResolvedDate.isBefore(effectiveIncidentDate)) {
            throw new LaborIncidentNotValidException(
                    messageSourceHelper.getMessage("laborIncident.resolvedDate.afterIncidentDate"));
        }
    }
}
