package PSG.backEnd.model.constants;

/**
 * Class that defines all available permissions in the system.
 * This is the "source of truth" for permissions - all permissions must be defined here.
 *
 * Permissions are automatically synchronized with the database at application startup via PermissionSeeder.
 * Format: MODULE_ACTION (e.g.: PROJECT_READ, EMPLOYEE_WRITE)
 *
 * Software Architecture: This architecture allows:
 * - Centralized permission control in code
 * - Type safety when referencing permissions
 * - Automatic synchronization with database
 * - Flexibility for administrators to create dynamic roles
 */
public final class AppPermissions {

    private AppPermissions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== MODULE: EMPLOYEES ====================
    
    /**
     * Module: Employees
     * Allows viewing employee information.
     */
    public static final String EMPLOYEE_READ = "EMPLOYEE_READ";
    
    /**
     * Module: Employees
     * Allows creating and editing employee information.
     */
    public static final String EMPLOYEE_WRITE = "EMPLOYEE_WRITE";
    
    /**
     * Module: Employees
     * Allows deleting employees.
     */
    public static final String EMPLOYEE_DELETE = "EMPLOYEE_DELETE";

    // ==================== MODULE: EMPLOYEE VACATIONS ====================
    
    /**
     * Module: Employee Vacations
     * Allows viewing employee vacation requests.
     */
    public static final String EMPLOYEE_VACATION_READ = "EMPLOYEE_VACATION_READ";
    
    /**
     * Module: Employee Vacations
     * Allows creating and editing vacation requests.
     */
    public static final String EMPLOYEE_VACATION_WRITE = "EMPLOYEE_VACATION_WRITE";
    
    /**
     * Module: Employee Vacations
     * Allows deleting vacation requests.
     */
    public static final String EMPLOYEE_VACATION_DELETE = "EMPLOYEE_VACATION_DELETE";

    // ==================== MODULE: DISCIPLINARY ACTIONS ====================
    
    /**
     * Module: Disciplinary Actions
     * Allows viewing disciplinary actions.
     */
    public static final String DISCIPLINARY_ACTION_READ = "DISCIPLINARY_ACTION_READ";
    
    /**
     * Module: Disciplinary Actions
     * Allows creating and editing disciplinary actions.
     */
    public static final String DISCIPLINARY_ACTION_WRITE = "DISCIPLINARY_ACTION_WRITE";
    
    /**
     * Module: Disciplinary Actions
     * Allows deleting disciplinary actions.
     */
    public static final String DISCIPLINARY_ACTION_DELETE = "DISCIPLINARY_ACTION_DELETE";

    // ==================== MODULE: SALARY PAYMENTS ====================
    
    /**
     * Module: Salary Payments
     * Allows viewing salary payments (sensitive information).
     */
    public static final String SALARY_PAYMENT_READ = "SALARY_PAYMENT_READ";
    
    /**
     * Module: Salary Payments
     * Allows creating and editing salary payments.
     */
    public static final String SALARY_PAYMENT_WRITE = "SALARY_PAYMENT_WRITE";
    
    /**
     * Module: Salary Payments
     * Allows deleting salary payments.
     */
    public static final String SALARY_PAYMENT_DELETE = "SALARY_PAYMENT_DELETE";

    // ==================== MODULE: EPP DELIVERIES ====================
    
    /**
     * Module: EPP Deliveries
     * Allows viewing EPP (Personal Protective Equipment) deliveries.
     */
    public static final String EPP_DELIVERY_READ = "EPP_DELIVERY_READ";
    
    /**
     * Module: EPP Deliveries
     * Allows creating and editing EPP deliveries.
     */
    public static final String EPP_DELIVERY_WRITE = "EPP_DELIVERY_WRITE";
    
    /**
     * Module: EPP Deliveries
     * Allows deleting EPP deliveries.
     */
    public static final String EPP_DELIVERY_DELETE = "EPP_DELIVERY_DELETE";

    // ==================== MODULE: VEHICLES ====================
    
    /**
     * Module: Vehicles
     * Allows viewing vehicle information.
     */
    public static final String VEHICLE_READ = "VEHICLE_READ";
    
    /**
     * Module: Vehicles
     * Allows creating and editing vehicles.
     */
    public static final String VEHICLE_WRITE = "VEHICLE_WRITE";
    
    /**
     * Module: Vehicles
     * Allows deleting vehicles.
     */
    public static final String VEHICLE_DELETE = "VEHICLE_DELETE";

    // ==================== MODULE: REPAIRS ====================
    
