package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.ExceptionLog;
import PSG.backEnd.repository.ExceptionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

/**
 * Service for auditing exceptions asynchronously
 * Persists exception logs to the database without blocking the main application flow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExceptionAuditService {

    private final ExceptionLogRepository exceptionLogRepository;

    /**
     * Saves an exception log asynchronously to the database
     * Uses REQUIRES_NEW propagation to ensure the transaction is independent
     * from the main request transaction
     *
     * @param exception the exception to log
     * @param path      the request path where the exception occurred
     * @param method    the HTTP method (GET, POST, etc.)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logException(Exception exception, String path, String method) {
        try {
            ExceptionLog exceptionLog = ExceptionLog.builder()
                    .exceptionName(exception.getClass().getSimpleName())
                    .message(exception.getMessage())
                    .path(path)
                    .method(method)
                    .timestamp(LocalDateTime.now())
                    .stackTrace(getStackTraceAsString(exception))
                    .build();

            exceptionLogRepository.save(exceptionLog);

            log.info("Exception log saved asynchronously: {} at {}",
                    exception.getClass().getSimpleName(), path);
        } catch (Exception e) {
            // Log the error but don't throw it - we don't want to disrupt the main flow
            log.error("Failed to save exception log to database", e);
        }
    }

    /**
     * Converts exception stack trace to string
     */
    private String getStackTraceAsString(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}

