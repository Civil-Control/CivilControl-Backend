package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.OrphanBiometricLog;
import PSG.backEnd.model.entity.employee.AttendanceRecord;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.enums.employee.MovementType;
import PSG.backEnd.repository.AttendanceRecordRepository;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.OrphanBiometricLogRepository;
import PSG.backEnd.service.port.IBiometricWebhookService;
import PSG.backEnd.service.util.TenantContext;
import PSG.backEnd.service.webhook.BiometricParseException;
import PSG.backEnd.service.webhook.BiometricParsedEvent;
import PSG.backEnd.service.webhook.BiometricPayloadParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the processing of raw biometric webhook payloads.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Resolve the brand-specific {@link BiometricPayloadParser}</li>
 *   <li>Parse the raw JSON → extract employee DNI and timestamp</li>
 *   <li>Resolve the employee and tenant via cross-tenant native query</li>
 *   <li>Set TenantContext → determine movement type → persist {@link AttendanceRecord}</li>
 *   <li>On parse failure or missing employee → route to {@link OrphanBiometricLog} dead-letter queue</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricWebhookService implements IBiometricWebhookService {

    private final List<BiometricPayloadParser> parsers;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final OrphanBiometricLogRepository orphanBiometricLogRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void process(String brand, String rawPayload) {
        String normalizedBrand = brand.toLowerCase().trim();

        // 1. Resolve parser
        BiometricPayloadParser parser = parsers.stream()
                .filter(p -> p.supports(normalizedBrand))
                .findFirst()
                .orElse(null);

        if (parser == null) {
            saveOrphan(normalizedBrand, rawPayload, "Unsupported clock brand: " + normalizedBrand);
            return;
        }

        // 2. Parse payload
        BiometricParsedEvent event;
        try {
            event = parser.parse(rawPayload);
        } catch (BiometricParseException e) {
            log.warn("Failed to parse {} payload: {}", normalizedBrand, e.getMessage());
            saveOrphan(normalizedBrand, rawPayload, e.getMessage());
            return;
        }

        // 3. Validate extracted data
        if (event.employeeDni() == null || event.employeeDni().isBlank()) {
            saveOrphan(normalizedBrand, rawPayload, "Parsed employee DNI is null or blank");
            return;
        }
        if (event.timestamp() == null) {
            saveOrphan(normalizedBrand, rawPayload, "Parsed timestamp is null");
            return;
        }

        // 4. Resolve employee (id + tenant_id) via cross-tenant native query
        EmployeeRef employeeRef = resolveEmployee(event.employeeDni());
        if (employeeRef == null) {
            log.warn("No active employee found with DNI {} — routing to orphan queue", event.employeeDni());
            saveOrphan(normalizedBrand, rawPayload,
                    "No active employee found with DNI: " + event.employeeDni());
            return;
        }

        // 5. Set tenant context so repositories work correctly
        Long previousTenant = TenantContext.getCurrentTenant();
        try {
            TenantContext.setCurrentTenant(employeeRef.tenantId());

            // Load the Employee entity (tenant filter now active for the correct tenant)
            Optional<Employee> employeeOpt = employeeRepository.findByIdAndDeletedFalse(employeeRef.employeeId());
            if (employeeOpt.isEmpty()) {
                saveOrphan(normalizedBrand, rawPayload,
                        "Employee not found after tenant resolution: DNI=" + event.employeeDni());
                return;
            }

            LocalDate date = event.timestamp().toLocalDate();
            LocalTime time = event.timestamp().toLocalTime();

            // 6. Determine movement type (toggle: first of day = ENTRADA, then alternate)
            MovementType movementType = resolveMovementType(employeeRef.employeeId(), date);

            // 7. Build and persist AttendanceRecord (idempotent via UNIQUE constraint)
            AttendanceRecord record = AttendanceRecord.builder()
                    .employee(employeeOpt.get())
                    .date(date)
                    .time(time)
                    .movementType(movementType)
                    .observation("Registro automático — reloj " + normalizedBrand)
                    .build();
            record.setTenantId(employeeRef.tenantId());

            try {
                attendanceRecordRepository.save(record);
                log.info("AttendanceRecord created: employeeId={}, date={}, time={}, type={}, brand={}",
                        employeeRef.employeeId(), date, time, movementType, normalizedBrand);
            } catch (DataIntegrityViolationException e) {
                // Duplicate record (same tenant + employee + date + time + movementType) — silently ignore
                log.debug("Duplicate attendance event ignored: employeeId={}, date={}, time={}",
                        employeeRef.employeeId(), date, time);
            }
        } finally {
            TenantContext.setCurrentTenant(previousTenant);
        }
    }

    /**
     * Resolves an employee's ID and tenant_id by DNI across all tenants using a native query.
     * Bypasses the Hibernate tenant filter to enable cross-tenant resolution.
     */
    private EmployeeRef resolveEmployee(String dni) {
        @SuppressWarnings("unchecked")
        List<Tuple> results = entityManager
                .createNativeQuery(
                        "SELECT id, tenant_id FROM employees WHERE dni = :dni AND deleted = false LIMIT 1",
                        Tuple.class)
                .setParameter("dni", dni)
                .getResultList();

        if (results.isEmpty()) return null;

        Tuple row = results.get(0);
        return new EmployeeRef(
                ((Number) row.get("id")).longValue(),
                ((Number) row.get("tenant_id")).longValue()
        );
    }

    /**
     * Determines the movement type for the next record using a toggle pattern:
     * If there are no records for the day, or the last record was SALIDA → ENTRADA.
     * If the last record was ENTRADA → SALIDA.
     */
    private MovementType resolveMovementType(Long employeeId, LocalDate date) {
        List<AttendanceRecord> dayRecords =
                attendanceRecordRepository.findByEmployeeIdAndDateOrderByTimeAsc(employeeId, date);

        if (dayRecords.isEmpty()) {
            return MovementType.ENTRADA;
        }

        AttendanceRecord lastRecord = dayRecords.get(dayRecords.size() - 1);
        return lastRecord.getMovementType() == MovementType.ENTRADA
                ? MovementType.SALIDA
                : MovementType.ENTRADA;
    }

    private void saveOrphan(String brand, String rawPayload, String errorMessage) {
        try {
            OrphanBiometricLog orphan = OrphanBiometricLog.builder()
                    .clockBrand(brand)
                    .rawPayload(rawPayload)
                    .errorMessage(errorMessage)
                    .receivedAt(LocalDateTime.now())
                    .build();
            orphanBiometricLogRepository.save(orphan);
            log.info("Orphan biometric payload saved: brand={}, error={}", brand, errorMessage);
        } catch (Exception e) {
            log.error("CRITICAL — Failed to save orphan biometric payload: {}", e.getMessage(), e);
        }
    }

    /** Internal record for cross-tenant employee resolution result. */
    private record EmployeeRef(Long employeeId, Long tenantId) {}
}
