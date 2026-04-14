package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.report.InvalidReportFilterException;
import PSG.backEnd.exception.report.InvalidReportFormatException;
import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.MoneyOutflowReportPreviewDTO;
import PSG.backEnd.model.dto.report.ReportFilterDTO;
import PSG.backEnd.model.dto.report.ReportItemDTO;
import PSG.backEnd.model.dto.report.salary.*;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.StockPurchase;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.entity.insurance.PolicyPayment;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.enums.MoneyOutflowCategory;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ReportFormat;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.entity.vehicle.RepairItem;
import PSG.backEnd.model.enums.vehicle.RepairItemType;
import PSG.backEnd.repository.*;
import PSG.backEnd.repository.PaymentRepository.PaymentRepository;
import PSG.backEnd.service.export.IReportExporter;
import PSG.backEnd.service.export.SalaryReportExcelExporter;
import PSG.backEnd.service.export.SalaryReportPdfExporter;
import PSG.backEnd.service.port.IReportService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of IReportService for generating money outflow reports.
 * Orchestrates data collection from multiple sources and delegates export to specific exporters.
 *
 * Follows SOLID principles:
 * - Single Responsibility: Only handles report generation logic
 * - Open/Closed: Can be extended with new exporters without modification
 * - Dependency Inversion: Depends on abstractions (IReportExporter)
 */
@Service
@Slf4j
public class ReportService implements IReportService {

    // Map of exporters by format (Strategy Pattern)
    private final Map<ReportFormat, IReportExporter> exporters;

    // Salary report exporters
    private final SalaryReportExcelExporter salaryExcelExporter;
    private final SalaryReportPdfExporter salaryPdfExporter;

    // Repositories for data collection
    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final ServicePaymentRepository servicePaymentRepository;
    private final FuelLoadRepository fuelLoadRepository;
    private final PolicyPaymentRepository policyPaymentRepository;
    private final RepairRepository repairRepository;
    private final StockPurchaseRepository stockPurchaseRepository;
    private final StockRepository stockRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final PaymentRepository paymentRepository;
    private final MessageSourceHelper messageSourceHelper;

    /**
     * Constructor that automatically maps exporters by their format.
     * Spring will inject all implementations of IReportExporter and all repositories.
     */
    public ReportService(
            List<IReportExporter> exporterList,
            SalaryReportExcelExporter salaryExcelExporter,
            SalaryReportPdfExporter salaryPdfExporter,
            TransactionalDocumentRepository transactionalDocumentRepository,
            SalaryPaymentRepository salaryPaymentRepository,
            ServicePaymentRepository servicePaymentRepository,
            FuelLoadRepository fuelLoadRepository,
            PolicyPaymentRepository policyPaymentRepository,
            RepairRepository repairRepository,
            StockPurchaseRepository stockPurchaseRepository,
            StockRepository stockRepository,
            ProjectAreaRepository projectAreaRepository,
            PaymentRepository paymentRepository,
            MessageSourceHelper messageSourceHelper) {

        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(
                        IReportExporter::getFormat,
                        exporter -> exporter
                ));

        this.salaryExcelExporter = salaryExcelExporter;
        this.salaryPdfExporter = salaryPdfExporter;
        this.transactionalDocumentRepository = transactionalDocumentRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.servicePaymentRepository = servicePaymentRepository;
        this.fuelLoadRepository = fuelLoadRepository;
        this.policyPaymentRepository = policyPaymentRepository;
        this.repairRepository = repairRepository;
        this.stockPurchaseRepository = stockPurchaseRepository;
        this.stockRepository = stockRepository;
        this.projectAreaRepository = projectAreaRepository;
        this.paymentRepository = paymentRepository;
        this.messageSourceHelper = messageSourceHelper;

