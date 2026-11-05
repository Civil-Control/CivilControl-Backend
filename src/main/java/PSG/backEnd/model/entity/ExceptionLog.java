package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity to persist exception logs in PostgreSQL database
 * Used for auditing and debugging purposes
 */
@Entity
@Table(name = "exception_log")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ExceptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exception_name", nullable = false, length = 255)
    private String exceptionName;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "path", length = 500)
    private String path;

    @Column(name = "method", length = 10)
    private String method;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
}

