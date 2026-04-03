package PSG.backEnd.exception.attendanceRecord;

public class DuplicateAttendanceRecordException extends RuntimeException {
    public DuplicateAttendanceRecordException(String message) {
        super(message);
    }
}
