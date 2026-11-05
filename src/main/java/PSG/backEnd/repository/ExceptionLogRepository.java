package PSG.backEnd.repository;

import PSG.backEnd.model.entity.ExceptionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ExceptionLog entity
 * Provides standard CRUD operations and custom queries
 */
@Repository
public interface ExceptionLogRepository extends JpaRepository<ExceptionLog, Long> {

    /**
     * Find all exception logs within a date range
     */
    List<ExceptionLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find all exception logs by exception name
     */
    List<ExceptionLog> findByExceptionName(String exceptionName);

    /**
     * Find all exception logs by path
     */
    List<ExceptionLog> findByPathContaining(String path);
}

