package PSG.backEnd.service.implementation.forecast;

import PSG.backEnd.exception.forecast.BudgetForecastNotValidException;
import PSG.backEnd.exception.forecast.BudgetForecastTemplateNotFoundException;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateItemDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateResponseDTO;
import PSG.backEnd.model.entity.forecast.BudgetForecastTemplate;
import PSG.backEnd.model.entity.forecast.BudgetForecastTemplateItem;
import PSG.backEnd.model.mapper.forecast.BudgetForecastTemplateMapper;
import PSG.backEnd.repository.EmployeeRepository;
import PSG.backEnd.repository.ServiceAssignmentRepository;
import PSG.backEnd.repository.StockRepository;
import PSG.backEnd.repository.SupplierRepository;
import PSG.backEnd.repository.VehicleRepository;
import PSG.backEnd.repository.forecast.BudgetForecastTemplateRepository;
import PSG.backEnd.service.port.IBudgetForecastTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de plantillas de previsión. Las plantillas son catálogos reutilizables que,
 * al instanciarse, generan una {@code BudgetForecast} BORRADOR con items pre-cargados.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BudgetForecastTemplateService implements IBudgetForecastTemplateService {

    private final BudgetForecastTemplateRepository templateRepository;
    private final BudgetForecastTemplateMapper mapper;
    private final BudgetForecastItemValidator itemValidator;

    private final EmployeeRepository employeeRepository;
    private final SupplierRepository supplierRepository;
    private final ServiceAssignmentRepository serviceAssignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final StockRepository stockRepository;

    @Override
    public BudgetForecastTemplateResponseDTO create(BudgetForecastTemplateDTO dto) {
        if (templateRepository.existsByNameIgnoreCaseAndDeletedFalse(dto.name())) {
            throw new BudgetForecastNotValidException("Ya existe una plantilla con el nombre: " + dto.name());
        }
        BudgetForecastTemplate template = mapper.toEntity(dto);
        template.setActive(dto.active() == null ? Boolean.TRUE : dto.active());
        template.setDeleted(false);

        if (dto.items() != null) {
            int order = 0;
            for (BudgetForecastTemplateItemDTO itemDTO : dto.items()) {
                validateTemplateItem(itemDTO);
                BudgetForecastTemplateItem item = buildItem(itemDTO, template);
                item.setRowOrder(itemDTO.rowOrder() != null ? itemDTO.rowOrder() : order++);
                template.getItems().add(item);
            }
        }
        return mapper.toResponse(templateRepository.save(template));
    }

    @Override
    public BudgetForecastTemplateResponseDTO update(Long id, BudgetForecastTemplateDTO dto) {
        BudgetForecastTemplate existing = loadActive(id);
        if (dto.name() != null && !dto.name().equalsIgnoreCase(existing.getName())
                && templateRepository.existsByNameIgnoreCaseAndDeletedFalse(dto.name())) {
            throw new BudgetForecastNotValidException("Ya existe otra plantilla con el nombre: " + dto.name());
        }
        mapper.partialUpdate(dto, existing);
        if (dto.active() != null) existing.setActive(dto.active());

        if (dto.items() != null) {
            existing.getItems().clear();
            int order = 0;
            for (BudgetForecastTemplateItemDTO itemDTO : dto.items()) {
                validateTemplateItem(itemDTO);
                BudgetForecastTemplateItem item = buildItem(itemDTO, existing);
                item.setRowOrder(itemDTO.rowOrder() != null ? itemDTO.rowOrder() : order++);
                existing.getItems().add(item);
            }
        }
        return mapper.toResponse(templateRepository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetForecastTemplateResponseDTO getById(Long id) {
        return mapper.toResponse(loadActive(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetForecastTemplateResponseDTO> getAll(Boolean active, String search, Pageable pageable) {
        return templateRepository.findAllWithFilters(active, search, pageable).map(mapper::toResponse);
    }

    @Override
    public void delete(Long id) {
        BudgetForecastTemplate t = loadActive(id);
        t.setDeleted(true);
        t.setActive(false);
        templateRepository.save(t);
    }

    private BudgetForecastTemplate loadActive(Long id) {
        return templateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BudgetForecastTemplateNotFoundException(id));
    }

    /** Adapta {@link BudgetForecastItemValidator} convirtiendo el template-item a DTO de previsión. */
    private void validateTemplateItem(BudgetForecastTemplateItemDTO t) {
        // El validator opera sobre BudgetForecastItemDTO; construimos un proxy temporal
        var proxy = new PSG.backEnd.model.dto.forecast.BudgetForecastItemDTO(
                null, t.rowOrder(), t.itemType(), t.description(),
                java.time.LocalDate.now(), t.expectedAmount(),
                t.employeeId(), t.supplierId(), t.serviceAssignmentId(),
                t.vehicleId(), t.stockId(), t.stockQuantity());
        itemValidator.validate(proxy);
    }

    private BudgetForecastTemplateItem buildItem(BudgetForecastTemplateItemDTO dto, BudgetForecastTemplate parent) {
        BudgetForecastTemplateItem item = mapper.toItemEntity(dto);
        item.setTemplate(parent);
        if (dto.employeeId() != null) {
            item.setEmployee(employeeRepository.findByIdAndDeletedFalse(dto.employeeId())
                    .orElseThrow(() -> new BudgetForecastNotValidException("Empleado no encontrado: " + dto.employeeId())));
        }
        if (dto.supplierId() != null) {
            item.setSupplier(supplierRepository.findByIdAndDeletedFalse(dto.supplierId())
                    .orElseThrow(() -> new BudgetForecastNotValidException("Proveedor no encontrado: " + dto.supplierId())));
        }
        if (dto.serviceAssignmentId() != null) {
            item.setServiceAssignment(serviceAssignmentRepository.findByIdAndDeletedFalse(dto.serviceAssignmentId())
                    .orElseThrow(() -> new BudgetForecastNotValidException("Asignación no encontrada: " + dto.serviceAssignmentId())));
        }
        if (dto.vehicleId() != null) {
            item.setVehicle(vehicleRepository.findByIdAndDeletedFalse(dto.vehicleId())
                    .orElseThrow(() -> new BudgetForecastNotValidException("Vehículo no encontrado: " + dto.vehicleId())));
        }
        if (dto.stockId() != null) {
            item.setStock(stockRepository.findByIdAndDeletedFalse(dto.stockId())
                    .orElseThrow(() -> new BudgetForecastNotValidException("Stock no encontrado: " + dto.stockId())));
        }
        return item;
    }
}