    /**
     * Module: Repairs
     * Allows viewing vehicle repairs.
     */
    public static final String REPAIR_READ = "REPAIR_READ";
    
    /**
     * Module: Repairs
     * Allows creating and editing repairs.
     */
    public static final String REPAIR_WRITE = "REPAIR_WRITE";
    
    /**
     * Module: Repairs
     * Allows deleting repairs.
     */
    public static final String REPAIR_DELETE = "REPAIR_DELETE";

    // ==================== MODULE: REPAIR ORDERS ====================

    /**
     * Module: Repair Orders
     * Allows field operators to create repair orders and manage their own.
     */
    public static final String REPAIR_ORDER_CREATE = "REPAIR_ORDER_CREATE";

    /**
     * Module: Repair Orders
     * Allows workshop staff to view all repair orders in the tenant.
     */
    public static final String REPAIR_ORDER_READ = "REPAIR_ORDER_READ";

    /**
     * Module: Repair Orders
     * Allows workshop staff to change order status and complete orders.
     */
    public static final String REPAIR_ORDER_WRITE = "REPAIR_ORDER_WRITE";

    // ==================== MODULE: INSURANCE POLICIES ====================
    
    /**
     * Module: Insurance Policies
     * Allows viewing insurance policies.
     */
    public static final String INSURANCE_POLICY_READ = "INSURANCE_POLICY_READ";
    
    /**
     * Module: Insurance Policies
     * Allows creating and editing insurance policies.
     */
    public static final String INSURANCE_POLICY_WRITE = "INSURANCE_POLICY_WRITE";
    
    /**
     * Module: Insurance Policies
     * Allows deleting insurance policies.
     */
    public static final String INSURANCE_POLICY_DELETE = "INSURANCE_POLICY_DELETE";

    // ==================== MODULE: LICENCE PLATE PAYMENTS ====================
    
    /**
     * Module: Licence Plate Payments
     * Allows viewing licence plate payments.
     */
    public static final String LICENCE_PLATE_PAYMENT_READ = "LICENCE_PLATE_PAYMENT_READ";
    
    /**
     * Module: Licence Plate Payments
     * Allows creating and editing licence plate payments.
     */
    public static final String LICENCE_PLATE_PAYMENT_WRITE = "LICENCE_PLATE_PAYMENT_WRITE";
    
    /**
     * Module: Licence Plate Payments
     * Allows deleting licence plate payments.
     */
    public static final String LICENCE_PLATE_PAYMENT_DELETE = "LICENCE_PLATE_PAYMENT_DELETE";

    // ==================== MODULE: GAS STATIONS ====================
    
    /**
     * Module: Gas Stations
     * Allows viewing gas stations.
     */
    public static final String GAS_STATION_READ = "GAS_STATION_READ";
    
    /**
     * Module: Gas Stations
     * Allows creating and editing gas stations.
     */
    public static final String GAS_STATION_WRITE = "GAS_STATION_WRITE";
    
    /**
     * Module: Gas Stations
     * Allows deleting gas stations.
     */
    public static final String GAS_STATION_DELETE = "GAS_STATION_DELETE";

    // ==================== MODULE: FUEL LOADS ====================
    
    /**
     * Module: Fuel Loads
     * Allows viewing fuel loads.
     */
    public static final String FUEL_LOAD_READ = "FUEL_LOAD_READ";
    
    /**
     * Module: Fuel Loads
     * Allows creating and editing fuel loads.
     */
    public static final String FUEL_LOAD_WRITE = "FUEL_LOAD_WRITE";
    
    /**
     * Module: Fuel Loads
     * Allows deleting fuel loads.
     */
    public static final String FUEL_LOAD_DELETE = "FUEL_LOAD_DELETE";

    // ==================== MODULE: INVENTORY (ITEMS & STOCK) ====================
    
    /**
     * Module: Inventory
     * Allows viewing items.
     */
    public static final String ITEM_READ = "ITEM_READ";
    
    /**
     * Module: Inventory
     * Allows creating and editing items.
     */
    public static final String ITEM_WRITE = "ITEM_WRITE";
    
    /**
     * Module: Inventory
     * Allows deleting items.
     */
    public static final String ITEM_DELETE = "ITEM_DELETE";
    
    /**
     * Module: Inventory
     * Allows viewing stock.
     */
    public static final String STOCK_READ = "STOCK_READ";
    
    /**
     * Module: Inventory
     * Allows creating and editing stock.
     */
    public static final String STOCK_WRITE = "STOCK_WRITE";
    
