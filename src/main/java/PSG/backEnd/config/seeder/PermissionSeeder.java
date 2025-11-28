package PSG.backEnd.config.seeder;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.entity.security.Permission;
import PSG.backEnd.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Seeder that synchronizes permissions defined in AppPermissions with the database.
 * Runs automatically when the application starts.
 *
 * Architecture:
 * - Reads all constants from AppPermissions using reflection
 * - Creates permissions in DB if they don't exist
 * - Assigns module based on permission prefix
 *
 * Order: Runs first (@Order(1)) to ensure permissions exist
 * before creating roles or users.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PermissionSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    /**
     * Map that defines which module each permission prefix belongs to.
     * This allows grouping permissions in the frontend UI.
     */
    private static final Map<String, String> MODULE_MAP = new HashMap<>();

    static {
        MODULE_MAP.put("EMPLOYEE_VACATION_", "Employee Vacations");
        MODULE_MAP.put("EMPLOYEE_", "Employees");
        MODULE_MAP.put("DISCIPLINARY_ACTION_", "Disciplinary Actions");
        MODULE_MAP.put("SALARY_PAYMENT_", "Salary Payments");
        MODULE_MAP.put("EPP_DELIVERY_", "EPP Deliveries");
        MODULE_MAP.put("VEHICLE_", "Vehicles");
        MODULE_MAP.put("REPAIR_", "Repairs");
        MODULE_MAP.put("INSURANCE_POLICY_", "Insurance Policies");
        MODULE_MAP.put("LICENCE_PLATE_PAYMENT_", "Licence Plate Payments");
        MODULE_MAP.put("GAS_STATION_", "Gas Stations");
        MODULE_MAP.put("FUEL_LOAD_", "Fuel Loads");
        MODULE_MAP.put("ITEM_", "Inventory");
        MODULE_MAP.put("STOCK_", "Inventory");
        MODULE_MAP.put("SUPPLIER_", "Suppliers");
        MODULE_MAP.put("SERVICE_SUPPLIER_", "Service Suppliers");
        MODULE_MAP.put("SERVICE_PAYMENT_", "Service Payments");
        MODULE_MAP.put("PAYMENT_", "Payments");
        MODULE_MAP.put("TRANSACTIONAL_DOCUMENT_", "Transactional Documents");
        MODULE_MAP.put("BUILDING_", "Buildings");
        MODULE_MAP.put("PROJECT_AREA_", "Project Areas");
        MODULE_MAP.put("REPORT_", "Reports");
        MODULE_MAP.put("DATA_", "System");
        MODULE_MAP.put("USER_", "System");
        MODULE_MAP.put("ROLE_", "System");
        MODULE_MAP.put("SYSTEM_", "System");
        MODULE_MAP.put("AUDIT_", "System");
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== Starting permission synchronization ===");

        int createdCount = 0;
        int existingCount = 0;

        // Get all constants from AppPermissions using reflection
        Field[] fields = AppPermissions.class.getDeclaredFields();

        for (Field field : fields) {
            // Only process public static final String constants
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                java.lang.reflect.Modifier.isFinal(field.getModifiers()) &&
                java.lang.reflect.Modifier.isPublic(field.getModifiers()) &&
                field.getType().equals(String.class)) {

                try {
                    String permissionName = (String) field.get(null);

                    // Check if permission already exists in DB
                    if (!permissionRepository.existsByName(permissionName)) {
                        // Create new permission
                        Permission permission = Permission.builder()
                                .name(permissionName)
                                .module(getModuleForPermission(permissionName))
                                .description(generateDescription(permissionName))
                                .build();

                        permissionRepository.save(permission);
                        createdCount++;
                        log.debug("Permission created: {} - Module: {}", permissionName, permission.getModule());
                    } else {
                        existingCount++;
                    }
                } catch (IllegalAccessException e) {
                    log.error("Error accessing field {}: {}", field.getName(), e.getMessage());
                }
            }
        }

        log.info("=== Permission synchronization completed ===");
        log.info("New permissions created: {}", createdCount);
        log.info("Existing permissions: {}", existingCount);
        log.info("Total permissions in system: {}", permissionRepository.count());
    }

    /**
     * Determines which module a permission belongs to based on its prefix.
     */
    private String getModuleForPermission(String permissionName) {
        for (Map.Entry<String, String> entry : MODULE_MAP.entrySet()) {
            if (permissionName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "General"; // Default module if no prefix matches
    }

    /**
     * Generates a readable description for a permission based on its name.
     * Converts EMPLOYEE_READ to "Allows viewing employees", etc.
     */
    private String generateDescription(String permissionName) {
        // Replace underscores with spaces and convert to lowercase
        String[] parts = permissionName.split("_");

        if (parts.length < 2) {
            return "System permission";
        }

        String module = parts[0].toLowerCase();
        String action = parts[1].toLowerCase();

        // Action mapping to descriptions in English
        Map<String, String> actionMap = new HashMap<>();
        actionMap.put("read", "view");
        actionMap.put("write", "create and edit");
        actionMap.put("delete", "delete");
        actionMap.put("create", "create");
        actionMap.put("view", "view");
        actionMap.put("manage", "manage");
        actionMap.put("approve", "approve");
        actionMap.put("cancel", "cancel");
        actionMap.put("export", "export");
        actionMap.put("assign", "assign");
        actionMap.put("certify", "certify");

        String actionDescription = actionMap.getOrDefault(action, action);

        // If there are more than 2 parts, include them in the description
        if (parts.length > 2) {
            StringBuilder extraInfo = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                extraInfo.append(" ").append(parts[i].toLowerCase());
            }
            return String.format("Allows to %s %s%s", actionDescription, module, extraInfo);
        }

        return String.format("Allows to %s %s", actionDescription, module);
    }
}

