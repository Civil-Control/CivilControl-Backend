package PSG.backEnd.service.notification.resolver;

import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import PSG.backEnd.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VehicleVtvDueDateResolver implements NextDueDateResolver {

    private final VehicleRepository vehicleRepository;

    @Override
    public NotificationSubjectType getSubjectType() { return NotificationSubjectType.VEHICLE_VTV; }

    @Override
    public List<SubjectDueDateInfo> resolveForId(Long vehicleId) {
        return vehicleRepository.findByIdAndDeletedFalse(vehicleId)
                .filter(v -> v.getVtvExpirationDate() != null)
                .map(v -> new SubjectDueDateInfo(v.getId(), buildDisplayName(v), v.getVtvExpirationDate()))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<SubjectDueDateInfo> resolveAll() {
        return vehicleRepository.findByDeletedFalse().stream()
                .filter(v -> v.getVtvExpirationDate() != null)
                .map(v -> new SubjectDueDateInfo(v.getId(), buildDisplayName(v), v.getVtvExpirationDate()))
                .toList();
    }

    private String buildDisplayName(Vehicle v) {
        String brand = v.getBrand() != null ? v.getBrand() + " " : "";
        String model = v.getModel() != null ? v.getModel() + " " : "";
        return brand + model + v.getLicensePlate() + " — VTV";
    }
}
