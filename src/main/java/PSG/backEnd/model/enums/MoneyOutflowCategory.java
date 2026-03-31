package PSG.backEnd.model.enums;

/**
 * Enum representing the different categories of money outflows in the system.
 * Used for categorizing and filtering financial reports.
 */
public enum MoneyOutflowCategory {
    /**
     * Invoices and transactional documents from suppliers (following accrual accounting principle)
     */
    INVOICE("Factura de Proveedor"),

    /**
     * Salary payments to employees
     */
    SALARY("Pago de Salario"),

    /**
     * Utility service payments (electricity, water, gas, internet, etc.)
     */
    SERVICE("Pago de Servicio"),

    /**
     * Vehicle license plate payments
     */
    LICENCE_PLATE("Pago de Patente"),

    /**
     * Fuel loads for vehicles
     */
    FUEL("Carga de Combustible"),

    /**
     * Insurance policy payments
     */
    INSURANCE("Pago de Póliza"),

    /**
     * Vehicle repair costs
     */
    REPAIR("Reparación de Vehículo");

    private final String displayName;

    MoneyOutflowCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

