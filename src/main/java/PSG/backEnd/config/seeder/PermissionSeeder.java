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
import java.util.Optional;

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

    /**
     * Map that defines Spanish translations for module names.
     */
    private static final Map<String, String> MODULE_SPANISH_MAP = new HashMap<>();

    /**
     * Map that defines Spanish translations for action types.
     */
    private static final Map<String, String> ACTION_SPANISH_MAP = new HashMap<>();

    /**
     * Map that defines which work module (general area) each permission prefix belongs to.
     * Work modules represent high-level groupings of system functionality.
     */
    private static final Map<String, String> WORK_MODULE_MAP = new HashMap<>();

    /**
     * Map that defines Spanish translations for work modules.
     */
    private static final Map<String, String> WORK_MODULE_SPANISH_MAP = new HashMap<>();

    static {
        MODULE_MAP.put("EMPLOYEE_VACATION_", "Employee Vacations");
        MODULE_MAP.put("EMPLOYEE_", "Employees");
        MODULE_MAP.put("DISCIPLINARY_ACTION_", "Disciplinary Actions");
        MODULE_MAP.put("SALARY_PAYMENT_", "Salary Payments");
        MODULE_MAP.put("EPP_DELIVERY_", "EPP Deliveries");
        MODULE_MAP.put("VEHICLE_", "Vehicles");
        MODULE_MAP.put("REPAIR_ORDER_", "Repair Orders");
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
        MODULE_MAP.put("TENANT_", "Tenants");
        MODULE_MAP.put("REPORT_", "Reports");
        MODULE_MAP.put("DATA_", "System");
        MODULE_MAP.put("USER_", "Users");
        MODULE_MAP.put("ROLE_", "Roles");
        MODULE_MAP.put("SYSTEM_", "System");
        MODULE_MAP.put("AUDIT_", "System");
        MODULE_MAP.put("EXCEPTION_LOG_", "System");

        // Spanish translations for modules
        MODULE_SPANISH_MAP.put("EMPLOYEE_VACATION_", "Vacaciones de Empleados");
        MODULE_SPANISH_MAP.put("EMPLOYEE_", "Empleados");
        MODULE_SPANISH_MAP.put("DISCIPLINARY_ACTION_", "Acciones Disciplinarias");
        MODULE_SPANISH_MAP.put("SALARY_PAYMENT_", "Pagos de Salarios");
        MODULE_SPANISH_MAP.put("EPP_DELIVERY_", "Entrega de EPP");
        MODULE_SPANISH_MAP.put("VEHICLE_", "Vehículos");
        MODULE_SPANISH_MAP.put("REPAIR_ORDER_", "Órdenes de Reparación");
        MODULE_SPANISH_MAP.put("REPAIR_", "Reparaciones");
        MODULE_SPANISH_MAP.put("INSURANCE_POLICY_", "Pólizas de Seguro");
        MODULE_SPANISH_MAP.put("LICENCE_PLATE_PAYMENT_", "Pagos de Patentes");
        MODULE_SPANISH_MAP.put("GAS_STATION_", "Estación de Servicios");
        MODULE_SPANISH_MAP.put("FUEL_LOAD_", "Cargas de Combustible");
        MODULE_SPANISH_MAP.put("ITEM_", "Inventario");
        MODULE_SPANISH_MAP.put("STOCK_", "Stock");
        MODULE_SPANISH_MAP.put("SUPPLIER_", "Proveedores");
        MODULE_SPANISH_MAP.put("SERVICE_SUPPLIER_", "Proveedores de Servicios");
        MODULE_SPANISH_MAP.put("SERVICE_PAYMENT_", "Pagos de Servicios");
        MODULE_SPANISH_MAP.put("PAYMENT_", "Pagos");
        MODULE_SPANISH_MAP.put("TRANSACTIONAL_DOCUMENT_", "Documentos Transaccionales");
        MODULE_SPANISH_MAP.put("BUILDING_", "Edificios");
        MODULE_SPANISH_MAP.put("PROJECT_AREA_", "Áreas de Proyecto");
        MODULE_SPANISH_MAP.put("TENANT_", "Empresa");
        MODULE_SPANISH_MAP.put("REPORT_", "Reportes");
        MODULE_SPANISH_MAP.put("DATA_", "Sistema");
        MODULE_SPANISH_MAP.put("USER_", "Usuarios");
        MODULE_SPANISH_MAP.put("ROLE_", "Roles");
        MODULE_SPANISH_MAP.put("SYSTEM_", "Sistema");
        MODULE_SPANISH_MAP.put("AUDIT_", "Sistema");
        MODULE_SPANISH_MAP.put("EXCEPTION_LOG_", "Sistema");

        // Spanish translations for actions
        ACTION_SPANISH_MAP.put("READ", "Lectura");
        ACTION_SPANISH_MAP.put("WRITE", "Escritura");
        ACTION_SPANISH_MAP.put("DELETE", "Eliminación");
        ACTION_SPANISH_MAP.put("CREATE", "Creación");
        ACTION_SPANISH_MAP.put("VIEW", "Vista");
        ACTION_SPANISH_MAP.put("MANAGE", "Gestión");
        ACTION_SPANISH_MAP.put("MANAGEMENT", "Gestión");
        ACTION_SPANISH_MAP.put("APPROVE", "Aprobación");
        ACTION_SPANISH_MAP.put("CANCEL", "Cancelación");
        ACTION_SPANISH_MAP.put("EXPORT", "Exportación");
        ACTION_SPANISH_MAP.put("ASSIGN", "Asignación");
        ACTION_SPANISH_MAP.put("CERTIFY", "Certificación");
        ACTION_SPANISH_MAP.put("FINANCIAL", "Financiero");
        ACTION_SPANISH_MAP.put("CONFIG", "Configuración");

        // Work module mappings (high-level groupings)
        // services: serviceSupplier, servicePayment
        WORK_MODULE_MAP.put("SERVICE_SUPPLIER_", "services");
        WORK_MODULE_MAP.put("SERVICE_PAYMENT_", "services");

        // documents: transactionalDocument, payment
        WORK_MODULE_MAP.put("TRANSACTIONAL_DOCUMENT_", "documents");
        WORK_MODULE_MAP.put("PAYMENT_", "documents");

        // vehicles: vehicle, fuelLoad, gasStation, licencePlatePayment, insurancePolicy, repair
        WORK_MODULE_MAP.put("VEHICLE_", "vehicles");
        WORK_MODULE_MAP.put("FUEL_LOAD_", "vehicles");
        WORK_MODULE_MAP.put("GAS_STATION_", "vehicles");
        WORK_MODULE_MAP.put("LICENCE_PLATE_PAYMENT_", "vehicles");
        WORK_MODULE_MAP.put("INSURANCE_POLICY_", "vehicles");

        // personal: employee, salaryPayment, disciplinaryAction, eppDelivery, employeeVacation
        WORK_MODULE_MAP.put("EMPLOYEE_VACATION_", "personal");
        WORK_MODULE_MAP.put("EMPLOYEE_", "personal");
        WORK_MODULE_MAP.put("DISCIPLINARY_ACTION_", "personal");
        WORK_MODULE_MAP.put("SALARY_PAYMENT_", "personal");
        WORK_MODULE_MAP.put("EPP_DELIVERY_", "personal");

        // mechanic: repair, repairOrder, stock
        WORK_MODULE_MAP.put("REPAIR_ORDER_", "mechanic");
        WORK_MODULE_MAP.put("REPAIR_", "mechanic");
        WORK_MODULE_MAP.put("STOCK_", "mechanic");

        // report: report
        WORK_MODULE_MAP.put("REPORT_", "report");

        // company: supplier, item, building, projectArea, tenant
        WORK_MODULE_MAP.put("SUPPLIER_", "company");
        WORK_MODULE_MAP.put("ITEM_", "company");
        WORK_MODULE_MAP.put("BUILDING_", "company");
        WORK_MODULE_MAP.put("PROJECT_AREA_", "company");
        WORK_MODULE_MAP.put("TENANT_", "company");

        // administration: user, role, permission
        WORK_MODULE_MAP.put("USER_", "administration");
        WORK_MODULE_MAP.put("ROLE_", "administration");
        WORK_MODULE_MAP.put("DATA_", "administration");
        WORK_MODULE_MAP.put("SYSTEM_", "administration");
        WORK_MODULE_MAP.put("AUDIT_", "administration");
        WORK_MODULE_MAP.put("EXCEPTION_LOG_", "administration");

        // Spanish translations for work modules
        WORK_MODULE_SPANISH_MAP.put("services", "Servicios");
        WORK_MODULE_SPANISH_MAP.put("documents", "Documentos");
        WORK_MODULE_SPANISH_MAP.put("vehicles", "Vehículos");
        WORK_MODULE_SPANISH_MAP.put("personal", "Personal");
        WORK_MODULE_SPANISH_MAP.put("mechanic", "Mecánica");
        WORK_MODULE_SPANISH_MAP.put("report", "Reportes");
        WORK_MODULE_SPANISH_MAP.put("company", "Empresa");
        WORK_MODULE_SPANISH_MAP.put("administration", "Administración");
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Starting permission synchronization ===");

        int createdCount = 0;
        int updatedCount = 0;
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
                    Optional<Permission> existingPermission = permissionRepository.findByName(permissionName);

                    if (!existingPermission.isPresent()) {
                        // Create new permission
                        Permission permission = Permission.builder()
                                .name(permissionName)
                                .module(getModuleForPermission(permissionName))
                                .workModule(getWorkModuleForPermission(permissionName))
                                .description(generateDescription(permissionName))
                                .spanishTranslation(generateSpanishTranslation(permissionName))
                                .spanishDescription(generateSpanishDescription(permissionName))
                                .build();

                        permissionRepository.save(permission);
                        createdCount++;
                        log.debug("Permission created: {} - Module: {} - WorkModule: {} - Spanish: {} - SpanishDesc: {}",
                                permissionName, permission.getModule(), permission.getWorkModule(),
                                permission.getSpanishTranslation(), permission.getSpanishDescription());
                    } else {
                        // Update existing permission if spanish translation, work module or spanish description is missing
                        Permission permission = existingPermission.get();
                        boolean updated = false;

                        if (permission.getSpanishTranslation() == null || permission.getSpanishTranslation().isEmpty()) {
                            permission.setSpanishTranslation(generateSpanishTranslation(permissionName));
                            updated = true;
                        }

                        if (permission.getWorkModule() == null || permission.getWorkModule().isEmpty()) {
                            permission.setWorkModule(getWorkModuleForPermission(permissionName));
                            updated = true;
                        }

                        if (permission.getSpanishDescription() == null || permission.getSpanishDescription().isEmpty()) {
                            permission.setSpanishDescription(generateSpanishDescription(permissionName));
                            updated = true;
                        }

                        if (updated) {
                            permissionRepository.save(permission);
                            updatedCount++;
                            log.debug("Permission updated: {} - WorkModule: {} - Spanish: {} - SpanishDesc: {}",
                                    permissionName, permission.getWorkModule(),
                                    permission.getSpanishTranslation(), permission.getSpanishDescription());
                        } else {
                            existingCount++;
                        }
                    }
                } catch (IllegalAccessException e) {
                    log.error("Error accessing field {}: {}", field.getName(), e.getMessage());
                }
            }
        }

        log.info("=== Permission synchronization completed ===");
        log.info("New permissions created: {}", createdCount);
        log.info("Permissions updated with translations: {}", updatedCount);
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
     * Determines which work module (general area) a permission belongs to based on its prefix.
     * Returns the high-level grouping like "vehicles", "personal", "administration", etc.
     */
    private String getWorkModuleForPermission(String permissionName) {
        for (Map.Entry<String, String> entry : WORK_MODULE_MAP.entrySet()) {
            if (permissionName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "general"; // Default work module if no prefix matches
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

    /**
     * Generates a Spanish translation for a permission based on its name.
     * Converts GAS_STATION_READ to "Estación de Servicios - Lectura", etc.
     */
    private String generateSpanishTranslation(String permissionName) {
        // First, try to find the module prefix
        String moduleSpanish = null;
        String actionSpanish = null;

        for (Map.Entry<String, String> entry : MODULE_SPANISH_MAP.entrySet()) {
            if (permissionName.startsWith(entry.getKey())) {
                moduleSpanish = entry.getValue();
                // Extract the action part (everything after the module prefix)
                String actionPart = permissionName.substring(entry.getKey().length());
                actionSpanish = ACTION_SPANISH_MAP.getOrDefault(actionPart, actionPart);
                break;
            }
        }

        // If no module prefix matched, try to parse it manually
        if (moduleSpanish == null) {
            String[] parts = permissionName.split("_");
            if (parts.length >= 2) {
                String lastPart = parts[parts.length - 1];
                actionSpanish = ACTION_SPANISH_MAP.getOrDefault(lastPart, lastPart);

                // Build module name from remaining parts
                StringBuilder moduleBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) moduleBuilder.append(" ");
                    moduleBuilder.append(parts[i]);
                }
                moduleSpanish = moduleBuilder.toString();
            } else {
                return permissionName;
            }
        }

        return String.format("%s - %s", moduleSpanish, actionSpanish);
    }

    /**
     * Generates a Spanish description for a permission based on its name.
     * Converts GAS_STATION_READ to "Permite visualizar estaciones de servicios", etc.
     */
    private String generateSpanishDescription(String permissionName) {
        // Map for Spanish action descriptions
        Map<String, String> actionDescriptionMap = new HashMap<>();
        actionDescriptionMap.put("READ", "visualizar");
        actionDescriptionMap.put("WRITE", "crear y editar");
        actionDescriptionMap.put("DELETE", "eliminar");
        actionDescriptionMap.put("CREATE", "crear");
        actionDescriptionMap.put("VIEW", "visualizar");
        actionDescriptionMap.put("MANAGE", "gestionar");
        actionDescriptionMap.put("MANAGEMENT", "gestionar");
        actionDescriptionMap.put("APPROVE", "aprobar");
        actionDescriptionMap.put("CANCEL", "cancelar");
        actionDescriptionMap.put("EXPORT", "exportar");
        actionDescriptionMap.put("ASSIGN", "asignar");
        actionDescriptionMap.put("CERTIFY", "certificar");
        actionDescriptionMap.put("FINANCIAL", "gestionar información financiera de");
        actionDescriptionMap.put("CONFIG", "configurar");

        // Find the module prefix and extract action
        String moduleSpanish = null;
        String actionVerb = null;

        for (Map.Entry<String, String> entry : MODULE_SPANISH_MAP.entrySet()) {
            if (permissionName.startsWith(entry.getKey())) {
                moduleSpanish = entry.getValue();
                // Extract the action part (everything after the module prefix)
                String actionPart = permissionName.substring(entry.getKey().length());
                actionVerb = actionDescriptionMap.getOrDefault(actionPart, actionPart.toLowerCase());
                break;
            }
        }

        // If no module prefix matched, try to parse it manually
        if (moduleSpanish == null) {
            String[] parts = permissionName.split("_");
            if (parts.length >= 2) {
                String lastPart = parts[parts.length - 1];
                actionVerb = actionDescriptionMap.getOrDefault(lastPart, lastPart.toLowerCase());

                // Build module name from remaining parts
                StringBuilder moduleBuilder = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    if (i > 0) moduleBuilder.append(" ");
                    moduleBuilder.append(parts[i].toLowerCase());
                }
                moduleSpanish = moduleBuilder.toString();
            } else {
                return "Permiso del sistema";
            }
        }

        // Adjust module name for proper Spanish grammar
        String adjustedModule = adjustModuleForSpanishDescription(moduleSpanish);

        return String.format("Permite %s %s", actionVerb, adjustedModule);
    }

    /**
     * Adjusts module names for proper Spanish grammar in descriptions.
     * Converts from nominative case to appropriate form after verbs.
     */
    private String adjustModuleForSpanishDescription(String module) {
        // Map specific modules to their proper Spanish forms in descriptions
        Map<String, String> adjustmentMap = new HashMap<>();
        adjustmentMap.put("Vacaciones de Empleados", "vacaciones de empleados");
        adjustmentMap.put("Empleados", "empleados");
        adjustmentMap.put("Acciones Disciplinarias", "acciones disciplinarias");
        adjustmentMap.put("Pagos de Salarios", "pagos de salarios");
        adjustmentMap.put("Entrega de EPP", "entregas de EPP");
        adjustmentMap.put("Vehículos", "vehículos");
        adjustmentMap.put("Reparaciones", "reparaciones");
        adjustmentMap.put("Pólizas de Seguro", "pólizas de seguro");
        adjustmentMap.put("Pagos de Patentes", "pagos de patentes");
        adjustmentMap.put("Estación de Servicios", "estaciones de servicios");
        adjustmentMap.put("Cargas de Combustible", "cargas de combustible");
        adjustmentMap.put("Inventario", "inventario");
        adjustmentMap.put("Stock", "stock");
        adjustmentMap.put("Proveedores", "proveedores");
        adjustmentMap.put("Proveedores de Servicios", "proveedores de servicios");
        adjustmentMap.put("Pagos de Servicios", "pagos de servicios");
        adjustmentMap.put("Pagos", "pagos");
        adjustmentMap.put("Documentos Transaccionales", "documentos transaccionales");
        adjustmentMap.put("Edificios", "edificios");
        adjustmentMap.put("Áreas de Proyecto", "áreas de proyecto");
        adjustmentMap.put("Reportes", "reportes");
        adjustmentMap.put("Sistema", "sistema");
        adjustmentMap.put("Empresa", "la empresa");

        return adjustmentMap.getOrDefault(module, module.toLowerCase());
    }
}

