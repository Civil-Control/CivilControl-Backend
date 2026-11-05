package PSG.backEnd.model.enums.notification;

public enum NotificationSubjectType {
    VEHICLE_VTV        ("VTV de vehículo"),
    CHECK_PAYMENT      ("Cheque pendiente"),
    INSURANCE_POLICY   ("Póliza de seguro"),
    SERVICE_ASSIGNMENT ("Afectación de servicio"),
    WORK_CONTRACT      ("Contrato de obra"),
    CUSTOM_REMINDER    ("Recordatorio personalizado");

    private final String displayName;

    NotificationSubjectType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
