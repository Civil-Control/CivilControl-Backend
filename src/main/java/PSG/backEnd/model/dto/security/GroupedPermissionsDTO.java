package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * DTO that groups permissions by module.
 * Facilitates permission presentation in Angular UI.
 */
@Schema(description = "Permissions grouped by module to facilitate UI visualization")
public record GroupedPermissionsDTO(

        @Schema(description = "Map where the key is the module name and the value is the list of permissions for that module",
                example = """
                {
                  "Projects": [
                    { "id": 1, "name": "PROJECT_READ", "module": "Projects", "description": "..." },
                    { "id": 2, "name": "PROJECT_WRITE", "module": "Projects", "description": "..." }
                  ],
                  "Employees": [
                    { "id": 10, "name": "EMPLOYEE_READ", "module": "Employees", "description": "..." }
                  ]
                }
                """)
        Map<String, List<PermissionDTO>> permissions
) {}

