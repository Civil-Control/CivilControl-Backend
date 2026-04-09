package PSG.backEnd.service.implementation;

import PSG.backEnd.model.dto.reference.EmployeeReferenceItem;
import PSG.backEnd.model.dto.reference.GasStationReferenceItem;
import PSG.backEnd.model.dto.reference.ReferenceItem;
import PSG.backEnd.model.dto.reference.ServiceAssignmentReferenceItem;
import PSG.backEnd.model.dto.reference.VehicleReferenceItem;
import PSG.backEnd.model.entity.Client;
import PSG.backEnd.repository.ClientRepository;
import PSG.backEnd.repository.SalesDocumentRepository;
import PSG.backEnd.model.entity.*;
import PSG.backEnd.model.entity.contracts.WorkContract;
import PSG.backEnd.repository.WorkContractRepository;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.repository.*;
import PSG.backEnd.service.port.IReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides lightweight reference data (id + label) for form dropdowns.
 *
 * <p>Only active, non-deleted records are returned.  No pagination —
 * reference lists are expected to be small enough to fit in memory.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceDataService implements IReferenceDataService {

    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final SupplierRepository supplierRepository;
    private final BuildingRepository buildingRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final GasStationRepository gasStationRepository;
    private final ItemRepository itemRepository;
    private final ServiceSupplierRepository serviceSupplierRepository;
    private final ServiceAssignmentRepository serviceAssignmentRepository;
    private final ClientRepository clientRepository;
    private final WorkContractRepository workContractRepository;
    private final SalesDocumentRepository salesDocumentRepository;

    @Override
    public List<VehicleReferenceItem> getVehicleReferences() {
        return vehicleRepository.findByDeletedFalse().stream()
                .filter(Vehicle::isActive)
                .map(v -> {
                    String label = v.getLicensePlate();
                    if (v.getBrand() != null || v.getModel() != null) {
                        label += " · " + nullSafe(v.getBrand()) + " " + nullSafe(v.getModel());
                        label = label.trim();
                    }
                    Long paId = v.getProjectArea() != null ? v.getProjectArea().getId() : null;
                    return new VehicleReferenceItem(v.getId(), label, paId);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<EmployeeReferenceItem> getEmployeeReferences() {
        return employeeRepository.findByDeletedFalse().stream()
                .map(e -> new EmployeeReferenceItem(
                        e.getId(),
                        nullSafe(e.getLastName()) + ", " + nullSafe(e.getName()),
                        e.getProjectArea() != null ? e.getProjectArea().getId() : null))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getSupplierReferences() {
        return supplierRepository.findByDeletedFalse().stream()
                .filter(Supplier::isActive)
                .map(s -> new ReferenceItem(s.getId(),
                        s.getTradeName() != null ? s.getTradeName() : s.getLegalName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getBuildingReferences() {
        return buildingRepository.findAll().stream()
                .filter(b -> !Boolean.TRUE.equals(b.getDeleted()))
                .filter(b -> Boolean.TRUE.equals(b.getActive()))
                .map(b -> new ReferenceItem(b.getId(), b.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getProjectAreaReferences() {
        return projectAreaRepository.findAll().stream()
                .filter(pa -> !Boolean.TRUE.equals(pa.getDeleted()))
                .filter(pa -> Boolean.TRUE.equals(pa.getActive()))
                .map(pa -> new ReferenceItem(pa.getId(), pa.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<GasStationReferenceItem> getGasStationReferences() {
        return gasStationRepository.findByDeletedFalse().stream()
                .map(gs -> {
                    Supplier sup = gs.getSupplier();
                    String label = sup != null
                            ? (sup.getTradeName() != null ? sup.getTradeName() : sup.getLegalName())
                            : "Estación #" + gs.getId();
                    Map<String, java.math.BigDecimal> pricesMap = gs.getPrices().stream()
                            .collect(Collectors.toMap(
                                    p -> p.getFuelType().name(),
                                    PSG.backEnd.model.entity.gasStation.GasStationPrice::getPrice));
                    Long supplierId = sup != null ? sup.getId() : null;
                    return new GasStationReferenceItem(gs.getId(), label, pricesMap, supplierId);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getItemReferences() {
        return itemRepository.findAll().stream()
                .map(i -> new ReferenceItem(i.getId(), i.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getItemReferencesByType(PSG.backEnd.model.enums.ItemType itemType) {
        return itemRepository.findByItemType(itemType).stream()
                .map(i -> new ReferenceItem(i.getId(), i.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getServiceSupplierReferences() {
        return serviceSupplierRepository.findByDeletedFalse().stream()
                .map(ss -> {
                    Supplier sup = ss.getSupplier();
                    String label = sup != null
                            ? (sup.getTradeName() != null ? sup.getTradeName() : sup.getLegalName())
                            : "Proveedor #" + ss.getId();
                    return new ReferenceItem(ss.getId(), label);
                })
                .collect(Collectors.toList());
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    @Override
    public List<ReferenceItem> getClientReferences() {
        return clientRepository.findByDeletedFalse().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .map(c -> new ReferenceItem(c.getId(),
                        c.getTradeName() != null ? c.getTradeName() : c.getBusinessName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getWorkContractReferences() {
        return workContractRepository.findByDeletedFalse().stream()
                .map(wc -> new ReferenceItem(wc.getId(),
                        wc.getContractNumber() + " — " + wc.getClient().getBusinessName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getSalesDocumentReferences() {
        return salesDocumentRepository.findAll().stream()
                .filter(sd -> !Boolean.TRUE.equals(sd.getDeleted()))
                .map(sd -> {
                    String label = sd.getDocumentType().name()
                            + " " + nullSafe(sd.getBranchCode())
                            + "-" + nullSafe(sd.getDocumentNumber());
                    if (sd.getTotal() != null) {
                        label += " — $" + String.format("%,.0f", sd.getTotal());
                    }
                    return new ReferenceItem(sd.getId(), label);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceAssignmentReferenceItem> getServiceAssignmentReferences() {
        return serviceAssignmentRepository.findByDeletedFalse().stream()
                .map(sa -> {
                    Supplier sup = sa.getServiceSupplier() != null ? sa.getServiceSupplier().getSupplier() : null;
                    String supplierName = sup != null
                            ? (sup.getTradeName() != null ? sup.getTradeName() : sup.getLegalName())
                            : "Proveedor #" + (sa.getServiceSupplier() != null ? sa.getServiceSupplier().getId() : "?");
                    String subjectName;
                    Long projectAreaId = null;
                    if (sa.getSubjectType() == PSG.backEnd.model.enums.SubjectType.VEHICLE && sa.getVehicle() != null) {
                        subjectName = sa.getVehicle().getLicensePlate();
                        if (sa.getVehicle().getProjectArea() != null) {
                            projectAreaId = sa.getVehicle().getProjectArea().getId();
                        }
                    } else if (sa.getBuilding() != null) {
                        subjectName = sa.getBuilding().getName();
                        if (sa.getBuilding().getProjectArea() != null) {
                            projectAreaId = sa.getBuilding().getProjectArea().getId();
                        }
                    } else {
                        subjectName = "";
                    }
                    String serviceType = sa.getServiceType() != null ? sa.getServiceType().getDisplayName() : "";
                    String label = supplierName + " · " + serviceType + " · " + subjectName;
                    if (sa.getAccountNumber() != null && !sa.getAccountNumber().isEmpty()) {
                        label += " (Cta: " + sa.getAccountNumber() + ")";
                    }
                    String subjectTypeStr = sa.getSubjectType() != null ? sa.getSubjectType().name() : "BUILDING";
                    return new ServiceAssignmentReferenceItem(sa.getId(), label, projectAreaId, subjectTypeStr);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getLinkedSupplierIds() {
        return serviceSupplierRepository.findByDeletedFalse().stream()
                .map(ss -> ss.getSupplier().getId())
                .collect(Collectors.toList());
    }
}
