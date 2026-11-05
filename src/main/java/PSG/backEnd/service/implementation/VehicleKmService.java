package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.vehicle.VehicleKmSource;
import PSG.backEnd.repository.CrewAssignmentRepository;
import PSG.backEnd.repository.RepairRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.service.port.IVehicleKmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleKmService implements IVehicleKmService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private static final Comparator<KmRecord> LATEST = Comparator
            .comparing(KmRecord::date)
            .thenComparing(r -> r.source() == VehicleKmSource.MANUAL)
            .thenComparing(KmRecord::km);

    private final VehicleRepository vehicleRepository;
    private final CrewAssignmentRepository crewAssignmentRepository;
    private final RepairRepository repairRepository;

    @Override
    @Transactional
    public void recalculate(Long vehicleId) {
        if (vehicleId == null) {
            return;
        }
        vehicleRepository.findById(vehicleId).ifPresent(this::recalculate);
    }

    @Override
    @Transactional
    public void applyManualKm(Vehicle vehicle, Integer km) {
        if (km == null) {
            if (vehicle.getManualKm() == null) {
                return;
            }
            vehicle.setManualKm(null);
            vehicle.setManualKmDate(null);
        } else {
            if (km.equals(vehicle.getKm())) {
                return;
            }
            vehicle.setManualKm(km);
            vehicle.setManualKmDate(LocalDate.now(ZONE));
        }
        recalculate(vehicle);
    }

    private void recalculate(Vehicle vehicle) {
        List<KmRecord> records = new ArrayList<>();
        crewAssignmentRepository.findByVehicleIdAndDeletedFalseAndKmIsNotNull(vehicle.getId())
                .forEach(ca -> records.add(new KmRecord(ca.getDate(), ca.getKm(), VehicleKmSource.PARTE_DIARIO)));
        repairRepository.findByVehicleIdAndMileageIsNotNull(vehicle.getId())
                .forEach(r -> records.add(new KmRecord(r.getDate(), r.getMileage(), VehicleKmSource.REPARACION)));
        if (vehicle.getManualKm() != null && vehicle.getManualKmDate() != null) {
            records.add(new KmRecord(vehicle.getManualKmDate(), vehicle.getManualKm(), VehicleKmSource.MANUAL));
        }

        records.stream().max(LATEST).ifPresentOrElse(latest -> {
            vehicle.setKm(latest.km());
            vehicle.setKmDate(latest.date());
            vehicle.setKmSource(latest.source());
        }, () -> {
            if (vehicle.getKmSource() != null) {
                vehicle.setKm(null);
            }
            vehicle.setKmDate(null);
            vehicle.setKmSource(null);
        });
        vehicleRepository.save(vehicle);
    }

    private record KmRecord(LocalDate date, Integer km, VehicleKmSource source) {}
}
