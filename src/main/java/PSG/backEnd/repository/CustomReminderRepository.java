package PSG.backEnd.repository;

import PSG.backEnd.model.entity.notification.CustomReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomReminderRepository extends JpaRepository<CustomReminder, Long> {

    Optional<CustomReminder> findByIdAndDeletedFalse(Long id);

    List<CustomReminder> findAllByUserIdAndDeletedFalse(Long userId);

    List<CustomReminder> findAllByDeletedFalse();

    @Modifying
    @Query("UPDATE CustomReminder r SET r.deleted = true WHERE r.userId = :userId")
    void markDeletedByUserId(@Param("userId") Long userId);
}
