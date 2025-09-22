package PSG.backEnd.exception.vehicle;

public class ProjectAreaNotValidException extends RuntimeException {
    public ProjectAreaNotValidException(Long projectAreaId) {
        super("Project area with ID " + projectAreaId + " does not exist or is not available");
    }
}
