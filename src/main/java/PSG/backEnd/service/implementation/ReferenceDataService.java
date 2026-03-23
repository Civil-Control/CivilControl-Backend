package PSG.backEnd.service.implementation;

import PSG.backEnd.model.dto.reference.ReferenceItem;
import PSG.backEnd.model.entity.*;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.gasStation.GasStation;
import PSG.backEnd.model.entity.serviceSupplier.ServiceSupplier;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.repository.*;
import PSG.backEnd.service.port.IReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    @Override
    public List<ReferenceItem> getVehicleReferences() {
        return vehicleRepository.findByDeletedFalse().stream()
                .filter(Vehicle::isActive)
                .map(v -> {
                    String label = v.getLicensePlate();
                    if (v.getBrand() != null || v.getModel() != null) {
                        label += " · " + nullSafe(v.getBrand()) + " " + nullSafe(v.getModel());
                        label = label.trim();
                    }
                    return new ReferenceItem(v.getId(), label);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ReferenceItem> getEmployeeReferences() {
        return employeeRepository.findByDeletedFalse().stream()
                .map(e -> new ReferenceItem(e.getId(),
                        nullSafe(e.getLastName()) + ", " + nullSafe(e.getName())))
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
    public List<ReferenceItem> getGasStationReferences() {
        return gasStationRepository.findByDeletedFalse().stream()
                .map(gs -> {
                    Supplier sup = gs.getSupplier();
                    String label = sup != null
                            ? (sup.getTradeName() != null ? sup.getTradeName() : sup.getLegalName())
                            : "Estación #" + gs.getId();
                    return new ReferenceItem(gs.getId(), label);
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
}
