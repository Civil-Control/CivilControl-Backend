package PSG.backEnd.service.implementation.forecast;

import PSG.backEnd.exception.forecast.BudgetForecastApplyException;
import PSG.backEnd.exception.forecast.BudgetForecastNotFoundException;
import PSG.backEnd.exception.forecast.BudgetForecastNotValidException;
import PSG.backEnd.exception.forecast.BudgetForecastTemplateNotFoundException;
import PSG.backEnd.model.dto.forecast.*;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.Supplier;
import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.entity.forecast.BudgetForecast;
import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.entity.forecast.BudgetForecastTemplate;
import PSG.backEnd.model.entity.forecast.BudgetForecastTemplateItem;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.entity.vehicle.Vehicle;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import PSG.backEnd.model.enums.forecast.BudgetForecastStatus;
import PSG.backEnd.model.mapper.forecast.BudgetForecastMapper;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import PSG.backEnd.repository.StockRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.repository.forecast.BudgetForecastItemRepository;
import PSG.backEnd.repository.forecast.BudgetForecastRepository;
import PSG.backEnd.repository.forecast.BudgetForecastTemplateRepository;
import PSG.backEnd.service.implementation.forecast.applier.AppliedEntityRef;
import PSG.backEnd.service.implementation.forecast.applier.BudgetForecastItemApplier;
import PSG.backEnd.service.implementation.forecast.applier.BudgetForecastItemApplierRegistry;
import PSG.backEnd.service.port.IBudgetForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio principal de previsiones de gastos (Feature 17).
 *
 * <h3>Reglas de estado</h3>
 * <ul>
 *   <li>BORRADOR: editable. Items pueden agregarse/modificarse/borrarse. NO se pueden aplicar.</li>
 *   <li>CONFIRMADA: items inmutables (monto/tipo/fecha/refs). SÍ se pueden aplicar/omitir/revertir.</li>
 *   <li>CERRADA: solo lectura. Se llega automáticamente cuando todos los items están APLICADO u OMITIDO.</li>
 * </ul>
 *
 * <h3>Aplicación</h3>
 * Cada item delega en un {@link BudgetForecastItemApplier} resuelto por tipo. El applier
 * crea el registro real (SalaryPayment / ServicePayment / Repair / StockPurchase) y
 * devuelve un {@link AppliedEntityRef} que se persiste en el item para trazabilidad.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BudgetForecastService implements IBudgetForecastService {

    private final BudgetForecastRepository forecastRepository;
    private final BudgetForecastItemRepository itemRepository;
    private final BudgetForecastTemplateRepository templateRepository;
    private final BudgetForecastMapper mapper;
    private final BudgetForecastItemValidator itemValidator;
    private final BudgetForecastItemApplierRegistry applierRegistry;

    // Repositorios de entidades referenciadas
    private final EmployeeRepository employeeRepository;
    private final SupplierRepository supplierRepository;
    private final ServiceAssignmentRepository serviceAssignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final StockRepository stockRepository;

    // Servicios secundarios
    private final BudgetForecastExcelService excelService;
    private final BudgetForecastPdfService pdfService;

    // ─────────────────────────── CRUD principal ───────────────────────────

    @Override
    public BudgetForecastResponseDTO create(BudgetForecastDTO dto) {
        validatePeriod(dto.periodFrom(), dto.periodTo());
        BudgetForecast forecast = mapper.toEntity(dto);
        forecast.setStatus(BudgetForecastStatus.BORRADOR);
        forecast.setTotalAmount(BigDecimal.ZERO);
        forecast.setAppliedAmount(BigDecimal.ZERO);
        forecast.setDeleted(false);

        if (dto.items() != null) {
            int order = 0;
            for (BudgetForecastItemDTO itemDTO : dto.items()) {
                itemValidator.validate(itemDTO);
                BudgetForecastItem item = buildItemFromDto(itemDTO, forecast);
                item.setRowOrder(itemDTO.rowOrder() != null ? itemDTO.rowOrder() : order++);
                forecast.getItems().add(item);
            }
        }
        recalculateTotals(forecast);
        BudgetForecast saved = forecastRepository.save(forecast);
        return mapper.toResponse(saved);
    }

    @Override
    public BudgetForecastResponseDTO update(Long id, BudgetForecastDTO dto) {
        BudgetForecast existing = loadActive(id);
        if (existing.getStatus() != BudgetForecastStatus.BORRADOR) {
            throw new BudgetForecastNotValidException(
                    "Solo se puede editar la cabecera/items en estado BORRADOR (estado actual: " + existing.getStatus() + ").");
        }
        if (dto.periodFrom() != null && dto.periodTo() != null) {
            validatePeriod(dto.periodFrom(), dto.periodTo());
        }
        mapper.partialUpdate(dto, existing);

        if (dto.items() != null) {
            // Reemplazo completo de items: estrategia simple — borra todos y reaplica.
            existing.getItems().clear();
            int order = 0;
            for (BudgetForecastItemDTO itemDTO : dto.items()) {
                itemValidator.validate(itemDTO);
                BudgetForecastItem item = buildItemFromDto(itemDTO, existing);
                item.setRowOrder(itemDTO.rowOrder() != null ? itemDTO.rowOrder() : order++);
                existing.getItems().add(item);
            }
        }
        recalculateTotals(existing);
        return mapper.toResponse(forecastRepository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetForecastResponseDTO getById(Long id) {
        return mapper.toResponse(loadActive(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetForecastSummaryDTO> getAll(BudgetForecastFilterDTO filter, Pageable pageable) {
        return forecastRepository.findAllWithFilters(
                filter.periodFromGte(),
                filter.periodToLte(),
                (filter.statuses() == null || filter.statuses().isEmpty()) ? null : filter.statuses(),
                filter.search(),
                filter.hasAppliedItems(),
                pageable
        ).map(mapper::toSummary);
    }

    @Override
    public void delete(Long id) {
        BudgetForecast existing = loadActive(id);
        if (existing.getAppliedAmount() != null && existing.getAppliedAmount().signum() > 0) {
            throw new BudgetForecastNotValidException(
                    "No se puede eliminar una previsión con items ya aplicados. Revertir primero.");
        }
        existing.setDeleted(true);
        forecastRepository.save(existing);
    }

    // ─────────────────────────── Estado ───────────────────────────

    @Override
    public BudgetForecastResponseDTO confirm(Long id) {
        BudgetForecast forecast = loadActive(id);
        if (forecast.getStatus() != BudgetForecastStatus.BORRADOR) {
            throw new BudgetForecastNotValidException("Solo se pueden confirmar previsiones en estado BORRADOR.");
        }
        if (forecast.getItems().isEmpty()) {
            throw new BudgetForecastNotValidException("No se puede confirmar una previsión sin items.");
        }
        forecast.setStatus(BudgetForecastStatus.CONFIRMADA);
        forecast.setConfirmedAt(LocalDateTime.now());
        forecast.setConfirmedByUserId(currentUserId());
        return mapper.toResponse(forecastRepository.save(forecast));
    }

    @Override
    public BudgetForecastResponseDTO reopen(Long id) {
        BudgetForecast forecast = loadActive(id);
        if (forecast.getStatus() != BudgetForecastStatus.CONFIRMADA) {
            throw new BudgetForecastNotValidException("Solo se pueden reabrir previsiones en estado CONFIRMADA.");
        }
        long appliedCount = itemRepository.countByBudgetForecastIdAndApplicationStatus(
                forecast.getId(), BudgetForecastItemApplicationStatus.APLICADO);
        if (appliedCount > 0) {
            throw new BudgetForecastNotValidException(
                    "No se puede reabrir: existen " + appliedCount + " items ya aplicados. Revertirlos primero.");
        }
        forecast.setStatus(BudgetForecastStatus.BORRADOR);
        forecast.setConfirmedAt(null);
        forecast.setConfirmedByUserId(null);
        return mapper.toResponse(forecastRepository.save(forecast));
    }

    // ─────────────────────────── Items ───────────────────────────

    @Override
    public BudgetForecastItemResponseDTO addItem(Long forecastId, BudgetForecastItemDTO itemDTO) {
        BudgetForecast forecast = loadActive(forecastId);
        ensureEditable(forecast);
        itemValidator.validate(itemDTO);
        int nextOrder = forecast.getItems().stream()
                .mapToInt(i -> i.getRowOrder() == null ? 0 : i.getRowOrder())
                .max().orElse(-1) + 1;
        BudgetForecastItem item = buildItemFromDto(itemDTO, forecast);
        item.setRowOrder(itemDTO.rowOrder() != null ? itemDTO.rowOrder() : nextOrder);
        forecast.getItems().add(item);
        recalculateTotals(forecast);
        forecastRepository.save(forecast);
        return mapper.toItemResponse(item);
    }

    @Override
    public BudgetForecastItemResponseDTO updateItem(Long forecastId, Long itemId, BudgetForecastItemDTO itemDTO) {
        BudgetForecast forecast = loadActive(forecastId);
        ensureEditable(forecast);
        BudgetForecastItem item = findItem(forecast, itemId);
        itemValidator.validate(itemDTO);

        item.setItemType(itemDTO.itemType());
        item.setDescription(itemDTO.description());
        item.setExpectedDate(itemDTO.expectedDate());
        item.setExpectedAmount(itemDTO.expectedAmount());
        if (itemDTO.rowOrder() != null) item.setRowOrder(itemDTO.rowOrder());
        item.setStockQuantity(itemDTO.stockQuantity());
        applyReferencesFromDto(item, itemDTO);

        recalculateTotals(forecast);
        forecastRepository.save(forecast);
        return mapper.toItemResponse(item);
    }

    @Override
    public void deleteItem(Long forecastId, Long itemId) {
        BudgetForecast forecast = loadActive(forecastId);
        ensureEditable(forecast);
        BudgetForecastItem item = findItem(forecast, itemId);
        forecast.getItems().remove(item);
        recalculateTotals(forecast);
        forecastRepository.save(forecast);
    }

    // ─────────────────────────── Aplicación ───────────────────────────

    @Override
    public BudgetForecastItemApplyResultDTO applyItem(Long forecastId, Long itemId) {
        BudgetForecast forecast = loadActive(forecastId);
        ensureApplicable(forecast);
        BudgetForecastItem item = findItem(forecast, itemId);
        return doApply(forecast, item);
    }

    @Override
    public BudgetForecastBatchApplyResponseDTO applyBatch(Long forecastId, BudgetForecastItemBatchApplyDTO batchDTO) {
        BudgetForecast forecast = loadActive(forecastId);
        ensureApplicable(forecast);
        boolean dryRun = Boolean.TRUE.equals(batchDTO.dryRun());

        List<BudgetForecastItemApplyResultDTO> results = new ArrayList<>();
        int ok = 0, fail = 0;
        for (Long itemId : batchDTO.itemIds()) {
            try {
                BudgetForecastItem item = findItem(forecast, itemId);
                if (dryRun) {
                    BudgetForecastItemApplier applier = applierRegistry.resolve(item.getItemType());
                    applier.validate(item);
                    results.add(BudgetForecastItemApplyResultDTO.ok(itemId, item.getApplicationStatus(), null, null));
                    ok++;
                } else {
                    BudgetForecastItemApplyResultDTO r = doApply(forecast, item);
                    results.add(r);
                    if (r.success()) ok++; else fail++;
                }
            } catch (Exception e) {
                log.warn("Error aplicando item {} de previsión {}: {}", itemId, forecastId, e.getMessage());
                results.add(BudgetForecastItemApplyResultDTO.error(itemId, e.getMessage()));
                fail++;
            }
        }
        return new BudgetForecastBatchApplyResponseDTO(batchDTO.itemIds().size(), ok, fail, results);
    }

    @Override
    public BudgetForecastItemResponseDTO revertItem(Long forecastId, Long itemId) {
        BudgetForecast forecast = loadActive(forecastId);
        BudgetForecastItem item = findItem(forecast, itemId);
        if (item.getApplicationStatus() != BudgetForecastItemApplicationStatus.APLICADO) {
            throw new BudgetForecastApplyException("Solo se pueden revertir items en estado APLICADO.");
        }
        BudgetForecastItemApplier applier = applierRegistry.resolve(item.getItemType());
        try {
            applier.revert(item);
        } catch (Exception ex) {
            throw new BudgetForecastApplyException("Error revirtiendo item: " + ex.getMessage(), ex);
        }
        item.setApplicationStatus(BudgetForecastItemApplicationStatus.PENDIENTE);
        item.setAppliedAt(null);
        item.setAppliedByUserId(null);
        item.setAppliedEntityType(null);
        item.setAppliedEntityId(null);

        // Si la previsión estaba CERRADA, vuelve a CONFIRMADA tras revertir
        if (forecast.getStatus() == BudgetForecastStatus.CERRADA) {
            forecast.setStatus(BudgetForecastStatus.CONFIRMADA);
            forecast.setClosedAt(null);
            forecast.setClosedByUserId(null);
        }
        recalculateTotals(forecast);
        forecastRepository.save(forecast);
        return mapper.toItemResponse(item);
    }

    @Override
    public BudgetForecastItemResponseDTO skipItem(Long forecastId, Long itemId, BudgetForecastItemSkipDTO skipDTO) {
        BudgetForecast forecast = loadActive(forecastId);
        ensureApplicable(forecast);
        BudgetForecastItem item = findItem(forecast, itemId);
        if (item.getApplicationStatus() == BudgetForecastItemApplicationStatus.APLICADO) {
            throw new BudgetForecastApplyException("No se puede omitir un item ya aplicado. Revertirlo primero.");
        }
        item.setApplicationStatus(BudgetForecastItemApplicationStatus.OMITIDO);
        item.setSkipReason(skipDTO.reason());
        maybeAutoClose(forecast);
        forecastRepository.save(forecast);
        return mapper.toItemResponse(item);
    }

    // ─────────────────────────── Instanciar desde plantilla ───────────────────────────

    @Override
    public BudgetForecastResponseDTO instantiateFromTemplate(BudgetForecastInstantiateDTO dto) {
        BudgetForecastTemplate template = templateRepository.findByIdAndDeletedFalse(dto.templateId())
                .orElseThrow(() -> new BudgetForecastTemplateNotFoundException(dto.templateId()));
        if (Boolean.FALSE.equals(template.getActive())) {
            throw new BudgetForecastNotValidException("La plantilla está inactiva: " + template.getName());
        }
        java.time.LocalDate periodFrom = dto.periodFrom();
        java.time.LocalDate periodTo = dto.periodTo() != null
                ? dto.periodTo()
                : periodFrom.plusDays(Math.max(0, template.getDefaultPeriodDays() - 1));
        validatePeriod(periodFrom, periodTo);

        BudgetForecast forecast = BudgetForecast.builder()
                .name(dto.nameOverride() != null && !dto.nameOverride().isBlank()
                        ? dto.nameOverride()
                        : template.getName() + " — " + periodFrom)
                .description(template.getDescription())
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .status(BudgetForecastStatus.BORRADOR)
                .totalAmount(BigDecimal.ZERO)
                .appliedAmount(BigDecimal.ZERO)
                .createdFromTemplateId(template.getId())
                .deleted(false)
                .build();

        int order = 0;
        for (BudgetForecastTemplateItem t : template.getItems()) {
            BudgetForecastItem item = BudgetForecastItem.builder()
                    .budgetForecast(forecast)
                    .rowOrder(t.getRowOrder() != null ? t.getRowOrder() : order++)
                    .itemType(t.getItemType())
                    .description(t.getDescription())
                    .expectedDate(periodFrom.plusDays(Math.max(0, t.getDayOffset())))
                    .expectedAmount(t.getExpectedAmount())
                    .employee(t.getEmployee())
                    .supplier(t.getSupplier())
                    .serviceAssignment(t.getServiceAssignment())
                    .vehicle(t.getVehicle())
                    .stock(t.getStock())
                    .stockQuantity(t.getStockQuantity())
                    .applicationStatus(BudgetForecastItemApplicationStatus.PENDIENTE)
                    .build();
            forecast.getItems().add(item);
        }
        recalculateTotals(forecast);
        return mapper.toResponse(forecastRepository.save(forecast));
    }

    // ─────────────────────────── Excel / PDF ───────────────────────────

    @Override
    public BudgetForecastImportResultDTO importItemsFromExcel(Long forecastId, MultipartFile file, boolean dryRun) {
        BudgetForecast forecast = loadActive(forecastId);
        ensureEditable(forecast);
        return excelService.importItems(forecast, file, dryRun);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(Long forecastId) {
        return excelService.export(loadActive(forecastId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(Long forecastId) {
        return pdfService.export(loadActive(forecastId));
    }

    // ─────────────────────────── Helpers ───────────────────────────

    private BudgetForecast loadActive(Long id) {
        return forecastRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BudgetForecastNotFoundException(id));
    }

    private BudgetForecastItem findItem(BudgetForecast forecast, Long itemId) {
        return forecast.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BudgetForecastNotFoundException(itemId));
    }

    private void ensureEditable(BudgetForecast forecast) {
        if (forecast.getStatus() != BudgetForecastStatus.BORRADOR) {
            throw new BudgetForecastNotValidException(
                    "Solo se pueden modificar items en estado BORRADOR. Estado actual: " + forecast.getStatus());
        }
    }

    private void ensureApplicable(BudgetForecast forecast) {
        if (forecast.getStatus() != BudgetForecastStatus.CONFIRMADA) {
            throw new BudgetForecastNotValidException(
                    "Solo se pueden aplicar items en previsiones CONFIRMADAS. Estado actual: " + forecast.getStatus());
        }
    }

    private void validatePeriod(java.time.LocalDate from, java.time.LocalDate to) {
        if (from == null || to == null) {
            throw new BudgetForecastNotValidException("periodFrom y periodTo son requeridos.");
        }
        if (to.isBefore(from)) {
            throw new BudgetForecastNotValidException("periodTo debe ser >= periodFrom.");
        }
    }

    /** Recalcula totalAmount y appliedAmount sumando items en memoria. */
    private void recalculateTotals(BudgetForecast forecast) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal applied = BigDecimal.ZERO;
        for (BudgetForecastItem i : forecast.getItems()) {
            BigDecimal amount = i.getExpectedAmount() != null ? i.getExpectedAmount() : BigDecimal.ZERO;
            total = total.add(amount);
            if (i.getApplicationStatus() == BudgetForecastItemApplicationStatus.APLICADO) {
                applied = applied.add(amount);
            }
        }
        forecast.setTotalAmount(total);
        forecast.setAppliedAmount(applied);
    }

    /** Auto-cierra cuando todos los items están APLICADO u OMITIDO. */
    private void maybeAutoClose(BudgetForecast forecast) {
        recalculateTotals(forecast);
        if (forecast.getItems().isEmpty()) return;
        boolean allDecided = forecast.getItems().stream()
                .allMatch(i -> i.getApplicationStatus() != BudgetForecastItemApplicationStatus.PENDIENTE);
        if (allDecided && forecast.getStatus() != BudgetForecastStatus.CERRADA) {
            forecast.setStatus(BudgetForecastStatus.CERRADA);
            forecast.setClosedAt(LocalDateTime.now());
            forecast.setClosedByUserId(currentUserId());
        }
    }

    private BudgetForecastItemApplyResultDTO doApply(BudgetForecast forecast, BudgetForecastItem item) {
        if (item.getApplicationStatus() == BudgetForecastItemApplicationStatus.APLICADO) {
            return BudgetForecastItemApplyResultDTO.error(item.getId(), "Item ya aplicado.");
        }
        if (item.getApplicationStatus() == BudgetForecastItemApplicationStatus.OMITIDO) {
            return BudgetForecastItemApplyResultDTO.error(item.getId(), "Item está OMITIDO; revertir omisión primero (volver a editar).");
        }
        BudgetForecastItemApplier applier = applierRegistry.resolve(item.getItemType());
        try {
            AppliedEntityRef ref = applier.apply(item);
            item.setApplicationStatus(BudgetForecastItemApplicationStatus.APLICADO);
            item.setAppliedEntityType(ref.entityType());
            item.setAppliedEntityId(ref.entityId());
            item.setAppliedAt(LocalDateTime.now());
            item.setAppliedByUserId(currentUserId());
            maybeAutoClose(forecast);
            forecastRepository.save(forecast);
            return BudgetForecastItemApplyResultDTO.ok(
                    item.getId(),
                    BudgetForecastItemApplicationStatus.APLICADO,
                    ref.entityType(),
                    ref.entityId());
        } catch (BudgetForecastApplyException e) {
            return BudgetForecastItemApplyResultDTO.error(item.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Fallo inesperado aplicando item {}", item.getId(), e);
            return BudgetForecastItemApplyResultDTO.error(item.getId(), "Error: " + e.getMessage());
        }
    }

    /** Construye un item nuevo a partir del DTO, resolviendo todas las FKs. */
    private BudgetForecastItem buildItemFromDto(BudgetForecastItemDTO dto, BudgetForecast parent) {
        BudgetForecastItem item = BudgetForecastItem.builder()
                .budgetForecast(parent)
                .itemType(dto.itemType())
                .description(dto.description())
                .expectedDate(dto.expectedDate())
                .expectedAmount(dto.expectedAmount())
                .stockQuantity(dto.stockQuantity())
                .applicationStatus(BudgetForecastItemApplicationStatus.PENDIENTE)
                .build();
        applyReferencesFromDto(item, dto);
        return item;
    }

    /** Resuelve y asigna todas las FKs del DTO al item. */
    private void applyReferencesFromDto(BudgetForecastItem item, BudgetForecastItemDTO dto) {
        item.setEmployee(resolveEmployee(dto.employeeId()));
        item.setSupplier(resolveSupplier(dto.supplierId()));
        item.setServiceAssignment(resolveAssignment(dto.serviceAssignmentId()));
        item.setVehicle(resolveVehicle(dto.vehicleId()));
        item.setStock(resolveStock(dto.stockId()));
    }

    private Employee resolveEmployee(Long id) {
        if (id == null) return null;
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BudgetForecastNotValidException("Empleado no encontrado: " + id));
    }

    private Supplier resolveSupplier(Long id) {
        if (id == null) return null;
        return supplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BudgetForecastNotValidException("Proveedor no encontrado: " + id));
    }

    private ServiceAssignment resolveAssignment(Long id) {
        if (id == null) return null;
        return serviceAssignmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BudgetForecastNotValidException("Asignación de servicio no encontrada: " + id));
    }

    private Vehicle resolveVehicle(Long id) {
        if (id == null) return null;
        return vehicleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BudgetForecastNotValidException("Vehículo no encontrado: " + id));
    }

    private Stock resolveStock(Long id) {
        if (id == null) return null;
        return stockRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BudgetForecastNotValidException("Stock no encontrado: " + id));
    }

    private static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) return null;
        return u.getId();
    }
}