        log.info("ReportService initialized with {} exporters: {}",
                 exporters.size(),
                 exporters.keySet());
    }

    @Override
    @Transactional(readOnly = true)
    public MoneyOutflowReportDTO generateMoneyOutflowReport(ReportFilterDTO filters) {
        log.info("Generating money outflow report with filters: {}", filters);

        // Validate filters
        validateFilters(filters);

        // Collect data from all sources
        List<ReportItemDTO> items = collectMoneyOutflows(filters);

        // Apply sorting
        items = applySorting(items, filters);

        // Calculate totals and summary
        BigDecimal totalAmount = calculateTotalAmount(items);
        Map<MoneyOutflowCategory, BigDecimal> summaryByCategory = calculateSummaryByCategory(items);

        // Calculate duplicated amount (items linked to an invoice whose amount is already in the invoice total)
        BigDecimal duplicatedAmount = calculateDuplicatedAmount(items);

        // Get project area name if filtered
        String projectAreaName = getProjectAreaName(filters.projectAreaIds());

        // Build report
        MoneyOutflowReportDTO report = MoneyOutflowReportDTO.builder()
                .filters(filters)
                .items(items)
                .totalAmount(totalAmount)
                .totalCount(items.size())
                .generatedAt(LocalDateTime.now())
                .summaryByCategory(summaryByCategory)
                .reportName("Reporte de Salidas de Dinero")
                .periodDescription(buildPeriodDescription(filters))
                .projectAreaName(projectAreaName)
                .duplicatedAmount(duplicatedAmount)
                .build();

        log.info("Report generated successfully with {} items, total amount: ${}",
                 items.size(), totalAmount);

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateMoneyOutflowReportFile(ReportFilterDTO filters, ReportFormat format) {
        log.info("Generating money outflow report file in format: {}", format);

        // Get the appropriate exporter
        IReportExporter exporter = exporters.get(format);
        if (exporter == null) {
            throw new InvalidReportFormatException(format.name());
        }

        // Generate report data
        MoneyOutflowReportDTO report = generateMoneyOutflowReport(filters);

        // Export to file
        byte[] fileContent = exporter.export(report);

        // Build filename
        String filename = buildFilename(filters, exporter.getFileExtension());

        // Build response with headers
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, exporter.getContentType());

        log.info("Report file generated successfully: {}", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }

    @Override
    @Transactional(readOnly = true)
    public MoneyOutflowReportPreviewDTO generateMoneyOutflowReportPreview(ReportFilterDTO filters) {
        log.info("Generating money outflow report preview with filters: {}", filters);

        // Validate filters
        validateFilters(filters);

        // Collect data from all sources (we still need to collect items to calculate statistics)
        List<ReportItemDTO> items = collectMoneyOutflows(filters);

        // Calculate totals and summary
        BigDecimal totalAmount = calculateTotalAmount(items);
        BigDecimal averageAmount = items.isEmpty()
                ? BigDecimal.ZERO
                : totalAmount.divide(BigDecimal.valueOf(items.size()), 2, java.math.RoundingMode.HALF_UP);

        Map<MoneyOutflowCategory, BigDecimal> summaryByCategory = calculateSummaryByCategory(items);
        Map<MoneyOutflowCategory, Integer> countByCategory = calculateCountByCategory(items);

        // Calculate duplicated amount
        BigDecimal duplicatedAmount = calculateDuplicatedAmount(items);

        // Get project area name if filtered
        String projectAreaName = getProjectAreaName(filters.projectAreaIds());

        // Build preview (without items list)
        MoneyOutflowReportPreviewDTO preview = MoneyOutflowReportPreviewDTO.builder()
                .filters(filters)
                .totalAmount(totalAmount)
                .totalCount(items.size())
                .averageAmount(averageAmount)
                .generatedAt(LocalDateTime.now())
                .summaryByCategory(summaryByCategory)
                .countByCategory(countByCategory)
                .periodDescription(buildPeriodDescription(filters))
                .projectAreaName(projectAreaName)
                .duplicatedAmount(duplicatedAmount)
                .build();

        log.info("Preview generated successfully: {} items, total amount: ${}, average: ${}",
                 items.size(), totalAmount, averageAmount);

        return preview;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportItemDTO> collectMoneyOutflows(ReportFilterDTO filters) {
        log.debug("Collecting money outflows with filters: {}", filters);

        List<Long> areaIds = filters.projectAreaIds();
        boolean hasAreaFilter = areaIds != null && !areaIds.isEmpty();

        // For multiple areas: collect per area and deduplicate by item ID
        if (hasAreaFilter && areaIds.size() > 1) {
            Map<Long, ReportItemDTO> merged = new LinkedHashMap<>();
            for (Long areaId : areaIds) {
                ReportFilterDTO singleAreaFilters = new ReportFilterDTO(
                        filters.startDate(), filters.endDate(), filters.categories(),
                        List.of(areaId), filters.minAmount(), filters.maxAmount(),
                        filters.sortBy(), filters.sortOrder()
                );
                collectMoneyOutflows(singleAreaFilters).forEach(item -> merged.put(item.id(), item));
            }
            log.debug("Collected {} money outflow items across {} areas", merged.size(), areaIds.size());
            return new ArrayList<>(merged.values());
        }

        List<ReportItemDTO> allItems = new ArrayList<>();

        // Check if categories filter is empty or null (means include all)
        boolean includeAll = filters.categories() == null || filters.categories().isEmpty();

        // Check if projectAreaIds filter is present - InsurancePolicy must be excluded in this case
        boolean excludeInsurance = hasAreaFilter;

        // Collect from all sources based on categories filter
        if (includeAll || filters.categories().contains(MoneyOutflowCategory.INVOICE)) {
            allItems.addAll(collectFromInvoices(filters));
        }

        if (includeAll || filters.categories().contains(MoneyOutflowCategory.SALARY)) {
            allItems.addAll(collectFromSalaryPayments(filters));
        }

        if (includeAll || filters.categories().contains(MoneyOutflowCategory.SERVICE)) {
            allItems.addAll(collectFromServicePayments(filters));
        }

        if (includeAll || filters.categories().contains(MoneyOutflowCategory.LICENCE_PLATE)) {
            allItems.addAll(collectFromLicencePlatePayments(filters));
        }

        if (includeAll || filters.categories().contains(MoneyOutflowCategory.FUEL)) {
            allItems.addAll(collectFromFuelLoads(filters));
        }

        // IMPORTANT: Insurance policies are excluded when projectAreaIds filter is applied
        // because insurance policies don't have a relationship with project areas
        if (!excludeInsurance && (includeAll || filters.categories().contains(MoneyOutflowCategory.INSURANCE))) {
            allItems.addAll(collectFromInsurances(filters));
        } else if (excludeInsurance && filters.categories() != null && filters.categories().contains(MoneyOutflowCategory.INSURANCE)) {
            log.debug("Insurance category requested but projectAreaIds filter is present - excluding insurance policies");
        }

        if (includeAll || filters.categories().contains(MoneyOutflowCategory.REPAIR)) {
            allItems.addAll(collectFromRepairs(filters));
        }

        if (includeAll || filters.categories().contains(MoneyOutflowCategory.STOCK_PURCHASE)) {
            allItems.addAll(collectFromStockPurchases(filters));
        }

        log.debug("Collected {} money outflow items", allItems.size());

        return allItems;
    }

    /**
     * Validates the filter criteria.
     */
    private void validateFilters(ReportFilterDTO filters) {
        if (filters == null) {
            throw new InvalidReportFilterException(messageSourceHelper.getMessage("report.filters.null"));
        }

        if (!filters.isValidDateRange()) {
            throw InvalidReportFilterException.invalidDateRange();
        }

        if (!filters.isValidAmountRange()) {
            throw InvalidReportFilterException.invalidAmountRange();
        }
    }



    /**
     * Applies sorting to the items based on filter criteria.
     */
    private List<ReportItemDTO> applySorting(List<ReportItemDTO> items, ReportFilterDTO filters) {
        Comparator<ReportItemDTO> comparator = getSortComparator(filters.sortBy());

        if ("desc".equalsIgnoreCase(filters.sortOrder())) {
            comparator = comparator.reversed();
        }

        return items.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * Returns the appropriate comparator based on the sort field.
     */
    private Comparator<ReportItemDTO> getSortComparator(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "amount" -> Comparator.comparing(ReportItemDTO::amount);
            case "category" -> Comparator.comparing(item -> item.category().name());
            case "beneficiary" -> Comparator.comparing(
                    item -> item.beneficiary() != null ? item.beneficiary() : "",
                    String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(ReportItemDTO::date);
        };
    }

    /**
     * Calculates the total amount of all items.
     */
    private BigDecimal calculateTotalAmount(List<ReportItemDTO> items) {
        return items.stream()
                .map(ReportItemDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the summary of amounts grouped by category.
     */
    private Map<MoneyOutflowCategory, BigDecimal> calculateSummaryByCategory(List<ReportItemDTO> items) {
        return items.stream()
                .collect(Collectors.groupingBy(
                        ReportItemDTO::category,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                ReportItemDTO::amount,
                                BigDecimal::add
                        )
                ));
    }

    /**
     * Calculates the count of items grouped by category.
     */
    private Map<MoneyOutflowCategory, Integer> calculateCountByCategory(List<ReportItemDTO> items) {
        return items.stream()
                .collect(Collectors.groupingBy(
                        ReportItemDTO::category,
                        Collectors.summingInt(item -> 1)
                ));
    }

    /**
     * Builds a human-readable period description.
     */
    private String buildPeriodDescription(ReportFilterDTO filters) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        if (filters.startDate() != null && filters.endDate() != null) {
            return String.format("Período: %s - %s",
                    filters.startDate().format(formatter),
                    filters.endDate().format(formatter));
        } else if (filters.startDate() != null) {
            return String.format("Desde: %s", filters.startDate().format(formatter));
        } else if (filters.endDate() != null) {
            return String.format("Hasta: %s", filters.endDate().format(formatter));
        } else {
            return "Todos los períodos";
        }
    }

    /**
     * Builds the filename for the exported report.
     */
    private String buildFilename(ReportFilterDTO filters, String extension) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder filename = new StringBuilder("reporte_salidas");

        if (filters.startDate() != null) {
            filename.append("_").append(filters.startDate().format(formatter));
        }

        if (filters.endDate() != null) {
            filename.append("_").append(filters.endDate().format(formatter));
        }

        if (filters.categories() != null && !filters.categories().isEmpty()) {
            String categoriesStr = filters.categories().stream()
                    .map(c -> c.name().toLowerCase())
                    .collect(Collectors.joining("-"));
            filename.append("_").append(categoriesStr);
        }

        filename.append(extension);

        return filename.toString();
    }

    // ==================== DATA COLLECTION METHODS BY CATEGORY ====================

    /**
     * Collects money outflow items from TransactionalDocument entities (Invoices).
     * Following accrual accounting principle - using invoice date instead of payment date.
     */
    private List<ReportItemDTO> collectFromInvoices(ReportFilterDTO filters) {
        log.debug("Collecting from transactional documents (invoices)");

        Pageable pageable = PageRequest.of(0, 10000); // Get all records

        var documents = transactionalDocumentRepository.findAllWithFilters(
                null, // documentNumber
                null, // documentType
                null, // supplierCuit
                null, // supplierName
                getEffectiveAreaId(filters), // projectAreaId - from filter
                null, // projectAreaName
                filters.maxAmount(), // maxTotalAmount
                filters.minAmount(), // minTotalAmount
                null, // totalAmount
                filters.startDate(), // fromDate
                filters.endDate(), // toDate
                null, // paid
                null, // search
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (TransactionalDocument doc : documents) {
            String description = buildInvoiceDescription(doc);
            String reference = buildInvoiceReference(doc);

            items.add(ReportItemDTO.builder()
                    .id(doc.getId())
                    .date(doc.getDate()) // Fecha de factura (devengado)
                    .category(MoneyOutflowCategory.INVOICE)
                    .description(description)
                    .amount(doc.getTotal())
                    .paymentMethod(doc.getPaid()
                            ? paymentRepository.findPaymentMethodByDocumentId(doc.getId()).orElse(null)
                            : "(Pendiente)")
                    .beneficiary(doc.getSupplier().getLegalName())
                    .reference(reference)
                    .comment(doc.getComment())
                    .projectAreaName(doc.getProjectArea() != null ? doc.getProjectArea().getName() : null)
                    .linkedDocumentId(null)
                    .build());
        }

        log.debug("Collected {} invoice items", items.size());
        return items;
    }

    /**
     * Builds a description for an invoice.
     */
    private String buildInvoiceDescription(TransactionalDocument doc) {
        String typeName = doc.getDocumentType().getDisplayName();
        String supplierName = doc.getSupplier().getLegalName();

        StringBuilder description = new StringBuilder();
        description.append(typeName);
        description.append(" - ").append(supplierName);

        if (doc.getProjectArea() != null) {
            description.append(" (").append(doc.getProjectArea().getName()).append(")");
        }

        return description.toString();
    }

    /**
     * Builds a reference string for an invoice.
     */
    private String buildInvoiceReference(TransactionalDocument doc) {
        return doc.getBranchCode() + "-" + doc.getDocumentNumber();
    }

    /**
     * Collects money outflow items from SalaryPayment entities.
     */
    private List<ReportItemDTO> collectFromSalaryPayments(ReportFilterDTO filters) {
        log.debug("Collecting from salary payments");

        Pageable pageable = PageRequest.of(0, 10000);

        var salaryPayments = salaryPaymentRepository.findAllWithFilters(
                null, // employeeId
                null, // firstName
                null, // lastName
                null, // salaryFrequency
                getEffectiveAreaId(filters), // projectAreaId - from filter
                filters.startDate(),
                filters.endDate(),
                filters.minAmount(),
                filters.maxAmount(),
                null, // paymentMethod - not filtered in reports
                null, // search
                null, // transactionalDocumentId
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (SalaryPayment sp : salaryPayments) {
            String employeeName = sp.getEmployee().getLastName() + " " + sp.getEmployee().getName();

            Long linkedDocId = sp.getTransactionalDocument() != null ? sp.getTransactionalDocument().getId() : null;

            items.add(ReportItemDTO.builder()
                    .id(sp.getId())
                    .date(sp.getPaymentDate())
                    .category(MoneyOutflowCategory.SALARY)
                    .description("Pago de salario - " + sp.getSalaryFrequency().getDisplayName())
                    .amount(sp.getAmount())
                    .paymentMethod(sp.getPaymentMethod() != null ? sp.getPaymentMethod().getDisplayName() : null)
                    .beneficiary(employeeName)
                    .reference("Salario " + sp.getPaymentDate().getMonthValue() + "/" + sp.getPaymentDate().getYear())
                    .comment(null)
                    .projectAreaName(sp.getEmployee().getProjectArea() != null ? sp.getEmployee().getProjectArea().getName() : null)
                    .linkedDocumentId(linkedDocId)
                    .build());
        }

        log.debug("Collected {} salary payment items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from ServicePayment entities.
     */
    private List<ReportItemDTO> collectFromServicePayments(ReportFilterDTO filters) {
        log.debug("Collecting from service payments");

        Pageable pageable = PageRequest.of(0, 10000);

        var servicePayments = servicePaymentRepository.findAllWithFilters(
                SubjectType.BUILDING, // only building-based payments
                null, // serviceAssignmentId
                null, // serviceSupplierId
                null, // buildingId
                getEffectiveAreaId(filters), // projectAreaId - from filter
                null, // serviceType
                null, // vehicleId
                null, // year
                null, // period
                filters.startDate(),
                filters.endDate(),
                filters.minAmount(),
                filters.maxAmount(),
                null, // referenceNumber
                null, // supplierName
                null, // search
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (ServicePayment sp : servicePayments) {
            var assignment = sp.getServiceAssignment();
            String serviceTypeName = assignment != null && assignment.getServiceType() != null
                    ? assignment.getServiceType().getDisplayName() : "Servicio";
            String buildingName = assignment != null && assignment.getBuilding() != null
                    ? assignment.getBuilding().getName() : "";
            String description = "Pago de servicio: " + serviceTypeName + " - " + buildingName;

            String beneficiary = assignment != null && assignment.getServiceSupplier() != null
                    && assignment.getServiceSupplier().getSupplier() != null
                    ? assignment.getServiceSupplier().getSupplier().getLegalName() : "";

            String projectAreaName = sp.getProjectArea() != null
                    ? sp.getProjectArea().getName()
                    : null;

            items.add(ReportItemDTO.builder()
                    .id(sp.getId())
                    .date(sp.getPaymentDate())
                    .category(MoneyOutflowCategory.SERVICE)
                    .description(description)
                    .amount(sp.getAmount())
                    .paymentMethod(sp.getPaymentMethod() != null ? sp.getPaymentMethod().getDisplayName() : null)
                    .beneficiary(beneficiary)
                    .reference(sp.getReferenceNumber())
                    .comment(sp.getComment())
                    .projectAreaName(projectAreaName)
                    .linkedDocumentId(null)
                    .build());
        }

        log.debug("Collected {} service payment items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from vehicle-based ServicePayment entities (formerly LicencePlatePayment).
     */
    private List<ReportItemDTO> collectFromLicencePlatePayments(ReportFilterDTO filters) {
        log.debug("Collecting from vehicle-based service payments (licence plate)");

        Pageable pageable = PageRequest.of(0, 10000);

        var vehiclePayments = servicePaymentRepository.findAllWithFilters(
                SubjectType.VEHICLE,
                null, // serviceAssignmentId
                null, // serviceSupplierId
                null, // buildingId
                getEffectiveAreaId(filters), // projectAreaId
                null, // serviceType
                null, // vehicleId
                null, // year
                null, // period
                filters.startDate(),
                filters.endDate(),
                filters.minAmount(),
                filters.maxAmount(),
                null, // referenceNumber
                null, // supplierName
                null, // search
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (ServicePayment sp : vehiclePayments) {
            var assignment = sp.getServiceAssignment();
            String licensePlate = assignment != null && assignment.getVehicle() != null
                    ? assignment.getVehicle().getLicensePlate() : "N/A";
            String description = "Pago de patente"
                    + (sp.getYear() != null ? " " + sp.getYear() : "")
                    + (sp.getPeriod() != null ? " - Período " + sp.getPeriod() : "")
                    + " - " + licensePlate;

            items.add(ReportItemDTO.builder()
                    .id(sp.getId())
                    .date(sp.getPaymentDate())
                    .category(MoneyOutflowCategory.LICENCE_PLATE)
                    .description(description)
                    .amount(sp.getAmount())
                    .paymentMethod(sp.getPaymentMethod() != null ? sp.getPaymentMethod().getDisplayName() : null)
                    .beneficiary(licensePlate)
                    .reference(sp.getReferenceNumber())
                    .comment(sp.getComment())
                    .projectAreaName(sp.getProjectArea() != null ? sp.getProjectArea().getName() : null)
                    .linkedDocumentId(null)
                    .build());
        }

        log.debug("Collected {} vehicle-based payment items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from FuelLoad entities.
     */
    private List<ReportItemDTO> collectFromFuelLoads(ReportFilterDTO filters) {
        log.debug("Collecting from fuel loads");

        Pageable pageable = PageRequest.of(0, 10000);

        var fuelLoads = fuelLoadRepository.findAllWithFilters(
                filters.startDate(),
                filters.endDate(),
                null, // branchCode
                null, // ticketNumber
                null, // fuelType
                null, // vehicleId
                null, // vehicleLicensePlate
                getEffectiveAreaId(filters), // projectAreaId - from filter
                null, // projectAreaName
                null, // gasStationId
                null, // search
                null, // gasStationName
                null, // totalAmountMin
                null, // totalAmountMax
                null, // transactionalDocumentId
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (FuelLoad fl : fuelLoads) {
            // Apply amount filters manually since repository doesn't have them
            if (filters.minAmount() != null && fl.getTotalAmount().compareTo(filters.minAmount()) < 0) {
                continue;
            }
            if (filters.maxAmount() != null && fl.getTotalAmount().compareTo(filters.maxAmount()) > 0) {
                continue;
            }

            String description = "Carga de combustible: " + fl.getFuelType().getDisplayName() +
                               " - " + fl.getLiters() + "L";
            String beneficiary = fl.getGasStation() != null ?
                               "Estación de servicio (ID: " + fl.getGasStation().getId() + ")" : "Estación de servicio";

            Long flLinkedDocId = fl.getTransactionalDocument() != null ? fl.getTransactionalDocument().getId() : null;

            items.add(ReportItemDTO.builder()
                    .id(fl.getId())
                    .date(fl.getDate())
                    .category(MoneyOutflowCategory.FUEL)
                    .description(description)
                    .amount(fl.getTotalAmount())
                    .paymentMethod(null)
                    .beneficiary(beneficiary)
                    .reference("Ticket: " + fl.getTicketNumber())
                    .comment("Vehículo: " + (fl.getVehicle() != null ? fl.getVehicle().getLicensePlate() : "Bidón"))
                    .projectAreaName(fl.getProjectArea() != null ? fl.getProjectArea().getName() : null)
                    .linkedDocumentId(flLinkedDocId)
                    .build());
        }

        log.debug("Collected {} fuel load items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from PolicyPayment records.
     * Uses actual registered payments instead of estimated amounts from policies.
     */
    private List<ReportItemDTO> collectFromInsurances(ReportFilterDTO filters) {
        log.debug("Collecting from policy payments");

        Pageable pageable = PageRequest.of(0, 10000);

        var policyPayments = policyPaymentRepository.findAllWithFilters(
                null, // insurancePolicyId
                filters.startDate(),
                filters.endDate(),
                filters.minAmount(),
                filters.maxAmount(),
                null, // policyNumber
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (PolicyPayment pp : policyPayments) {
            String policyNumber = pp.getInsurancePolicy().getPolicyNumber();
            String periodDesc = pp.getPeriodFrom().format(formatter) + " - " + pp.getPeriodTo().format(formatter);

            items.add(ReportItemDTO.builder()
                    .id(pp.getId())
                    .date(pp.getPaymentDate())
                    .category(MoneyOutflowCategory.INSURANCE)
                    .description("Pago de póliza N° " + policyNumber + " - Período " + periodDesc)
                    .amount(pp.getAmount())
                    .paymentMethod(null)
                    .beneficiary("Aseguradora")
                    .reference(policyNumber)
                    .comment(pp.getNotes())
                    .projectAreaName(null)
                    .linkedDocumentId(null)
                    .build());
        }

        log.debug("Collected {} policy payment items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from Repair entities.
     */
    private List<ReportItemDTO> collectFromRepairs(ReportFilterDTO filters) {
        log.debug("Collecting from repairs");

        Pageable pageable = PageRequest.of(0, 10000);

        var repairs = repairRepository.findAllWithFilters(
                filters.startDate(),
                filters.endDate(),
                null, // vehicleId
                null, // licensePlate
                getEffectiveAreaId(filters), // projectAreaId - from filter
                filters.minAmount(),
                filters.maxAmount(),
                null, // supplierId
                null, // supplierName
                null, // description
                null, // itemDescription
                null, // minMileage
                null, // maxMileage
                null, // search
                null, // transactionalDocumentId
                false, // unlinked
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (Repair r : repairs) {
            // Calculate total from items
            java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
            if (r.getItems() != null) {
                for (RepairItem item : r.getItems()) {
                    if (item.getAmount() != null) {
                        totalCost = totalCost.add(item.getAmount());
                    }
                }
            }
            if (totalCost.compareTo(java.math.BigDecimal.ZERO) == 0) {
                continue; // Skip repairs without cost
            }

            String itemDescriptions = "";
            if (r.getItems() != null && !r.getItems().isEmpty()) {
                itemDescriptions = r.getItems().stream()
                        .map(RepairItem::getDescription)
                        .collect(java.util.stream.Collectors.joining(", "));
            }
            String vehiclePlate = r.getVehicle() != null ? r.getVehicle().getLicensePlate() : "N/A";
            String description = "Reparación: " + itemDescriptions +
                               " - Vehículo: " + vehiclePlate;

            String beneficiary;
            if (r.getSupplier() != null) {
                beneficiary = r.getSupplier().getLegalName();
            } else {
                // Use first MANO_DE_OBRA item description as beneficiary
                beneficiary = r.getItems() != null ? r.getItems().stream()
                        .filter(i -> i.getItemType() == RepairItemType.MANO_DE_OBRA)
                        .map(RepairItem::getDescription)
                        .findFirst()
                        .orElse("No especificado") : "No especificado";
            }

            // Collect linked document IDs from items
            Long rLinkedDocId = r.getItems() != null ? r.getItems().stream()
                    .filter(i -> i.getTransactionalDocument() != null)
                    .map(i -> i.getTransactionalDocument().getId())
                    .findFirst()
                    .orElse(null) : null;

            items.add(ReportItemDTO.builder()
                    .id(r.getId())
                    .date(r.getDate())
                    .category(MoneyOutflowCategory.REPAIR)
                    .description(description)
                    .amount(totalCost)
                    .paymentMethod(null)
                    .beneficiary(beneficiary)
                    .reference(null)
                    .comment(r.getDescription())
                    .projectAreaName(r.getVehicle() != null && r.getVehicle().getProjectArea() != null ? r.getVehicle().getProjectArea().getName() : null)
                    .linkedDocumentId(rLinkedDocId)
                    .build());
        }

        log.debug("Collected {} repair items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from StockPurchase entities.
     * StockPurchase does not have projectArea, so when projectAreaIds filter is present, no items are returned.
     */
    private List<ReportItemDTO> collectFromStockPurchases(ReportFilterDTO filters) {
        log.debug("Collecting from stock purchases");

        // StockPurchase has no projectArea - exclude when filtering by area (same as insurance)
        if (filters.projectAreaIds() != null && !filters.projectAreaIds().isEmpty()) {
            log.debug("Stock purchases excluded: projectAreaIds filter active");
            return new ArrayList<>();
        }

        Pageable pageable = PageRequest.of(0, 10000);

        var stockPurchases = stockPurchaseRepository.findAllWithFilters(
                filters.startDate(),
                filters.endDate(),
                null, // stockId
                null, // stockName
                null, // stockCategory
                null, // minQuantity
                null, // maxQuantity
                filters.minAmount(),
                filters.maxAmount(),
                null, // transactionalDocumentId
                null, // search
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (StockPurchase sp : stockPurchases) {
            String stockName = stockRepository.findById(sp.getStockId())
                    .map(Stock::getName)
                    .orElse("Stock ID: " + sp.getStockId());

            String description = "Compra de stock: " + stockName + " - " + sp.getQuantity() + " unidades";

            items.add(ReportItemDTO.builder()
                    .id(sp.getId())
                    .date(sp.getDate())
                    .category(MoneyOutflowCategory.STOCK_PURCHASE)
                    .description(description)
                    .amount(sp.getTotalAmount())
                    .paymentMethod(null)
                    .beneficiary(null)
                    .reference(sp.getNotes())
                    .comment(null)
                    .projectAreaName(null)
                    .linkedDocumentId(sp.getTransactionalDocumentId())
                    .build());
        }

        log.debug("Collected {} stock purchase items", items.size());
        return items;
    }

    /**
     * Calculates the duplicated amount in the report caused by entities linked to TransactionalDocuments.
     * When a salary, fuel load, repair or stock purchase is linked to an invoice, its amount is already
     * included in the invoice total (via DocumentTotalRecalculator). If both the invoice and the linked
     * entity appear in the report, the linked entity's amount is counted twice.
     * This method returns the sum of amounts from linked items whose parent invoice also appears in the report.
     */
    private BigDecimal calculateDuplicatedAmount(List<ReportItemDTO> items) {
        // Collect all invoice IDs present in the report
        Set<Long> invoiceIds = items.stream()
                .filter(item -> item.category() == MoneyOutflowCategory.INVOICE)
                .map(ReportItemDTO::id)
                .collect(Collectors.toSet());

        // Sum amounts of items that are linked to an invoice present in the report
        return items.stream()
                .filter(item -> item.linkedDocumentId() != null)
                .filter(item -> invoiceIds.contains(item.linkedDocumentId()))
                .map(ReportItemDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the single effective project area ID from the filter list, or null if no filter.
     * Only used when iterating with exactly one area ID (recursive single-area calls).
     */
    private Long getEffectiveAreaId(ReportFilterDTO filters) {
        List<Long> ids = filters.projectAreaIds();
        return (ids != null && !ids.isEmpty()) ? ids.get(0) : null;
    }

    /**
     * Gets the project area name(s) by ID list if provided.
     * Returns null if projectAreaIds is null or empty.
     */
    private String getProjectAreaName(List<Long> projectAreaIds) {
        if (projectAreaIds == null || projectAreaIds.isEmpty()) {
            return null;
        }

        return projectAreaIds.stream()
                .map(id -> projectAreaRepository.findByIdAndDeletedFalse(id)
                        .map(pa -> pa.getName())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SALARY REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public SalaryReportDTO generateSalaryReport(SalaryReportFilterDTO filters) {
        log.info("Generating salary report with filters: {}", filters);

        validateSalaryFilters(filters);

        Pageable pageable = PageRequest.of(0, 10000);

        // Collect raw salary payments using existing repository
        List<Long> areaIds = filters.projectAreaIds();
        Long effectiveAreaId = (areaIds != null && !areaIds.isEmpty()) ? areaIds.get(0) : null;

        // If multiple area IDs, we collect per area and merge; otherwise single call
        List<SalaryPayment> allPayments;
        if (areaIds != null && areaIds.size() > 1) {
            allPayments = new ArrayList<>();
            for (Long areaId : areaIds) {
                allPayments.addAll(salaryPaymentRepository.findAllWithFilters(
                        null, null, null,
                        filters.salaryFrequency(),
                        areaId,
                        filters.startDate(),
                        filters.endDate(),
                        filters.minAmount(),
                        filters.maxAmount(),
                        filters.paymentMethod(),
                        null, null,
                        pageable
                ).getContent());
            }
        } else {
            allPayments = salaryPaymentRepository.findAllWithFilters(
                    null, null, null,
                    filters.salaryFrequency(),
                    effectiveAreaId,
                    filters.startDate(),
                    filters.endDate(),
                    filters.minAmount(),
                    filters.maxAmount(),
                    filters.paymentMethod(),
                    null, null,
                    pageable
            ).getContent();
        }

        // Group by project area, then by employee
        List<SalaryReportAreaGroupDTO> areaGroups = buildAreaGroups(allPayments);

        // Calculate grand totals
        BigDecimal totalAmount = allPayments.stream()
                .map(SalaryPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<SalaryFrecuency, BigDecimal> totalsByFrequency = buildFrequencySubtotals(allPayments);

        // Build period description
        String periodDesc = buildSalaryPeriodDescription(filters);

        return SalaryReportDTO.builder()
                .filters(filters)
                .areaGroups(areaGroups)
                .totalAmount(totalAmount)
                .totalCount(allPayments.size())
                .totalsByFrequency(totalsByFrequency)
                .generatedAt(LocalDateTime.now())
                .reportName("Reporte de Salarios")
                .periodDescription(periodDesc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateSalaryReportFile(SalaryReportFilterDTO filters, ReportFormat format) {
        log.info("Generating salary report file: format={}", format);

        SalaryReportDTO report = generateSalaryReport(filters);

        byte[] content;
        switch (format) {
            case EXCEL -> content = salaryExcelExporter.export(report);
            case PDF -> content = salaryPdfExporter.export(report);
            default -> throw new InvalidReportFormatException(format.name());
        }

        String filename = "reporte_salarios_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + format.getFileExtension() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, format.getContentType())
                .body(content);
    }

    /**
     * Builds the hierarchical area groups from a flat list of salary payments.
     */
    private List<SalaryReportAreaGroupDTO> buildAreaGroups(List<SalaryPayment> payments) {
        // Group payments by project area (employee's area)
        Map<Long, List<SalaryPayment>> byArea = new LinkedHashMap<>();

        for (SalaryPayment sp : payments) {
            Long areaId = sp.getEmployee().getProjectArea() != null
                    ? sp.getEmployee().getProjectArea().getId()
                    : -1L; // sentinel for "no area"
            byArea.computeIfAbsent(areaId, k -> new ArrayList<>()).add(sp);
        }

        List<SalaryReportAreaGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<SalaryPayment>> entry : byArea.entrySet()) {
            Long areaId = entry.getKey();
            List<SalaryPayment> areaPayments = entry.getValue();

            String areaName;
            String areaColor;
            Long areaIdDTO;

            if (areaId == -1L) {
                areaIdDTO = null;
                areaName = "Sin área asignada";
                areaColor = null;
            } else {
                areaIdDTO = areaId;
                SalaryPayment first = areaPayments.get(0);
                areaName = first.getEmployee().getProjectArea().getName();
                areaColor = first.getEmployee().getProjectArea().getColor();
            }

            List<SalaryReportEmployeeGroupDTO> employeeGroups = buildEmployeeGroups(areaPayments);

            BigDecimal subtotal = areaPayments.stream()
                    .map(SalaryPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<SalaryFrecuency, BigDecimal> areaFreqSubtotals = buildFrequencySubtotals(areaPayments);

            groups.add(SalaryReportAreaGroupDTO.builder()
                    .projectAreaId(areaIdDTO)
                    .projectAreaName(areaName)
                    .projectAreaColor(areaColor)
                    .subtotalAmount(subtotal)
                    .paymentCount(areaPayments.size())
                    .subtotalsByFrequency(areaFreqSubtotals)
                    .employeeGroups(employeeGroups)
                    .build());
        }

        // Sort alphabetically by area name, "Sin área asignada" goes last
        groups.sort((a, b) -> {
            if (a.projectAreaId() == null) return 1;
            if (b.projectAreaId() == null) return -1;
            return a.projectAreaName().compareToIgnoreCase(b.projectAreaName());
        });

        return groups;
    }

    /**
     * Builds employee groups within an area, sorted alphabetically by lastName then name.
     */
    private List<SalaryReportEmployeeGroupDTO> buildEmployeeGroups(List<SalaryPayment> areaPayments) {
        // Group by employee ID
        Map<Long, List<SalaryPayment>> byEmployee = new LinkedHashMap<>();
        for (SalaryPayment sp : areaPayments) {
            byEmployee.computeIfAbsent(sp.getEmployee().getId(), k -> new ArrayList<>()).add(sp);
        }

        List<SalaryReportEmployeeGroupDTO> employeeGroups = new ArrayList<>();

        for (Map.Entry<Long, List<SalaryPayment>> entry : byEmployee.entrySet()) {
            List<SalaryPayment> empPayments = entry.getValue();
            SalaryPayment first = empPayments.get(0);

            BigDecimal empTotal = empPayments.stream()
                    .map(SalaryPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<SalaryFrecuency, BigDecimal> empFreqSubtotals = buildFrequencySubtotals(empPayments);

            // Build individual payment DTOs sorted by frequency (MENSUAL > QUINCENAL > SEMANAL) then date desc
            List<SalaryReportPaymentDTO> paymentDTOs = empPayments.stream()
                    .sorted(Comparator.comparing((SalaryPayment sp) -> sp.getSalaryFrequency().ordinal())
                            .thenComparing(SalaryPayment::getPaymentDate, Comparator.reverseOrder()))
                    .map(sp -> new SalaryReportPaymentDTO(
                            sp.getId(),
                            sp.getPaymentDate(),
                            sp.getAmount(),
                            sp.getSalaryFrequency(),
                            sp.getPaymentMethod()
                    ))
                    .toList();

            employeeGroups.add(SalaryReportEmployeeGroupDTO.builder()
                    .employeeId(first.getEmployee().getId())
                    .employeeName(first.getEmployee().getName())
                    .employeeLastName(first.getEmployee().getLastName())
                    .totalAmount(empTotal)
                    .paymentCount(empPayments.size())
                    .subtotalsByFrequency(empFreqSubtotals)
                    .payments(paymentDTOs)
                    .build());
        }

        // Sort alphabetically by lastName, then by name
        employeeGroups.sort(Comparator
                .comparing(SalaryReportEmployeeGroupDTO::employeeLastName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(SalaryReportEmployeeGroupDTO::employeeName, String.CASE_INSENSITIVE_ORDER));

        return employeeGroups;
    }

    /**
     * Builds a map of subtotals keyed by SalaryFrecuency.
     * Only frequencies that have actual payments are included.
     */
    private Map<SalaryFrecuency, BigDecimal> buildFrequencySubtotals(List<SalaryPayment> payments) {
        Map<SalaryFrecuency, BigDecimal> subtotals = new EnumMap<>(SalaryFrecuency.class);

        for (SalaryPayment sp : payments) {
            subtotals.merge(sp.getSalaryFrequency(), sp.getAmount(), BigDecimal::add);
        }

        return subtotals;
    }

    private void validateSalaryFilters(SalaryReportFilterDTO filters) {
        if (filters.startDate() != null && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new InvalidReportFilterException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (filters.minAmount() != null && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new InvalidReportFilterException("El monto mínimo no puede ser mayor al monto máximo");
        }
    }

    private static final List<SalaryFrecuency> FREQUENCY_ORDER = List.of(
            SalaryFrecuency.MENSUAL, SalaryFrecuency.QUINCENAL, SalaryFrecuency.SEMANAL);

    private String buildSalaryPeriodDescription(SalaryReportFilterDTO filters) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filters.startDate() != null && filters.endDate() != null) {
            // Check if the range covers a full calendar month
            if (filters.startDate().getDayOfMonth() == 1
                    && filters.endDate().equals(filters.startDate().withDayOfMonth(
                            filters.startDate().lengthOfMonth()))
                    && filters.startDate().getMonth() == filters.endDate().getMonth()
                    && filters.startDate().getYear() == filters.endDate().getYear()) {
                String monthName = filters.startDate().getMonth()
                        .getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "AR"));
                monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                return monthName + " " + filters.startDate().getYear();
            }
            return "Período: " + filters.startDate().format(fmt) + " - " + filters.endDate().format(fmt);
        } else if (filters.startDate() != null) {
            return "Desde: " + filters.startDate().format(fmt);
        } else if (filters.endDate() != null) {
            return "Hasta: " + filters.endDate().format(fmt);
        }
        return "Sin filtro de período";
    }
}
