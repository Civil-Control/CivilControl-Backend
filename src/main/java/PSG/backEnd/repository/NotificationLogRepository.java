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

    @Query("SELECT CONCAT(str(l.subscriptionId), '_', str(l.alertId), '_', COALESCE(str(l.subjectId), 'null'), '_', str(l.dueDate)) " +
           "FROM NotificationLog l " +
           "WHERE l.tenantId = :tenantId AND l.status = 'SENT' AND l.dueDate >= :fromDate")
    Set<String> findSentCycleKeysForTenant(@Param("tenantId") Long tenantId,
                                           @Param("fromDate") LocalDate fromDate);

    @Query("SELECT l FROM NotificationLog l " +
           "WHERE l.subscriptionId IN :subscriptionIds " +
           "AND l.channel = 'SYSTEM' " +
           "AND l.status = 'SENT' " +
           "AND l.dismissed = false " +
           "ORDER BY l.sentAt DESC")
    List<NotificationLog> findSystemInboxBySubscriptionIds(
            @Param("subscriptionIds") List<Long> subscriptionIds,
            Pageable pageable);
}
