package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.supplier.SupplierNotValidException;
import PSG.backEnd.exception.vehicle.*;
import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.vehicle.*;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.entity.vehicle.RepairOrder;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.enums.vehicle.RepairOrderStatus;
import PSG.backEnd.model.enums.vehicle.RepairType;
import PSG.backEnd.model.mapper.RepairOrderMapper;
import PSG.backEnd.repository.RepairOrderRepository;
import PSG.backEnd.repository.RepairRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.IRepairOrderService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairOrderService implements IRepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final RepairRepository repairRepository;
    private final RepairOrderMapper repairOrderMapper;
    private final VehicleRepository vehicleRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public RepairOrderResponseDTO createRepairOrder(RepairOrderRequestDTO dto) {
        validateVehicleExists(dto.vehicleId());

        RepairOrder order = repairOrderMapper.toEntity(dto);
        order.setStatus(RepairOrderStatus.PENDIENTE);
        order.setCreatedByUser(getCurrentUser());

        return repairOrderMapper.toResponseDto(repairOrderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public RepairOrderResponseDTO getRepairOrderById(Long id) {
        RepairOrder order = findOrderById(id);
        validateOwnership(order);
        return repairOrderMapper.toResponseDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairOrderResponseDTO> getAllRepairOrders(RepairOrderFilterDTO filterDTO, Pageable pageable) {
        return repairOrderRepository.findAllWithFilters(
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.status(),
                filterDTO.search(),
                pageable
        ).map(repairOrderMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairOrderResponseDTO> getMyRepairOrders(RepairOrderFilterDTO filterDTO, Pageable pageable) {
        Long userId = getCurrentUser().getId();
        return repairOrderRepository.findAllByCreatedByUserWithFilters(
                userId,
                filterDTO.dateFrom(),
                filterDTO.dateTo(),
                filterDTO.vehicleId(),
                filterDTO.vehicleLicensePlate(),
                filterDTO.status(),
                filterDTO.search(),
                pageable
        ).map(repairOrderMapper::toResponseDto);
    }

    @Override
    @Transactional
    public RepairOrderResponseDTO updateRepairOrder(Long id, RepairOrderRequestDTO dto) {
        RepairOrder order = findOrderById(id);
        validateOwnership(order);
        validateEditableStatus(order);

        if (dto.vehicleId() != null) {
            validateVehicleExists(dto.vehicleId());
        }

        repairOrderMapper.partialUpdate(dto, order);
        return repairOrderMapper.toResponseDto(repairOrderRepository.save(order));
    }

    @Override
    @Transactional
    public void deleteRepairOrder(Long id) {
        RepairOrder order = findOrderById(id);
        validateOwnership(order);
        validateDeleteableStatus(order);
        order.setDeleted(true);
        repairOrderRepository.save(order);
    }

    @Override
    @Transactional
    public RepairOrderResponseDTO changeStatus(Long id, RepairOrderStatusDTO statusDTO) {
        RepairOrder order = findOrderById(id);

        // Only PENDIENTE → EN_PROCESO is allowed via this endpoint
        if (order.getStatus() != RepairOrderStatus.PENDIENTE || statusDTO.status() != RepairOrderStatus.EN_PROCESO) {
            throw new RepairOrderNotValidException(
                    MessageSourceHelper.getMessageStatic("repairOrder.invalidStatusTransition",
                            order.getStatus().name(), statusDTO.status().name())
            );
        }

        order.setStatus(RepairOrderStatus.EN_PROCESO);
        return repairOrderMapper.toResponseDto(repairOrderRepository.save(order));
    }

    @Override
    @Transactional
    public RepairOrderResponseDTO completeRepairOrder(Long id, RepairOrderCompleteDTO completeDTO) {
        RepairOrder order = findOrderById(id);

        if (order.getStatus() != RepairOrderStatus.EN_PROCESO) {
            throw new RepairOrderNotValidException(
                    MessageSourceHelper.getMessageStatic("repairOrder.mustBeInProcess")
            );
        }

        if (completeDTO.supplierId() != null) {
            validateSupplierExists(completeDTO.supplierId());
        }

        // Build Repair from the order data + completion details
        List<RepairType> types = completeDTO.repairTypes().stream()
                .map(RepairType::valueOf)
                .collect(Collectors.toList());

        Supplier supplier = null;
        if (completeDTO.supplierId() != null) {
            supplier = new Supplier();
            supplier.setId(completeDTO.supplierId());
        }

        Repair repair = Repair.builder()
                .date(order.getDate())
                .vehicle(order.getVehicle())
                .description(completeDTO.description() != null ? completeDTO.description() : order.getDescription())
                .cost(completeDTO.cost())
                .employee(completeDTO.employee())
                .supplier(supplier)
                .repairTypes(types)
                .repairOrder(order)
                .build();

        repairRepository.save(repair);

        order.setStatus(RepairOrderStatus.COMPLETADA);
        return repairOrderMapper.toResponseDto(repairOrderRepository.save(order));
    }

    // ── Private helpers ──────────────────────────────────────────────

    private RepairOrder findOrderById(Long id) {
        return repairOrderRepository.findById(id)
                .filter(o -> Boolean.FALSE.equals(o.getDeleted()))
                .orElseThrow(() -> new RepairOrderNotFoundException(id));
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean currentUserHasReadPermission() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(AppPermissions.REPAIR_ORDER_READ));
    }

    private void validateOwnership(RepairOrder order) {
        if (!currentUserHasReadPermission()) {
            User current = getCurrentUser();
            if (!order.getCreatedByUser().getId().equals(current.getId())) {
                throw new RepairOrderNotValidException(
                        MessageSourceHelper.getMessageStatic("repairOrder.notOwnedByUser")
                );
            }
        }
    }

    private void validateEditableStatus(RepairOrder order) {
        if (order.getStatus() != RepairOrderStatus.PENDIENTE) {
            throw new RepairOrderNotValidException(
                    MessageSourceHelper.getMessageStatic("repairOrder.editOnlyPending")
            );
        }
    }

    private void validateDeleteableStatus(RepairOrder order) {
        if (order.getStatus() != RepairOrderStatus.PENDIENTE) {
            throw new RepairOrderNotValidException(
                    MessageSourceHelper.getMessageStatic("repairOrder.deleteOnlyPending")
            );
        }
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
}
