package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.entity.ExceptionLog;
import PSG.backEnd.repository.ExceptionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for querying system exception logs.
 * Accessible to users with EXCEPTION_LOG_READ permission.
 *
 * Security: All read operations require EXCEPTION_LOG_READ permission.
 *           Write/Delete operations require EXCEPTION_LOG_WRITE permission.
 */
@RestController
@RequestMapping("/api/exception-logs")
@RequiredArgsConstructor
public class ExceptionLogController {

    private final ExceptionLogRepository exceptionLogRepository;

    /**
     * Retrieves all exceptions with pagination.
     * GET /api/exception-logs?page=0&size=20&sort=timestamp,desc
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_READ + "')")
    public ResponseEntity<Page<ExceptionLog>> getAllExceptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp,desc") String[] sort) {

        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<ExceptionLog> exceptions = exceptionLogRepository.findAll(pageable);

        return ResponseEntity.ok(exceptions);
    }

    /**
     * Retrieves a specific exception by ID.
     * GET /api/exception-logs/1
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_READ + "')")
    public ResponseEntity<ExceptionLog> getExceptionById(@PathVariable Long id) {
        return exceptionLogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves exceptions filtered by type.
     * GET /api/exception-logs/by-type/NotFoundException?page=0&size=10
     */
    @GetMapping("/by-type/{exceptionName}")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_READ + "')")
    public ResponseEntity<List<ExceptionLog>> getExceptionsByType(
            @PathVariable String exceptionName) {

        List<ExceptionLog> exceptions = exceptionLogRepository.findByExceptionName(exceptionName);
        return ResponseEntity.ok(exceptions);
    }

    /**
     * Retrieves exceptions filtered by date range.
     * GET /api/exception-logs/by-date-range?start=2026-02-01T00:00:00&end=2026-02-17T23:59:59
     */
    @GetMapping("/by-date-range")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_READ + "')")
    public ResponseEntity<List<ExceptionLog>> getExceptionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<ExceptionLog> exceptions = exceptionLogRepository.findByTimestampBetween(start, end);
        return ResponseEntity.ok(exceptions);
    }

    /**
     * Retrieves exceptions filtered by path (endpoint).
     * GET /api/exception-logs/by-path?path=/api/vehicles
     */
    @GetMapping("/by-path")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_READ + "')")
    public ResponseEntity<List<ExceptionLog>> getExceptionsByPath(
            @RequestParam String path) {

        List<ExceptionLog> exceptions = exceptionLogRepository.findByPathContaining(path);
        return ResponseEntity.ok(exceptions);
    }

    /**
     * Retrieves exception statistics.
     * GET /api/exception-logs/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_READ + "')")
    public ResponseEntity<Map<String, Object>> getExceptionStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total exceptions
        long totalExceptions = exceptionLogRepository.count();
        stats.put("totalExceptions", totalExceptions);

        // Today's exceptions
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        long todayExceptions = exceptionLogRepository.findByTimestampBetween(startOfDay, endOfDay).size();
        stats.put("todayExceptions", todayExceptions);

        // Last exception
        List<ExceptionLog> lastException = exceptionLogRepository.findAll(
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();

        if (!lastException.isEmpty()) {
            stats.put("lastExceptionTimestamp", lastException.get(0).getTimestamp());
            stats.put("lastExceptionType", lastException.get(0).getExceptionName());
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * Retrieves unique exception types with count.
     * GET /api/exception-logs/types-summary
     */
    @GetMapping("/types-summary")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_READ + "')")
    public ResponseEntity<Map<String, Long>> getExceptionTypesSummary() {
        List<ExceptionLog> allExceptions = exceptionLogRepository.findAll();

        Map<String, Long> typesCount = new HashMap<>();
        for (ExceptionLog exception : allExceptions) {
            String type = exception.getExceptionName();
            typesCount.put(type, typesCount.getOrDefault(type, 0L) + 1);
        }

        return ResponseEntity.ok(typesCount);
    }

    /**
     * Deletes old exception logs (cleanup).
     * DELETE /api/exception-logs/cleanup?daysToKeep=90
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_WRITE + "')")
    public ResponseEntity<Map<String, Object>> cleanupOldExceptions(
            @RequestParam(defaultValue = "90") int daysToKeep) {

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<ExceptionLog> oldExceptions = exceptionLogRepository.findAll()
                .stream()
                .filter(log -> log.getTimestamp().isBefore(cutoffDate))
                .toList();

        int deletedCount = oldExceptions.size();
        exceptionLogRepository.deleteAll(oldExceptions);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Limpieza completada exitosamente");
        response.put("deletedCount", deletedCount);
        response.put("daysToKeep", daysToKeep);
        response.put("cutoffDate", cutoffDate);

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a specific exception by ID.
     * DELETE /api/exception-logs/1
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + AppPermissions.EXCEPTION_LOG_WRITE + "')")
    public ResponseEntity<Map<String, String>> deleteExceptionById(@PathVariable Long id) {
        if (!exceptionLogRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        exceptionLogRepository.deleteById(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Log de excepción eliminado exitosamente");
        response.put("id", id.toString());

        return ResponseEntity.ok(response);
    }
}

