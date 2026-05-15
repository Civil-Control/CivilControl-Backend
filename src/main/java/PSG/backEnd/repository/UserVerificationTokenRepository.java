package PSG.backEnd.repository;

import PSG.backEnd.model.entity.security.UserVerificationToken;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserVerificationTokenRepository extends JpaRepository<UserVerificationToken, Long> {

    Optional<UserVerificationToken> findByUserIdAndChannel(Long userId, NotificationChannel channel);

    void deleteByUserIdAndChannel(Long userId, NotificationChannel channel);
}