    /**
     * Module: Inventory
     * Allows deleting stock.
     */
    public static final String STOCK_DELETE = "STOCK_DELETE";

    // ==================== MODULE: STOCK PURCHASES ====================

    /**
     * Module: Stock Purchases
     * Allows viewing stock purchase records.
     */
    public static final String STOCK_PURCHASE_READ = "STOCK_PURCHASE_READ";

    /**
     * Module: Stock Purchases
     * Allows creating and editing stock purchases.
     */
    public static final String STOCK_PURCHASE_WRITE = "STOCK_PURCHASE_WRITE";

    /**
     * Module: Stock Purchases
     * Allows deleting stock purchases.
     */
    public static final String STOCK_PURCHASE_DELETE = "STOCK_PURCHASE_DELETE";

    // ==================== MODULE: SUPPLIERS ====================
    
    /**
     * Module: Suppliers
     * Allows viewing suppliers.
     */
    public static final String SUPPLIER_READ = "SUPPLIER_READ";
    
    /**
     * Module: Suppliers
     * Allows creating and editing suppliers.
     */
    public static final String SUPPLIER_WRITE = "SUPPLIER_WRITE";
    
    /**
     * Module: Suppliers
     * Allows deleting suppliers.
     */
    public static final String SUPPLIER_DELETE = "SUPPLIER_DELETE";

    // ==================== MODULE: SERVICE SUPPLIERS ====================
    
    /**
     * Module: Service Suppliers
     * Allows viewing service suppliers.
     */
    public static final String SERVICE_SUPPLIER_READ = "SERVICE_SUPPLIER_READ";
    
    /**
     * Module: Service Suppliers
     * Allows creating and editing service suppliers.
     */
    public static final String SERVICE_SUPPLIER_WRITE = "SERVICE_SUPPLIER_WRITE";
    
    /**
     * Module: Service Suppliers
     * Allows deleting service suppliers.
     */
    public static final String SERVICE_SUPPLIER_DELETE = "SERVICE_SUPPLIER_DELETE";

    // ==================== MODULE: SERVICE PAYMENTS ====================
    
    /**
     * Module: Service Payments
     * Allows viewing service payments.
     */
    public static final String SERVICE_PAYMENT_READ = "SERVICE_PAYMENT_READ";
    
    /**
     * Module: Service Payments
     * Allows creating and editing service payments.
     */
    public static final String SERVICE_PAYMENT_WRITE = "SERVICE_PAYMENT_WRITE";
    
    /**
     * Module: Service Payments
     * Allows deleting service payments.
     */
    public static final String SERVICE_PAYMENT_DELETE = "SERVICE_PAYMENT_DELETE";

    // ==================== MODULE: PAYMENTS ====================
    
    /**
     * Module: Payments
     * Allows viewing general payments.
     */
    public static final String PAYMENT_READ = "PAYMENT_READ";
    
    /**
     * Module: Payments
     * Allows creating and editing payments.
     */
    public static final String PAYMENT_WRITE = "PAYMENT_WRITE";
    
    /**
     * Module: Payments
     * Allows deleting payments.
     */
    public static final String PAYMENT_DELETE = "PAYMENT_DELETE";

    // ==================== MODULE: TRANSACTIONAL DOCUMENTS ====================
    
    /**
     * Module: Transactional Documents
     * Allows viewing transactional documents.
     */
    public static final String TRANSACTIONAL_DOCUMENT_READ = "TRANSACTIONAL_DOCUMENT_READ";
    
    /**
     * Module: Transactional Documents
     * Allows creating and editing transactional documents.
     */
    public static final String TRANSACTIONAL_DOCUMENT_WRITE = "TRANSACTIONAL_DOCUMENT_WRITE";
    
    /**
     * Module: Transactional Documents
     * Allows deleting transactional documents.
     */
    public static final String TRANSACTIONAL_DOCUMENT_DELETE = "TRANSACTIONAL_DOCUMENT_DELETE";

    // ==================== MODULE: ATTENDANCE RECORDS ====================

    /**
     * Module: Attendance Records
     * Allows viewing attendance records (clock-in/clock-out).
     */
    public static final String ATTENDANCE_RECORD_READ = "ATTENDANCE_RECORD_READ";

    /**
     * Module: Attendance Records
     * Allows creating and editing attendance records.
     */
    public static final String ATTENDANCE_RECORD_WRITE = "ATTENDANCE_RECORD_WRITE";

    /**
     * Module: Attendance Records
     * Allows deleting attendance records.
     */
    public static final String ATTENDANCE_RECORD_DELETE = "ATTENDANCE_RECORD_DELETE";

