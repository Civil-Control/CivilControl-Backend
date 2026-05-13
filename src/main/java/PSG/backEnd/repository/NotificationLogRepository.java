package PSG.backEnd.repository;

import PSG.backEnd.model.entity.notification.NotificationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    @Query("SELECT CONCAT(str(l.subscriptionId), '_', str(l.alertId), '_', COALESCE(str(l.subjectId), 'null')) " +
           "FROM NotificationLog l " +
           "WHERE l.tenantId = :tenantId AND l.logDate = :logDate")
    Set<String> findSentKeysForTenantAndDate(@Param("tenantId") Long tenantId,
                                              @Param("logDate") LocalDate logDate);

    @Query("SELECT l FROM NotificationLog l " +
           "WHERE l.subscriptionId IN :subscriptionIds " +
           "AND l.channel = 'SYSTEM' " +
           "AND l.status = 'SENT' " +
           "ORDER BY l.sentAt DESC")
    List<NotificationLog> findSystemInboxBySubscriptionIds(
            @Param("subscriptionIds") List<Long> subscriptionIds,
            Pageable pageable);
}
