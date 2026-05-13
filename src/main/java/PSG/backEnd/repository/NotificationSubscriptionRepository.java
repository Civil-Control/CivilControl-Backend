package PSG.backEnd.repository;

import PSG.backEnd.model.entity.notification.NotificationSubscription;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

    List<NotificationSubscription> findAllByDeletedFalseAndActiveTrue();

    List<NotificationSubscription> findAllByUserIdAndDeletedFalse(Long userId);

    boolean existsByUserIdAndSubjectTypeAndSubjectIdAndDeletedFalse(
            Long userId, NotificationSubjectType subjectType, Long subjectId);

    boolean existsByUserIdAndSubjectTypeAndSubjectIdIsNullAndDeletedFalse(
            Long userId, NotificationSubjectType subjectType);

    @Modifying
    @Query("UPDATE NotificationSubscription s SET s.deleted = true WHERE s.userId = :userId")
    void markDeletedByUserId(@Param("userId") Long userId);
}
