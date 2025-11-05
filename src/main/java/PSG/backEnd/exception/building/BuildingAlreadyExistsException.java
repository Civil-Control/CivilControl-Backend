package PSG.backEnd.exception.building;

public class BuildingAlreadyExistsException extends RuntimeException {
    public BuildingAlreadyExistsException(String message) {
        super(message);
    }
}

