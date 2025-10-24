package PSG.backEnd.exception.disciplinaryAction;

public class DisciplinaryActionNotFoundException extends RuntimeException {
    public DisciplinaryActionNotFoundException(Long id) {
        super("No disciplinary action found for ID: " + id);
    }
}

