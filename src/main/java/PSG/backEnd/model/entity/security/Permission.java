package PSG.backEnd.model.entity.security;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a system permission.
 * Permissions are the minimum unit of authorization and are defined in code (AppPermissions class).
 * They are automatically synchronized with the DB when the application starts.
 */
@Entity
@Table(name = "permissions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique permission name (e.g., "EMPLOYEE_READ", "VEHICLE_WRITE").
     * Must exactly match the constants defined in AppPermissions.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Module to which the permission belongs (e.g., "Employees", "Vehicles", "System").
     * Used to group permissions in the user interface.
     */
    @Column(nullable = false, length = 50)
    private String module;

    /**
     * Human-readable description of the permission to display in the UI.
     */
    @Column(length = 255)
    private String description;
}

