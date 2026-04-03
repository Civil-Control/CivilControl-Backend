package PSG.backEnd.exception.attendanceRecord;

public class AttendanceImportException extends RuntimeException {
    public AttendanceImportException(String message) {
        super(message);
    }

    public AttendanceImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
