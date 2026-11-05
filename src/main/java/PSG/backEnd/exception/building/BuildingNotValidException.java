package PSG.backEnd.exception.building;

public class BuildingNotValidException extends RuntimeException {
    public BuildingNotValidException(String message) {
        super(message);
    }
}