    // ==================== MODULE: BUILDINGS ====================
    
    /**
     * Module: Buildings
     * Allows viewing buildings.
     */
    public static final String BUILDING_READ = "BUILDING_READ";
    
    /**
     * Module: Buildings
     * Allows creating and editing buildings.
     */
    public static final String BUILDING_WRITE = "BUILDING_WRITE";
    
    /**
     * Module: Buildings
     * Allows deleting buildings.
     */
    public static final String BUILDING_DELETE = "BUILDING_DELETE";

    // ==================== MODULE: PROJECT AREAS ====================
    
    /**
     * Module: Project Areas
     * Allows viewing project areas.
     */
    public static final String PROJECT_AREA_READ = "PROJECT_AREA_READ";
    
    /**
     * Module: Project Areas
     * Allows creating and editing project areas.
     */
    public static final String PROJECT_AREA_WRITE = "PROJECT_AREA_WRITE";
    
    /**
     * Module: Project Areas
     * Allows deleting project areas.
     */
    public static final String PROJECT_AREA_DELETE = "PROJECT_AREA_DELETE";

    // ==================== MODULE: REPORTS ====================
    
    /**
     * Module: Reports
     * Allows viewing basic reports.
     */
    public static final String REPORT_VIEW = "REPORT_VIEW";
    
    /**
     * Module: Reports
     * Allows generating financial reports (sensitive information).
     */
    public static final String REPORT_FINANCIAL = "REPORT_FINANCIAL";
    
    /**
     * Module: Reports
     * Allows exporting reports.
     */
    public static final String REPORT_EXPORT = "REPORT_EXPORT";

    // ==================== MODULE: TENANTS ====================
    
    /**
     * Module: Tenants
     * Allows viewing tenant (company) information.
     */
    public static final String TENANT_READ = "TENANT_READ";
    
    /**
     * Module: Tenants
     * Allows creating and editing tenant information.
     */
    public static final String TENANT_WRITE = "TENANT_WRITE";
    
    /**
     * Module: Tenants
     * Allows deleting tenants.
     */
    public static final String TENANT_DELETE = "TENANT_DELETE";

    // ==================== MODULE: CLIENTS ====================
    public static final String CLIENT_READ = "CLIENT_READ";
    public static final String CLIENT_WRITE = "CLIENT_WRITE";
    public static final String CLIENT_DELETE = "CLIENT_DELETE";

    // ==================== MODULE: SALES DOCUMENTS ====================
    public static final String SALES_DOCUMENT_READ = "SALES_DOCUMENT_READ";
    public static final String SALES_DOCUMENT_WRITE = "SALES_DOCUMENT_WRITE";
    public static final String SALES_DOCUMENT_DELETE = "SALES_DOCUMENT_DELETE";

    // ==================== MODULE: WORK CONTRACTS ====================
    public static final String WORK_CONTRACT_READ = "WORK_CONTRACT_READ";
    public static final String WORK_CONTRACT_WRITE = "WORK_CONTRACT_WRITE";
    public static final String WORK_CONTRACT_DELETE = "WORK_CONTRACT_DELETE";

    // ==================== MODULE: CERTIFICATIONS ====================
    public static final String CERTIFICATION_READ = "CERTIFICATION_READ";
    public static final String CERTIFICATION_WRITE = "CERTIFICATION_WRITE";
    public static final String CERTIFICATION_DELETE = "CERTIFICATION_DELETE";

    // ==================== MODULE: SYSTEM ====================
    
    /**
     * Module: System
     * Allows exporting system data.
     */
    public static final String DATA_EXPORT = "DATA_EXPORT";
    
    /**
     * Module: System
     * Allows managing system users.
     */
    public static final String USER_MANAGEMENT = "USER_MANAGEMENT";
    
    /**
     * Module: System
     * Allows managing roles and permissions.
     */
    public static final String ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    
    /**
     * Module: System
     * Allows accessing system configuration.
     */
    public static final String SYSTEM_CONFIG = "SYSTEM_CONFIG";
    
    /**
     * Module: System
     * Allows viewing system audit logs.
     */
    public static final String AUDIT_VIEW = "AUDIT_VIEW";

    /**
     * Module: System
     * Allows viewing exception logs for system monitoring and debugging.
     */
    public static final String EXCEPTION_LOG_READ = "EXCEPTION_LOG_READ";

    /**
     * Module: System
     * Allows managing exception logs (cleanup, deletion).
     */
    public static final String EXCEPTION_LOG_WRITE = "EXCEPTION_LOG_WRITE";
}

