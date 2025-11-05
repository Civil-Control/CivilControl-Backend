package PSG.backEnd.exception.building;

public class BuildingNotFoundException extends RuntimeException {
    public BuildingNotFoundException(Long id) {
        super("No building found for ID: " + id);
    }

    public BuildingNotFoundException(String message) {
        super(message);
    }
}

