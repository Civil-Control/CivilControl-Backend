package PSG.backEnd.exception.attendanceRecord;

import PSG.backEnd.service.util.MessageSourceHelper;

public class AttendanceRecordNotFoundException extends RuntimeException {
    public AttendanceRecordNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("attendanceRecord.notFound", id));
    }
}
