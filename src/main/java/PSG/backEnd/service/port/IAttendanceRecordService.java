package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.employee.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface IAttendanceRecordService {
    AttendanceRecordResponseDTO createAttendanceRecord(AttendanceRecordDTO dto);
    List<AttendanceRecordResponseDTO> createBatchAttendanceRecords(AttendanceRecordBatchDTO batchDTO);
    AttendanceRecordResponseDTO getAttendanceRecordById(Long id);
    AttendanceRecordResponseDTO updateAttendanceRecord(Long id, AttendanceRecordDTO dto);
    void deleteAttendanceRecord(Long id);
    Page<AttendanceRecordResponseDTO> getAllAttendanceRecords(AttendanceRecordFilterDTO filterDTO, Pageable pageable);
    long countWithFilters(AttendanceRecordFilterDTO filterDTO);
    List<AttendanceRecordResponseDTO> getAllAttendanceRecordsNoPage(AttendanceRecordFilterDTO filterDTO);
    AttendanceImportResultDTO importFromExcel(MultipartFile file, boolean dryRun);
    AttendanceImportResultDTO importFromExcel(MultipartFile file, boolean dryRun, Set<Integer> excludeRows);
    byte[] generateTemplate();
    byte[] exportToExcel(AttendanceRecordFilterDTO filterDTO);
}
