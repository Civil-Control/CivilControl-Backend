package PSG.backEnd.model.entity.security;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing user authentication credentials.
 * Separated from User entity to improve modularity and security.
 */
@Entity
@Table(name = "credentials")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Credentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username for login.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Hashed password (BCrypt).
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * Soft delete flag.
     */
    @Column(nullable = false)
    private Boolean deleted;

    /**
     * One-to-one relationship with User.
     */
    @OneToOne(mappedBy = "credentials", cascade = CascadeType.ALL)
    private User user;
}

