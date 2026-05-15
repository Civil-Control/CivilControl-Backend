package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.UserVerificationToken;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserVerificationTokenRepository extends JpaRepository<UserVerificationToken, Long> {

    Optional<UserVerificationToken> findByUserIdAndChannel(Long userId, NotificationChannel channel);

    @Modifying
    @Query("DELETE FROM UserVerificationToken t WHERE t.userId = :userId AND t.channel = :channel")
    void deleteByUserIdAndChannel(@Param("userId") Long userId, @Param("channel") NotificationChannel channel);
}
