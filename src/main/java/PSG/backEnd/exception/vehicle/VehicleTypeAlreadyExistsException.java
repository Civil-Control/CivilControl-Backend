package PSG.backEnd.exception.vehicle;

public class VehicleTypeAlreadyExistsException extends RuntimeException {
    public VehicleTypeAlreadyExistsException(String name) {
        super("Ya existe un tipo de vehículo con el nombre: " + name);
    }
}

