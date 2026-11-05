package PSG.backEnd.exception.building;

public class BuildingDataConflictException extends RuntimeException {
    public BuildingDataConflictException(String message) {
        super(message);
    }

    public BuildingDataConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

