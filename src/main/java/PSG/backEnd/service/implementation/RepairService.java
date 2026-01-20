package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.supplier.SupplierNotValidException;
import PSG.backEnd.exception.vehicle.RepairNotFoundException;
import PSG.backEnd.exception.vehicle.VehicleNotValidException;
import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairFilterDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.mapper.RepairMapper;
import PSG.backEnd.repository.RepairRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.IRepairService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepairService implements IRepairService {

    private final RepairRepository repairRepository;
    private final RepairMapper repairMapper;
    private final VehicleRepository vehicleRepository;
    private final SupplierRepository supplierRepository;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public RepairResponseDTO createRepair(RepairDTO repairDTO) {
        // Validate that the vehicle exists
        validateVehicleExists(repairDTO.vehicleId());

        // Validate that the supplier exists if supplierId is provided
        if (repairDTO.supplierId() != null) {
            validateSupplierExists(repairDTO.supplierId());
        }

        // Validate that at least one of employee or supplierId is provided
        validateRepairSource(repairDTO);

        Repair repair = repairMapper.toEntity(repairDTO);
        Repair savedRepair = repairRepository.save(repair);

        return repairMapper.toResponseDto(savedRepair);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairResponseDTO> getAllRepairs(RepairFilterDTO filterDTO, Pageable pageable) {
        // Validate vehicle if a filter by vehicleId is provided
        if (filterDTO.vehicleId() != null) {
            validateVehicleExists(filterDTO.vehicleId());
        }

        // Validate supplier if a filter by supplierId is provided
        if (filterDTO.supplierId() != null) {
            validateSupplierExists(filterDTO.supplierId());
        }

        Page<Repair> repairs = repairRepository.findAllWithFilters(
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.minCost(),
                filterDTO.maxCost(),
                filterDTO.employee(),
                filterDTO.supplierId(),
                filterDTO.supplierLegalName(),
                filterDTO.repairType(),
                pageable
        );

        return repairs.map(repairMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RepairResponseDTO getRepairById(Long id) {
        Repair repair = repairRepository.findById(id)
                .orElseThrow(() -> new RepairNotFoundException(id));

        return repairMapper.toResponseDto(repair);
    }

    @Override
    @Transactional
    public RepairResponseDTO updateRepair(Long id, RepairDTO repairDTO) {
        Repair existingRepair = repairRepository.findById(id)
                .orElseThrow(() -> new RepairNotFoundException(id));

        // Validate vehicle if it is being updated
        if (repairDTO.vehicleId() != null) {
            validateVehicleExists(repairDTO.vehicleId());
        }

        // Validate supplier if it is being updated
        if (repairDTO.supplierId() != null) {
            validateSupplierExists(repairDTO.supplierId());
        }

        // Validate that at least one of employee or supplierId is provided after the update
        validateRepairSourceForUpdate(repairDTO, existingRepair);

        repairMapper.partialUpdate(repairDTO, existingRepair);
        Repair updatedRepair = repairRepository.save(existingRepair);

        return repairMapper.toResponseDto(updatedRepair);
    }

    @Override
    @Transactional
    public void deleteRepair(Long id) {
        Repair repair = repairRepository.findById(id)
                .orElseThrow(() -> new RepairNotFoundException(id));

        repairRepository.delete(repair);
    }

    private void validateVehicleExists(Long vehicleId) {
        if (!vehicleRepository.existsByIdAndDeletedFalse(vehicleId)) {
            throw new VehicleNotValidException(vehicleId);
        }
    }

    private void validateSupplierExists(Long supplierId) {
        if (!supplierRepository.existsByIdAndDeletedFalse(supplierId)) {
            throw new SupplierNotValidException(supplierId);
        }
    }

    private void validateRepairSource(RepairDTO repairDTO) {
        boolean hasEmployee = repairDTO.employee() != null && !repairDTO.employee().trim().isEmpty();
        boolean hasSupplier = repairDTO.supplierId() != null;

        if (!hasEmployee && !hasSupplier) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("repair.source.required"));
        }
    }

    private void validateRepairSourceForUpdate(RepairDTO repairDTO, Repair existingRepair) {
        // Determine the final values after the update
        String finalEmployee = repairDTO.employee() != null ? repairDTO.employee() : existingRepair.getEmployee();
        Long finalSupplierId = repairDTO.supplierId() != null ? repairDTO.supplierId() :
                              (existingRepair.getSupplier() != null ? existingRepair.getSupplier().getId() : null);

        boolean hasEmployee = finalEmployee != null && !finalEmployee.trim().isEmpty();
        boolean hasSupplier = finalSupplierId != null;

        if (!hasEmployee && !hasSupplier) {
            throw new IllegalArgumentException(messageSourceHelper.getMessage("repair.source.required"));
        }
    }
}
