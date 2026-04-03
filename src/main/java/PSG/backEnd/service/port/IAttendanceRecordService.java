package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.AttendanceRecordBatchDTO;
import PSG.backEnd.model.dto.employee.AttendanceRecordDTO;
import PSG.backEnd.model.dto.employee.AttendanceRecordFilterDTO;
import PSG.backEnd.model.dto.employee.AttendanceRecordResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAttendanceRecordService {
    AttendanceRecordResponseDTO createAttendanceRecord(AttendanceRecordDTO dto);
    List<AttendanceRecordResponseDTO> createBatchAttendanceRecords(AttendanceRecordBatchDTO batchDTO);
    AttendanceRecordResponseDTO getAttendanceRecordById(Long id);
    AttendanceRecordResponseDTO updateAttendanceRecord(Long id, AttendanceRecordDTO dto);
    void deleteAttendanceRecord(Long id);
    Page<AttendanceRecordResponseDTO> getAllAttendanceRecords(AttendanceRecordFilterDTO filterDTO, Pageable pageable);
    long countWithFilters(AttendanceRecordFilterDTO filterDTO);
    List<AttendanceRecordResponseDTO> getAllAttendanceRecordsNoPage(AttendanceRecordFilterDTO filterDTO);
    byte[] generateTemplate();
    byte[] exportToExcel(AttendanceRecordFilterDTO filterDTO);
}
