package PSG.backEnd.service.port;

import PSG.backEnd.model.entity.vehicle.Vehicle;

public interface IVehicleKmService {

    /** Sets the vehicle km from the record with the latest date (crew reports, repairs, manual). */
    void recalculate(Long vehicleId);

    /** Stores a manual km dated today when it differs from the current km, then recalculates. */
    void applyManualKm(Vehicle vehicle, Integer km);
}
