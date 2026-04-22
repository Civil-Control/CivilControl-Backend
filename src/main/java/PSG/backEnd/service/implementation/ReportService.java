package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.report.InvalidReportFilterException;
import PSG.backEnd.exception.report.InvalidReportFormatException;
import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.MoneyOutflowReportPreviewDTO;
import PSG.backEnd.model.dto.report.ReportFilterDTO;
import PSG.backEnd.model.dto.report.ReportItemDTO;
import PSG.backEnd.model.dto.report.salary.*;
import PSG.backEnd.model.dto.report.invoice.*;
import PSG.backEnd.model.dto.report.servicePayment.*;
import PSG.backEnd.model.dto.report.fuelLoad.*;
import PSG.backEnd.model.dto.report.repair.*;
import PSG.backEnd.model.dto.report.stockPurchase.*;
import PSG.backEnd.model.dto.report.policyPayment.*;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.Stock;
import PSG.backEnd.model.entity.StockPurchase;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.entity.insurance.InsurancePolicyPaymentDetail;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.insurance.PolicyVehicle;
import PSG.backEnd.model.entity.payment.PaymentDetails;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.enums.MoneyOutflowCategory;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.enums.ReportFormat;
import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.entity.vehicle.RepairItem;
import PSG.backEnd.model.enums.vehicle.RepairItemType;
import PSG.backEnd.repository.*;
import PSG.backEnd.repository.PaymentRepository.PaymentRepository;
import PSG.backEnd.service.export.IReportExporter;
import PSG.backEnd.service.export.SalaryReportExcelExporter;
import PSG.backEnd.service.export.SalaryReportPdfExporter;
import PSG.backEnd.service.export.InvoiceReportExcelExporter;
import PSG.backEnd.service.export.InvoiceReportPdfExporter;
import PSG.backEnd.service.export.ServicePaymentReportExcelExporter;
import PSG.backEnd.service.export.ServicePaymentReportPdfExporter;
import PSG.backEnd.service.export.FuelLoadReportExcelExporter;
import PSG.backEnd.service.export.FuelLoadReportPdfExporter;
import PSG.backEnd.service.export.RepairReportExcelExporter;
import PSG.backEnd.service.export.RepairReportPdfExporter;
import PSG.backEnd.service.export.StockPurchaseReportExcelExporter;
import PSG.backEnd.service.export.StockPurchaseReportPdfExporter;
import PSG.backEnd.service.export.PolicyPaymentReportExcelExporter;
import PSG.backEnd.service.export.PolicyPaymentReportPdfExporter;
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

    // Invoice report exporters
    private final InvoiceReportExcelExporter invoiceExcelExporter;
    private final InvoiceReportPdfExporter invoicePdfExporter;

    // Service payment report exporters
    private final ServicePaymentReportExcelExporter servicePaymentExcelExporter;
    private final ServicePaymentReportPdfExporter servicePaymentPdfExporter;

    // Fuel load report exporters
    private final FuelLoadReportExcelExporter fuelLoadExcelExporter;
    private final FuelLoadReportPdfExporter fuelLoadPdfExporter;

    // Repair report exporters
    private final RepairReportExcelExporter repairExcelExporter;
    private final RepairReportPdfExporter repairPdfExporter;

    // Stock purchase report exporters
    private final StockPurchaseReportExcelExporter stockPurchaseExcelExporter;
    private final StockPurchaseReportPdfExporter stockPurchasePdfExporter;

    // Policy payment report exporters
    private final PolicyPaymentReportExcelExporter policyPaymentExcelExporter;
    private final PolicyPaymentReportPdfExporter policyPaymentPdfExporter;

    // Repositories for data collection
    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final ServicePaymentRepository servicePaymentRepository;
    private final FuelLoadRepository fuelLoadRepository;
    private final InsurancePolicyPaymentDetailRepository insurancePolicyPaymentDetailRepository;
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
            InvoiceReportExcelExporter invoiceExcelExporter,
            InvoiceReportPdfExporter invoicePdfExporter,
            ServicePaymentReportExcelExporter servicePaymentExcelExporter,
            ServicePaymentReportPdfExporter servicePaymentPdfExporter,
            FuelLoadReportExcelExporter fuelLoadExcelExporter,
            FuelLoadReportPdfExporter fuelLoadPdfExporter,
            RepairReportExcelExporter repairExcelExporter,
            RepairReportPdfExporter repairPdfExporter,
            StockPurchaseReportExcelExporter stockPurchaseExcelExporter,
            StockPurchaseReportPdfExporter stockPurchasePdfExporter,
            PolicyPaymentReportExcelExporter policyPaymentExcelExporter,
            PolicyPaymentReportPdfExporter policyPaymentPdfExporter,
            TransactionalDocumentRepository transactionalDocumentRepository,
            SalaryPaymentRepository salaryPaymentRepository,
            ServicePaymentRepository servicePaymentRepository,
            FuelLoadRepository fuelLoadRepository,
            InsurancePolicyPaymentDetailRepository insurancePolicyPaymentDetailRepository,
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
        this.invoiceExcelExporter = invoiceExcelExporter;
        this.invoicePdfExporter = invoicePdfExporter;
        this.servicePaymentExcelExporter = servicePaymentExcelExporter;
        this.servicePaymentPdfExporter = servicePaymentPdfExporter;
        this.fuelLoadExcelExporter = fuelLoadExcelExporter;
        this.fuelLoadPdfExporter = fuelLoadPdfExporter;
        this.repairExcelExporter = repairExcelExporter;
        this.repairPdfExporter = repairPdfExporter;
        this.stockPurchaseExcelExporter = stockPurchaseExcelExporter;
        this.stockPurchasePdfExporter = stockPurchasePdfExporter;
        this.policyPaymentExcelExporter = policyPaymentExcelExporter;
        this.policyPaymentPdfExporter = policyPaymentPdfExporter;
        this.transactionalDocumentRepository = transactionalDocumentRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.servicePaymentRepository = servicePaymentRepository;
        this.fuelLoadRepository = fuelLoadRepository;
        this.insurancePolicyPaymentDetailRepository = insurancePolicyPaymentDetailRepository;
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
                    .projectAreaTaskName(doc.getProjectAreaTask() != null ? doc.getProjectAreaTask().getName() : null)
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
                    .projectAreaTaskName(sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getName() : null)
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
                    .projectAreaTaskName(sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getName() : null)
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
                    .projectAreaTaskName(sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getName() : null)
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
                null, // totalAmountLike
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
                    .projectAreaTaskName(fl.getProjectAreaTask() != null ? fl.getProjectAreaTask().getName() : null)
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

        var policyPaymentDetails = insurancePolicyPaymentDetailRepository.findAllWithFilters(
                null, // insurancePolicyId
                filters.startDate(),
                filters.endDate(),
                filters.minAmount(),
                filters.maxAmount(),
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (InsurancePolicyPaymentDetail ippd : policyPaymentDetails) {
            PaymentDetails pd = ippd.getPaymentDetails();
            String policyNumber = ippd.getInsurancePolicy().getPolicyNumber();
            String periodDesc = ippd.getPeriodFrom().format(formatter) + " - " + ippd.getPeriodTo().format(formatter);

            items.add(ReportItemDTO.builder()
                    .id(ippd.getId())
                    .date(pd.getPaymentDate())
                    .category(MoneyOutflowCategory.INSURANCE)
                    .description("Pago de póliza N° " + policyNumber + " - Período " + periodDesc)
                    .amount(pd.getAmount())
                    .paymentMethod(null)
                    .beneficiary(pd.getSupplier() != null ? pd.getSupplier().getLegalName() : "Aseguradora")
                    .reference(policyNumber)
                    .comment(pd.getComment())
                    .projectAreaName(null)
                    .projectAreaTaskName(null)
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
                    .projectAreaTaskName(r.getVehicle() != null && r.getVehicle().getProjectAreaTask() != null ? r.getVehicle().getProjectAreaTask().getName() : null)
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
                    .projectAreaTaskName(null)
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
        // Group payments by the project area of each individual payment.
        // This ensures that an employee with payments in multiple sectors is
        // distributed across those sectors, instead of forcing all of their
        // payments under the employee's primary assigned sector.
        Map<Long, List<SalaryPayment>> byArea = new LinkedHashMap<>();

        for (SalaryPayment sp : payments) {
            Long areaId = sp.getProjectArea() != null
                    ? sp.getProjectArea().getId()
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
                areaName = first.getProjectArea().getName();
                areaColor = first.getProjectArea().getColor();
            }

            List<SalaryReportEmployeeGroupDTO> employeeGroups = buildEmployeeGroups(areaPayments, areaName);

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
    private List<SalaryReportEmployeeGroupDTO> buildEmployeeGroups(List<SalaryPayment> areaPayments, String areaName) {
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
                            sp.getPaymentMethod(),
                            sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getId() : null,
                            sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getName() : null,
                            sp.getEmployee().getName(),
                            sp.getEmployee().getLastName(),
                            areaName
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

    // ═══════════════════════════════════════════════════════════════════════
    // INVOICE REPORT
    // ═══════════════════════════════════════════════════════════════════════

    private static final List<DocumentType> DOCUMENT_TYPE_ORDER = List.of(
            DocumentType.BILL_A, DocumentType.BILL_B, DocumentType.BILL_C,
            DocumentType.DEBIT_NOTE_A, DocumentType.DEBIT_NOTE_B, DocumentType.DEBIT_NOTE_C,
            DocumentType.CREDIT_NOTE_A, DocumentType.CREDIT_NOTE_B, DocumentType.CREDIT_NOTE_C,
            DocumentType.OTHER_DOCUMENT);

    @Override
    @Transactional(readOnly = true)
    public InvoiceReportDTO generateInvoiceReport(InvoiceReportFilterDTO filters) {
        log.info("Generating invoice report with filters: {}", filters);

        validateInvoiceFilters(filters);

        Pageable pageable = PageRequest.of(0, 10000);

        List<Long> areaIds = filters.projectAreaIds();
        Long effectiveAreaId = (areaIds != null && !areaIds.isEmpty()) ? areaIds.get(0) : null;

        List<TransactionalDocument> allDocuments;
        if (areaIds != null && areaIds.size() > 1) {
            allDocuments = new ArrayList<>();
            for (Long areaId : areaIds) {
                allDocuments.addAll(transactionalDocumentRepository.findAllWithFilters(
                        null,
                        filters.documentType(),
                        null, null,
                        areaId,
                        null,
                        filters.maxAmount(),
                        filters.minAmount(),
                        null,
                        filters.startDate(),
                        filters.endDate(),
                        filters.paid(),
                        null,
                        pageable
                ).getContent());
            }
        } else {
            allDocuments = transactionalDocumentRepository.findAllWithFilters(
                    null,
                    filters.documentType(),
                    null, null,
                    effectiveAreaId,
                    null,
                    filters.maxAmount(),
                    filters.minAmount(),
                    null,
                    filters.startDate(),
                    filters.endDate(),
                    filters.paid(),
                    null,
                    pageable
            ).getContent();
        }

        List<InvoiceReportAreaGroupDTO> areaGroups = buildInvoiceAreaGroups(allDocuments);

        // Credit notes are subtracted from totals because they reduce the supplier's liability.
        BigDecimal totalAmount = allDocuments.stream()
                .map(td -> signed(td, td.getTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = allDocuments.stream()
                .map(td -> signed(td, td.getNetTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIva = allDocuments.stream()
                .map(td -> signed(td, td.getIvaTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIvaExempt = allDocuments.stream()
                .map(td -> signed(td, td.getIvaExemptTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOtherTaxes = allDocuments.stream()
                .map(td -> signed(td, td.getOtherTaxes()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<DocumentType, BigDecimal> totalsByDocType = buildDocumentTypeSubtotals(allDocuments);
        Map<String, BigDecimal> totalsByIvaRate = buildIvaRateSubtotals(allDocuments);

        String periodDesc = buildInvoicePeriodDescription(filters);

        return InvoiceReportDTO.builder()
                .filters(filters)
                .areaGroups(areaGroups)
                .totalAmount(totalAmount)
                .totalNet(totalNet)
                .totalIva(totalIva)
                .totalIvaExempt(totalIvaExempt)
                .totalOtherTaxes(totalOtherTaxes)
                .totalCount(allDocuments.size())
                .totalsByDocumentType(totalsByDocType)
                .totalsByIvaRate(totalsByIvaRate)
                .generatedAt(LocalDateTime.now())
                .reportName("Reporte de Facturación")
                .periodDescription(periodDesc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateInvoiceReportFile(InvoiceReportFilterDTO filters, ReportFormat format) {
        log.info("Generating invoice report file: format={}", format);

        InvoiceReportDTO report = generateInvoiceReport(filters);

        byte[] content;
        switch (format) {
            case EXCEL -> content = invoiceExcelExporter.export(report);
            case PDF -> content = invoicePdfExporter.export(report);
            default -> throw new InvalidReportFormatException(format.name());
        }

        String filename = "reporte_facturacion_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + format.getFileExtension() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, format.getContentType())
                .body(content);
    }

    private List<InvoiceReportAreaGroupDTO> buildInvoiceAreaGroups(List<TransactionalDocument> documents) {
        Map<Long, List<TransactionalDocument>> byArea = new LinkedHashMap<>();

        for (TransactionalDocument td : documents) {
            Long areaId = td.getProjectArea() != null
                    ? td.getProjectArea().getId()
                    : -1L;
            byArea.computeIfAbsent(areaId, k -> new ArrayList<>()).add(td);
        }

        List<InvoiceReportAreaGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<TransactionalDocument>> entry : byArea.entrySet()) {
            Long areaId = entry.getKey();
            List<TransactionalDocument> areaDocs = entry.getValue();

            String areaName;
            String areaColor;
            Long areaIdDTO;

            if (areaId == -1L) {
                areaIdDTO = null;
                areaName = "Sin área asignada";
                areaColor = null;
            } else {
                areaIdDTO = areaId;
                TransactionalDocument first = areaDocs.get(0);
                areaName = first.getProjectArea().getName();
                areaColor = first.getProjectArea().getColor();
            }

            List<InvoiceReportSupplierGroupDTO> supplierGroups = buildSupplierGroups(areaDocs, areaName);

            BigDecimal subtotal = areaDocs.stream()
                    .map(td -> signed(td, td.getTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal subtotalNet = areaDocs.stream()
                    .map(td -> signed(td, td.getNetTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal subtotalIva = areaDocs.stream()
                    .map(td -> signed(td, td.getIvaTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal subtotalIvaExempt = areaDocs.stream()
                    .map(td -> signed(td, td.getIvaExemptTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal subtotalOtherTaxes = areaDocs.stream()
                    .map(td -> signed(td, td.getOtherTaxes()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<DocumentType, BigDecimal> areaDocTypeSubtotals = buildDocumentTypeSubtotals(areaDocs);

            groups.add(InvoiceReportAreaGroupDTO.builder()
                    .projectAreaId(areaIdDTO)
                    .projectAreaName(areaName)
                    .projectAreaColor(areaColor)
                    .subtotalAmount(subtotal)
                    .subtotalNet(subtotalNet)
                    .subtotalIva(subtotalIva)
                    .subtotalIvaExempt(subtotalIvaExempt)
                    .subtotalOtherTaxes(subtotalOtherTaxes)
                    .documentCount(areaDocs.size())
                    .subtotalsByDocumentType(areaDocTypeSubtotals)
                    .supplierGroups(supplierGroups)
                    .build());
        }

        groups.sort((a, b) -> {
            if (a.projectAreaId() == null) return 1;
            if (b.projectAreaId() == null) return -1;
            return a.projectAreaName().compareToIgnoreCase(b.projectAreaName());
        });

        return groups;
    }

    private List<InvoiceReportSupplierGroupDTO> buildSupplierGroups(List<TransactionalDocument> areaDocs, String areaName) {
        Map<Long, List<TransactionalDocument>> bySupplier = new LinkedHashMap<>();
        for (TransactionalDocument td : areaDocs) {
            bySupplier.computeIfAbsent(td.getSupplier().getId(), k -> new ArrayList<>()).add(td);
        }

        List<InvoiceReportSupplierGroupDTO> supplierGroups = new ArrayList<>();

        for (Map.Entry<Long, List<TransactionalDocument>> entry : bySupplier.entrySet()) {
            List<TransactionalDocument> supplierDocs = entry.getValue();
            TransactionalDocument first = supplierDocs.get(0);

            BigDecimal supplierTotal = supplierDocs.stream()
                    .map(td -> signed(td, td.getTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal supplierNet = supplierDocs.stream()
                    .map(td -> signed(td, td.getNetTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal supplierIva = supplierDocs.stream()
                    .map(td -> signed(td, td.getIvaTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal supplierIvaExempt = supplierDocs.stream()
                    .map(td -> signed(td, td.getIvaExemptTotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal supplierOtherTaxes = supplierDocs.stream()
                    .map(td -> signed(td, td.getOtherTaxes()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<DocumentType, BigDecimal> supplierDocTypeSubtotals = buildDocumentTypeSubtotals(supplierDocs);

            List<InvoiceReportDocumentDTO> documentDTOs = supplierDocs.stream()
                    .sorted(Comparator.comparing(TransactionalDocument::getDate, Comparator.reverseOrder()))
                    .map(td -> new InvoiceReportDocumentDTO(
                            td.getId(),
                            td.getDate(),
                            td.getDocumentType(),
                            td.getBranchCode(),
                            td.getDocumentNumber(),
                            td.getTotal(),
                            td.getNetTotal(),
                            td.getIvaTotal(),
                            td.getIvaExemptTotal(),
                            td.getOtherTaxes(),
                            td.getSupplier().getLegalName(),
                            td.getProjectAreaTask() != null ? td.getProjectAreaTask().getId() : null,
                            td.getProjectAreaTask() != null ? td.getProjectAreaTask().getName() : null,
                            td.getPaid(),
                            areaName
                    ))
                    .toList();

            supplierGroups.add(InvoiceReportSupplierGroupDTO.builder()
                    .supplierId(first.getSupplier().getId())
                    .supplierLegalName(first.getSupplier().getLegalName())
                    .supplierTradeName(first.getSupplier().getTradeName())
                    .supplierCuit(first.getSupplier().getCuit())
                    .totalAmount(supplierTotal)
                    .totalNet(supplierNet)
                    .totalIva(supplierIva)
                    .totalIvaExempt(supplierIvaExempt)
                    .totalOtherTaxes(supplierOtherTaxes)
                    .documentCount(supplierDocs.size())
                    .subtotalsByDocumentType(supplierDocTypeSubtotals)
                    .documents(documentDTOs)
                    .build());
        }

        supplierGroups.sort(Comparator
                .comparing(InvoiceReportSupplierGroupDTO::supplierLegalName, String.CASE_INSENSITIVE_ORDER));

        return supplierGroups;
    }

    private Map<DocumentType, BigDecimal> buildDocumentTypeSubtotals(List<TransactionalDocument> documents) {
        Map<DocumentType, BigDecimal> subtotals = new EnumMap<>(DocumentType.class);

        for (TransactionalDocument td : documents) {
            subtotals.merge(td.getDocumentType(), td.getTotal(), BigDecimal::add);
        }

        return subtotals;
    }

    /**
     * Returns {@code value} negated when the document is a Credit Note (because credit notes
     * reduce the supplier's liability and therefore subtract from invoicing totals),
     * otherwise returns {@code value} unchanged. Null-safe.
     */
    private BigDecimal signed(TransactionalDocument td, BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        DocumentType type = td.getDocumentType();
        if (type == DocumentType.CREDIT_NOTE_A
                || type == DocumentType.CREDIT_NOTE_B
                || type == DocumentType.CREDIT_NOTE_C) {
            return value.negate();
        }
        return value;
    }

    private Map<String, BigDecimal> buildIvaRateSubtotals(List<TransactionalDocument> documents) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        BigDecimal LINKED_IVA_RATE = new BigDecimal("21");

        for (TransactionalDocument td : documents) {
            // IVA exempt total
            if (td.getIvaExemptTotal() != null && td.getIvaExemptTotal().compareTo(BigDecimal.ZERO) > 0) {
                result.merge("Exento", td.getIvaExemptTotal(), BigDecimal::add);
            }

            if (td.getIvaTotal() == null || td.getIvaTotal().compareTo(BigDecimal.ZERO) <= 0) {
                // No IVA on this document — skip to other taxes
                if (td.getOtherTaxes() != null && td.getOtherTaxes().compareTo(BigDecimal.ZERO) > 0) {
                    result.merge("Otros tributos", td.getOtherTaxes(), BigDecimal::add);
                }
                continue;
            }

            // Compute IVA explicitly from ItemDetails, grouped by rate
            // Uses the same formula as DocumentTotalRecalculator:
            //   iva = unitAmount * quantity * (ivaPercentage / 100)
            Map<BigDecimal, BigDecimal> ivaByRate = new TreeMap<>();
            BigDecimal totalItemsIva = BigDecimal.ZERO;

            if (td.getItems() != null) {
                for (var item : td.getItems()) {
                    if (item.getIvaPercentage() != null && item.getIvaPercentage().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal itemIva = item.getUnitAmount()
                                .multiply(BigDecimal.valueOf(item.getQuantity()))
                                .multiply(item.getIvaPercentage()
                                        .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
                        ivaByRate.merge(item.getIvaPercentage().stripTrailingZeros(), itemIva, BigDecimal::add);
                        totalItemsIva = totalItemsIva.add(itemIva);
                    }
                }
            }

            // Remaining IVA not covered by ItemDetails comes from linked records
            // (FuelLoad, RepairItem, SalaryPayment, StockPurchase — all at 21% per DocumentTotalRecalculator)
            BigDecimal remainingIva = td.getIvaTotal()
                    .subtract(totalItemsIva.setScale(2, java.math.RoundingMode.HALF_UP));
            if (remainingIva.compareTo(BigDecimal.ZERO) > 0) {
                ivaByRate.merge(LINKED_IVA_RATE, remainingIva, BigDecimal::add);
            }

            // Add to result map
            for (Map.Entry<BigDecimal, BigDecimal> entry : ivaByRate.entrySet()) {
                String label = "IVA " + entry.getKey().stripTrailingZeros().toPlainString() + "%";
                result.merge(label, entry.getValue().setScale(2, java.math.RoundingMode.HALF_UP), BigDecimal::add);
            }

            // Other taxes
            if (td.getOtherTaxes() != null && td.getOtherTaxes().compareTo(BigDecimal.ZERO) > 0) {
                result.merge("Otros tributos", td.getOtherTaxes(), BigDecimal::add);
            }
        }

        return result;
    }

    private void validateInvoiceFilters(InvoiceReportFilterDTO filters) {
        if (filters.startDate() != null && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new InvalidReportFilterException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
        if (filters.minAmount() != null && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new InvalidReportFilterException("El monto mínimo no puede ser mayor al monto máximo");
        }
    }

    private String buildInvoicePeriodDescription(InvoiceReportFilterDTO filters) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filters.startDate() != null && filters.endDate() != null) {
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

    // ═══════════════════════════════════════════════════════════════════════
    // SERVICE PAYMENT REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ServicePaymentReportDTO generateServicePaymentReport(ServicePaymentReportFilterDTO filters) {
        log.info("Generating service payment report with filters: {}", filters);

        validateServicePaymentFilters(filters);

        Pageable pageable = PageRequest.of(0, 10000);

        // Collect raw service payments using existing repository
        List<Long> areaIds = filters.projectAreaIds();

        List<ServicePayment> allPayments;
        if (areaIds != null && areaIds.size() > 1) {
            allPayments = new ArrayList<>();
            for (Long areaId : areaIds) {
                allPayments.addAll(servicePaymentRepository.findAllWithFilters(
                        filters.subjectType(), null, null, null,
                        areaId,
                        filters.serviceType(),
                        null,
                        filters.year(),
                        filters.period(),
                        filters.startDate(),
                        filters.endDate(),
                        filters.minAmount(),
                        filters.maxAmount(),
                        null, null, null,
                        pageable
                ).getContent());
            }
        } else {
            Long effectiveAreaId = (areaIds != null && !areaIds.isEmpty()) ? areaIds.get(0) : null;
            allPayments = servicePaymentRepository.findAllWithFilters(
                    filters.subjectType(), null, null, null,
                    effectiveAreaId,
                    filters.serviceType(),
                    null,
                    filters.year(),
                    filters.period(),
                    filters.startDate(),
                    filters.endDate(),
                    filters.minAmount(),
                    filters.maxAmount(),
                    null, null, null,
                    pageable
            ).getContent();
        }

        // Filter by payment method if specified (not in repository query)
        if (filters.paymentMethod() != null) {
            allPayments = allPayments.stream()
                    .filter(sp -> filters.paymentMethod().equals(sp.getPaymentMethod()))
                    .toList();
        }

        // Group by project area, then by building
        List<ServicePaymentReportAreaGroupDTO> areaGroups = buildServicePaymentAreaGroups(allPayments);

        // Calculate grand totals
        BigDecimal totalAmount = allPayments.stream()
                .map(ServicePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> totalsByServiceType = buildServiceTypeSubtotals(allPayments);
        Map<String, BigDecimal> totalsBySubjectType = buildSubjectTypeSubtotals(allPayments);

        // Build period description
        String periodDesc = buildServicePaymentPeriodDescription(filters);

        return ServicePaymentReportDTO.builder()
                .filters(filters)
                .areaGroups(areaGroups)
                .totalAmount(totalAmount)
                .totalCount(allPayments.size())
                .totalsByServiceType(totalsByServiceType)
                .totalsBySubjectType(totalsBySubjectType)
                .generatedAt(LocalDateTime.now())
                .reportName("Reporte de Pago de Servicios")
                .periodDescription(periodDesc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateServicePaymentReportFile(ServicePaymentReportFilterDTO filters, ReportFormat format) {
        log.info("Generating service payment report file: format={}", format);

        ServicePaymentReportDTO report = generateServicePaymentReport(filters);

        byte[] content;
        switch (format) {
            case EXCEL -> content = servicePaymentExcelExporter.export(report);
            case PDF -> content = servicePaymentPdfExporter.export(report);
            default -> throw new InvalidReportFormatException(format.name());
        }

        String filename = "reporte_servicios_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + format.getFileExtension() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, format.getContentType())
                .body(content);
    }

    /**
     * Builds the hierarchical area groups from a flat list of service payments.
     */
    private List<ServicePaymentReportAreaGroupDTO> buildServicePaymentAreaGroups(List<ServicePayment> payments) {
        Map<Long, List<ServicePayment>> byArea = new LinkedHashMap<>();

        for (ServicePayment sp : payments) {
            Long areaId = sp.getProjectArea() != null
                    ? sp.getProjectArea().getId()
                    : -1L;
            byArea.computeIfAbsent(areaId, k -> new ArrayList<>()).add(sp);
        }

        List<ServicePaymentReportAreaGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<ServicePayment>> entry : byArea.entrySet()) {
            Long areaId = entry.getKey();
            List<ServicePayment> areaPayments = entry.getValue();

            String areaName;
            String areaColor;
            Long areaIdDTO;

            if (areaId == -1L) {
                areaIdDTO = null;
                areaName = "Sin área asignada";
                areaColor = null;
            } else {
                areaIdDTO = areaId;
                ServicePayment first = areaPayments.get(0);
                areaName = first.getProjectArea().getName();
                areaColor = first.getProjectArea().getColor();
            }

            List<ServicePaymentReportBuildingGroupDTO> buildingGroups = buildBuildingGroups(areaPayments, areaName);

            BigDecimal subtotal = areaPayments.stream()
                    .map(ServicePayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, BigDecimal> areaServiceTypeSubtotals = buildServiceTypeSubtotals(areaPayments);
            Map<String, BigDecimal> areaSubjectTypeSubtotals = buildSubjectTypeSubtotals(areaPayments);

            groups.add(ServicePaymentReportAreaGroupDTO.builder()
                    .projectAreaId(areaIdDTO)
                    .projectAreaName(areaName)
                    .projectAreaColor(areaColor)
                    .subtotalAmount(subtotal)
                    .paymentCount(areaPayments.size())
                    .subtotalsByServiceType(areaServiceTypeSubtotals)
                    .subtotalsBySubjectType(areaSubjectTypeSubtotals)
                    .buildingGroups(buildingGroups)
                    .build());
        }

        // Sort alphabetically, "Sin área asignada" goes last
        groups.sort((a, b) -> {
            if (a.projectAreaId() == null) return 1;
            if (b.projectAreaId() == null) return -1;
            return a.projectAreaName().compareToIgnoreCase(b.projectAreaName());
        });

        return groups;
    }

    /**
     * Builds building groups within an area.
     * Building services group by building, vehicle services group under "Sin Edificio asignado".
     */
    private List<ServicePaymentReportBuildingGroupDTO> buildBuildingGroups(List<ServicePayment> areaPayments, String areaName) {
        // Group by building, using -1L sentinel for payments without building (vehicle or unassigned)
        Map<Long, List<ServicePayment>> byBuilding = new LinkedHashMap<>();
        for (ServicePayment sp : areaPayments) {
            Long buildingId = (sp.getServiceAssignment() != null && sp.getServiceAssignment().getBuilding() != null)
                    ? sp.getServiceAssignment().getBuilding().getId()
                    : -1L;
            byBuilding.computeIfAbsent(buildingId, k -> new ArrayList<>()).add(sp);
        }

        List<ServicePaymentReportBuildingGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<ServicePayment>> entry : byBuilding.entrySet()) {
            Long buildingId = entry.getKey();
            List<ServicePayment> buildingPayments = entry.getValue();

            String buildingName;
            Long buildingIdDTO;

            if (buildingId == -1L) {
                buildingIdDTO = null;
                buildingName = "Sin Edificio asignado";
            } else {
                buildingIdDTO = buildingId;
                buildingName = buildingPayments.get(0).getServiceAssignment().getBuilding().getName();
            }

            // Build individual payment items sorted by service type then date desc
            List<ServicePaymentReportItemDTO> paymentItems = buildingPayments.stream()
                    .sorted(Comparator.comparing((ServicePayment sp) ->
                                    sp.getServiceAssignment().getServiceType().ordinal())
                            .thenComparing(ServicePayment::getPaymentDate, Comparator.reverseOrder()))
                    .map(sp -> new ServicePaymentReportItemDTO(
                            sp.getId(),
                            sp.getPaymentDate(),
                            sp.getAmount(),
                            sp.getYear(),
                            sp.getPeriod(),
                            sp.getServiceAssignment().getServiceType().name(),
                            sp.getServiceAssignment().getServiceSupplier().getSupplier().getLegalName(),
                            sp.getServiceAssignment().getServiceSupplier().getSupplier().getTradeName(),
                            sp.getReferenceNumber(),
                            sp.getPaymentMethod(),
                            sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getId() : null,
                            sp.getProjectAreaTask() != null ? sp.getProjectAreaTask().getName() : null,
                            buildingName,
                            sp.getServiceAssignment().getSubjectType().name(),
                            areaName
                    ))
                    .toList();

            BigDecimal total = buildingPayments.stream()
                    .map(ServicePayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, BigDecimal> serviceTypeSubtotals = buildServiceTypeSubtotals(buildingPayments);
            Map<String, BigDecimal> subjectTypeSubtotals = buildSubjectTypeSubtotals(buildingPayments);

            groups.add(ServicePaymentReportBuildingGroupDTO.builder()
                    .buildingId(buildingIdDTO)
                    .buildingName(buildingName)
                    .totalAmount(total)
                    .paymentCount(buildingPayments.size())
                    .subtotalsByServiceType(serviceTypeSubtotals)
                    .subtotalsBySubjectType(subjectTypeSubtotals)
                    .payments(paymentItems)
                    .build());
        }

        // Sort alphabetically, "Sin Edificio asignado" goes last
        groups.sort((a, b) -> {
            if (a.buildingId() == null) return 1;
            if (b.buildingId() == null) return -1;
            return a.buildingName().compareToIgnoreCase(b.buildingName());
        });

        return groups;
    }

    /**
     * Builds a map of subtotals keyed by ServiceType name.
     */
    private Map<String, BigDecimal> buildServiceTypeSubtotals(List<ServicePayment> payments) {
        Map<String, BigDecimal> subtotals = new LinkedHashMap<>();
        for (ServicePayment sp : payments) {
            String key = sp.getServiceAssignment().getServiceType().name();
            subtotals.merge(key, sp.getAmount(), BigDecimal::add);
        }
        return subtotals;
    }

    /**
     * Builds a map of subtotals keyed by SubjectType name (BUILDING, VEHICLE).
     */
    private Map<String, BigDecimal> buildSubjectTypeSubtotals(List<ServicePayment> payments) {
        Map<String, BigDecimal> subtotals = new LinkedHashMap<>();
        for (ServicePayment sp : payments) {
            String key = sp.getServiceAssignment().getSubjectType().name();
            subtotals.merge(key, sp.getAmount(), BigDecimal::add);
        }
        return subtotals;
    }

    private void validateServicePaymentFilters(ServicePaymentReportFilterDTO filters) {
        if (filters.startDate() != null && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.date.range.invalid"));
        }
        if (filters.minAmount() != null && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.amount.range.invalid"));
        }
    }

    private String buildServicePaymentPeriodDescription(ServicePaymentReportFilterDTO filters) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filters.startDate() != null && filters.endDate() != null) {
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

    // ═══════════════════════════════════════════════════════════════════════
    // FUEL LOAD REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public FuelLoadReportDTO generateFuelLoadReport(FuelLoadReportFilterDTO filters) {
        log.info("Generating fuel load report with filters: {}", filters);

        validateFuelLoadFilters(filters);

        Pageable pageable = PageRequest.of(0, 10000);

        List<Long> areaIds = filters.projectAreaIds();
        String fuelTypeStr = filters.fuelType() != null ? filters.fuelType().name() : null;

        List<FuelLoad> allLoads;
        if (areaIds != null && areaIds.size() > 1) {
            allLoads = new ArrayList<>();
            for (Long areaId : areaIds) {
                allLoads.addAll(fuelLoadRepository.findAllWithFilters(
                        filters.startDate(), filters.endDate(),
                        null, null, fuelTypeStr,
                        filters.vehicleId(), null,
                        areaId, null,
                        filters.gasStationId(), null, null,
                        filters.minAmount(), filters.maxAmount(),
                        null, null,
                        pageable
                ).getContent());
            }
        } else {
            Long effectiveAreaId = (areaIds != null && !areaIds.isEmpty()) ? areaIds.get(0) : null;
            allLoads = fuelLoadRepository.findAllWithFilters(
                    filters.startDate(), filters.endDate(),
                    null, null, fuelTypeStr,
                    filters.vehicleId(), null,
                    effectiveAreaId, null,
                    filters.gasStationId(), null, null,
                    filters.minAmount(), filters.maxAmount(),
                    null, null,
                    pageable
            ).getContent();
        }

        // In-memory filter: vehicle type (not supported by repository query)
        if (filters.vehicleTypeId() != null) {
            allLoads = allLoads.stream()
                    .filter(fl -> fl.getVehicle() != null
                            && fl.getVehicle().getVehicleType() != null
                            && filters.vehicleTypeId().equals(fl.getVehicle().getVehicleType().getId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Build area groups (without gas station grouping — frontend handles gas station toggle)
        List<FuelLoadReportAreaGroupDTO> areaGroups = buildFuelLoadAreaGroups(allLoads);

        // Build gas station groups
        List<FuelLoadReportGasStationGroupDTO> gasStationGroups = buildFuelLoadGasStationGroups(allLoads);

        // Calculate grand totals
        BigDecimal totalAmount = allLoads.stream()
                .map(FuelLoad::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiters = allLoads.stream()
                .map(FuelLoad::getLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> totalsByFuelType = buildFuelTypeAmountSubtotals(allLoads);
        Map<String, BigDecimal> litersByFuelType = buildFuelTypeLiterSubtotals(allLoads);
        Map<String, BigDecimal> totalsByGasStation = buildGasStationSubtotals(allLoads);

        String periodDesc = buildFuelLoadPeriodDescription(filters);

        return FuelLoadReportDTO.builder()
                .filters(filters)
                .areaGroups(areaGroups)
                .gasStationGroups(gasStationGroups)
                .totalAmount(totalAmount)
                .totalLiters(totalLiters)
                .totalCount(allLoads.size())
                .totalsByFuelType(totalsByFuelType)
                .litersByFuelType(litersByFuelType)
                .totalsByGasStation(totalsByGasStation)
                .generatedAt(LocalDateTime.now())
                .reportName("Reporte de Combustible")
                .periodDescription(periodDesc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateFuelLoadReportFile(FuelLoadReportFilterDTO filters, ReportFormat format) {
        log.info("Generating fuel load report file: format={}", format);

        FuelLoadReportDTO report = generateFuelLoadReport(filters);

        byte[] content;
        switch (format) {
            case EXCEL -> content = fuelLoadExcelExporter.export(report);
            case PDF -> content = fuelLoadPdfExporter.export(report);
            default -> throw new InvalidReportFormatException(format.name());
        }

        String filename = "reporte_combustible_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + format.getFileExtension() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, format.getContentType())
                .body(content);
    }

    private List<FuelLoadReportAreaGroupDTO> buildFuelLoadAreaGroups(List<FuelLoad> loads) {
        Map<Long, List<FuelLoad>> byArea = new LinkedHashMap<>();

        for (FuelLoad fl : loads) {
            Long areaId = fl.getProjectArea() != null ? fl.getProjectArea().getId() : -1L;
            byArea.computeIfAbsent(areaId, k -> new ArrayList<>()).add(fl);
        }

        List<FuelLoadReportAreaGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<FuelLoad>> entry : byArea.entrySet()) {
            Long areaId = entry.getKey();
            List<FuelLoad> areaLoads = entry.getValue();

            String areaName;
            String areaColor;
            Long areaIdDTO;

            if (areaId == -1L) {
                areaIdDTO = null;
                areaName = "Sin área asignada";
                areaColor = null;
            } else {
                areaIdDTO = areaId;
                FuelLoad first = areaLoads.get(0);
                areaName = first.getProjectArea().getName();
                areaColor = first.getProjectArea().getColor();
            }

            List<FuelLoadReportVehicleGroupDTO> vehicleGroups = buildFuelLoadVehicleGroups(areaLoads);

            BigDecimal subtotal = areaLoads.stream()
                    .map(FuelLoad::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal subtotalLiters = areaLoads.stream()
                    .map(FuelLoad::getLiters)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            groups.add(FuelLoadReportAreaGroupDTO.builder()
                    .projectAreaId(areaIdDTO)
                    .projectAreaName(areaName)
                    .projectAreaColor(areaColor)
                    .subtotalAmount(subtotal)
                    .subtotalLiters(subtotalLiters)
                    .loadCount(areaLoads.size())
                    .subtotalsByFuelType(buildFuelTypeAmountSubtotals(areaLoads))
                    .litersByFuelType(buildFuelTypeLiterSubtotals(areaLoads))
                    .vehicleGroups(vehicleGroups)
                    .build());
        }

        groups.sort((a, b) -> {
            if (a.projectAreaId() == null) return 1;
            if (b.projectAreaId() == null) return -1;
            return a.projectAreaName().compareToIgnoreCase(b.projectAreaName());
        });

        return groups;
    }

    private List<FuelLoadReportVehicleGroupDTO> buildFuelLoadVehicleGroups(List<FuelLoad> areaLoads) {
        Map<Long, List<FuelLoad>> byVehicle = new LinkedHashMap<>();
        for (FuelLoad fl : areaLoads) {
            Long vehicleId = fl.getVehicle() != null ? fl.getVehicle().getId() : -1L;
            byVehicle.computeIfAbsent(vehicleId, k -> new ArrayList<>()).add(fl);
        }

        List<FuelLoadReportVehicleGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<FuelLoad>> entry : byVehicle.entrySet()) {
            Long vehicleId = entry.getKey();
            List<FuelLoad> vehicleLoads = entry.getValue();

            String vehicleName;
            String vehicleDescription;
            Long vehicleIdDTO;

            if (vehicleId == -1L) {
                vehicleIdDTO = null;
                vehicleName = "Bidón";
                vehicleDescription = null;
            } else {
                vehicleIdDTO = vehicleId;
                var vehicle = vehicleLoads.get(0).getVehicle();
                vehicleName = vehicle.getLicensePlate();
                vehicleDescription = buildVehicleDescription(vehicle);
            }

            List<FuelLoadReportItemDTO> items = vehicleLoads.stream()
                    .sorted(Comparator.comparing(FuelLoad::getDate, Comparator.reverseOrder()))
                    .map(fl -> new FuelLoadReportItemDTO(
                            fl.getId(),
                            fl.getDate(),
                            fl.getFuelType().name(),
                            fl.getLiters(),
                            fl.getPricePerLiter(),
                            fl.getTotalAmount(),
                            fl.getVehicle() != null ? fl.getVehicle().getLicensePlate() : "Bidón",
                            fl.getVehicle() != null ? buildVehicleDescription(fl.getVehicle()) : null,
                            fl.getGasStation() != null && fl.getGasStation().getSupplier() != null
                                    ? fl.getGasStation().getSupplier().getLegalName() : "Sin estación",
                            fl.getBranchCode(),
                            fl.getTicketNumber(),
                            fl.getProjectAreaTask() != null ? fl.getProjectAreaTask().getId() : null,
                            fl.getProjectAreaTask() != null ? fl.getProjectAreaTask().getName() : null,
                            fl.getProjectArea() != null ? fl.getProjectArea().getName() : "Sin área"
                    ))
                    .toList();

            BigDecimal total = vehicleLoads.stream()
                    .map(FuelLoad::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalLiters = vehicleLoads.stream()
                    .map(FuelLoad::getLiters)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            groups.add(FuelLoadReportVehicleGroupDTO.builder()
                    .vehicleId(vehicleIdDTO)
                    .vehicleName(vehicleName)
                    .vehicleDescription(vehicleDescription)
                    .totalAmount(total)
                    .totalLiters(totalLiters)
                    .loadCount(vehicleLoads.size())
                    .subtotalsByFuelType(buildFuelTypeAmountSubtotals(vehicleLoads))
                    .litersByFuelType(buildFuelTypeLiterSubtotals(vehicleLoads))
                    .loads(items)
                    .build());
        }

        groups.sort((a, b) -> {
            if (a.vehicleId() == null) return 1;
            if (b.vehicleId() == null) return -1;
            return a.vehicleName().compareToIgnoreCase(b.vehicleName());
        });

        return groups;
    }

    private List<FuelLoadReportGasStationGroupDTO> buildFuelLoadGasStationGroups(List<FuelLoad> loads) {
        Map<Long, List<FuelLoad>> byStation = new LinkedHashMap<>();

        for (FuelLoad fl : loads) {
            Long stationId = fl.getGasStation() != null ? fl.getGasStation().getId() : -1L;
            byStation.computeIfAbsent(stationId, k -> new ArrayList<>()).add(fl);
        }

        List<FuelLoadReportGasStationGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<FuelLoad>> entry : byStation.entrySet()) {
            Long stationId = entry.getKey();
            List<FuelLoad> stationLoads = entry.getValue();

            String stationName;
            Long stationIdDTO;

            if (stationId == -1L) {
                stationIdDTO = null;
                stationName = "Sin estación asignada";
            } else {
                stationIdDTO = stationId;
                var gs = stationLoads.get(0).getGasStation();
                stationName = gs.getSupplier() != null ? gs.getSupplier().getLegalName() : "Estación #" + stationId;
            }

            List<FuelLoadReportAreaGroupDTO> areaGroups = buildFuelLoadAreaGroups(stationLoads);

            BigDecimal subtotal = stationLoads.stream()
                    .map(FuelLoad::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal subtotalLiters = stationLoads.stream()
                    .map(FuelLoad::getLiters)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            groups.add(FuelLoadReportGasStationGroupDTO.builder()
                    .gasStationId(stationIdDTO)
                    .gasStationName(stationName)
                    .subtotalAmount(subtotal)
                    .subtotalLiters(subtotalLiters)
                    .loadCount(stationLoads.size())
                    .subtotalsByFuelType(buildFuelTypeAmountSubtotals(stationLoads))
                    .litersByFuelType(buildFuelTypeLiterSubtotals(stationLoads))
                    .areaGroups(areaGroups)
                    .build());
        }

        groups.sort((a, b) -> {
            if (a.gasStationId() == null) return 1;
            if (b.gasStationId() == null) return -1;
            return a.gasStationName().compareToIgnoreCase(b.gasStationName());
        });

        return groups;
    }

    private String buildVehicleDescription(PSG.backEnd.model.entity.vehicle.Vehicle vehicle) {
        StringBuilder sb = new StringBuilder();
        if (vehicle.getBrand() != null) sb.append(vehicle.getBrand());
        if (vehicle.getModel() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(vehicle.getModel());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private Map<String, BigDecimal> buildFuelTypeAmountSubtotals(List<FuelLoad> loads) {
        Map<String, BigDecimal> subtotals = new LinkedHashMap<>();
        for (FuelLoad fl : loads) {
            String key = fl.getFuelType().name();
            subtotals.merge(key, fl.getTotalAmount(), BigDecimal::add);
        }
        return subtotals;
    }

    private Map<String, BigDecimal> buildFuelTypeLiterSubtotals(List<FuelLoad> loads) {
        Map<String, BigDecimal> subtotals = new LinkedHashMap<>();
        for (FuelLoad fl : loads) {
            String key = fl.getFuelType().name();
            subtotals.merge(key, fl.getLiters(), BigDecimal::add);
        }
        return subtotals;
    }

    private Map<String, BigDecimal> buildGasStationSubtotals(List<FuelLoad> loads) {
        Map<String, BigDecimal> subtotals = new LinkedHashMap<>();
        for (FuelLoad fl : loads) {
            String key = fl.getGasStation() != null && fl.getGasStation().getSupplier() != null
                    ? fl.getGasStation().getSupplier().getLegalName()
                    : "Sin estación";
            subtotals.merge(key, fl.getTotalAmount(), BigDecimal::add);
        }
        return subtotals;
    }

    private void validateFuelLoadFilters(FuelLoadReportFilterDTO filters) {
        if (filters.startDate() != null && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.date.range.invalid"));
        }
        if (filters.minAmount() != null && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.amount.range.invalid"));
        }
    }

    private String buildFuelLoadPeriodDescription(FuelLoadReportFilterDTO filters) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filters.startDate() != null && filters.endDate() != null) {
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

    // ═══════════════════════════════════════════════════════════════════════
    // REPAIR REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public RepairReportDTO generateRepairReport(RepairReportFilterDTO filters) {
        log.info("Generating repair report with filters: {}", filters);

        validateRepairFilters(filters);

        Pageable pageable = PageRequest.of(0, 10000);

        List<Long> areaIds = filters.projectAreaIds();

        List<Repair> allRepairs;
        if (areaIds != null && areaIds.size() > 1) {
            allRepairs = new ArrayList<>();
            for (Long areaId : areaIds) {
                allRepairs.addAll(repairRepository.findAllWithFilters(
                        filters.startDate(), filters.endDate(),
                        filters.vehicleId(), null,
                        areaId,
                        filters.minAmount(), filters.maxAmount(),
                        filters.supplierId(), null,
                        null, null, null, null, null, null, false,
                        pageable
                ).getContent());
            }
        } else {
            Long effectiveAreaId = (areaIds != null && !areaIds.isEmpty()) ? areaIds.get(0) : null;
            allRepairs = repairRepository.findAllWithFilters(
                    filters.startDate(), filters.endDate(),
                    filters.vehicleId(), null,
                    effectiveAreaId,
                    filters.minAmount(), filters.maxAmount(),
                    filters.supplierId(), null,
                    null, null, null, null, null, null, false,
                    pageable
            ).getContent();
        }

        List<RepairReportAreaGroupDTO> areaGroups = buildRepairAreaGroups(allRepairs);

        BigDecimal totalMaterialCost = BigDecimal.ZERO;
        BigDecimal totalLaborCost = BigDecimal.ZERO;
        for (Repair r : allRepairs) {
            for (RepairItem item : r.getItems()) {
                BigDecimal amt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
                if (item.getItemType() == RepairItemType.MATERIAL) {
                    totalMaterialCost = totalMaterialCost.add(amt);
                } else if (item.getItemType() == RepairItemType.MANO_DE_OBRA) {
                    totalLaborCost = totalLaborCost.add(amt);
                }
            }
        }

        BigDecimal totalAmount = totalMaterialCost.add(totalLaborCost);

        Map<String, BigDecimal> totalsByItemType = new LinkedHashMap<>();
        totalsByItemType.put(RepairItemType.MATERIAL.name(), totalMaterialCost);
        totalsByItemType.put(RepairItemType.MANO_DE_OBRA.name(), totalLaborCost);

        String periodDesc = buildRepairPeriodDescription(filters);

        return RepairReportDTO.builder()
                .filters(filters)
                .areaGroups(areaGroups)
                .totalAmount(totalAmount)
                .totalCount(allRepairs.size())
                .totalMaterialCost(totalMaterialCost)
                .totalLaborCost(totalLaborCost)
                .totalsByItemType(totalsByItemType)
                .generatedAt(LocalDateTime.now())
                .reportName("Reporte de Reparaciones")
                .periodDescription(periodDesc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateRepairReportFile(RepairReportFilterDTO filters, ReportFormat format) {
        log.info("Generating repair report file: format={}", format);

        RepairReportDTO report = generateRepairReport(filters);

        byte[] content;
        switch (format) {
            case EXCEL -> content = repairExcelExporter.export(report);
            case PDF -> content = repairPdfExporter.export(report);
            default -> throw new InvalidReportFormatException(format.name());
        }

        String filename = "reporte_reparaciones_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + format.getFileExtension() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, format.getContentType())
                .body(content);
    }

    private List<RepairReportAreaGroupDTO> buildRepairAreaGroups(List<Repair> repairs) {
        Map<Long, List<Repair>> byArea = new LinkedHashMap<>();

        for (Repair r : repairs) {
            Long areaId = r.getVehicle() != null && r.getVehicle().getProjectArea() != null
                    ? r.getVehicle().getProjectArea().getId() : -1L;
            byArea.computeIfAbsent(areaId, k -> new ArrayList<>()).add(r);
        }

        List<RepairReportAreaGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<Repair>> entry : byArea.entrySet()) {
            Long areaId = entry.getKey();
            List<Repair> areaRepairs = entry.getValue();

            String areaName;
            String areaColor;
            Long areaIdDTO;

            if (areaId == -1L) {
                areaIdDTO = null;
                areaName = "Sin área asignada";
                areaColor = null;
            } else {
                areaIdDTO = areaId;
                var pa = areaRepairs.get(0).getVehicle().getProjectArea();
                areaName = pa.getName();
                areaColor = pa.getColor();
            }

            List<RepairReportVehicleGroupDTO> vehicleGroups = buildRepairVehicleGroups(areaRepairs, areaName);

            BigDecimal materialSubtotal = BigDecimal.ZERO;
            BigDecimal laborSubtotal = BigDecimal.ZERO;
            for (Repair r : areaRepairs) {
                for (RepairItem item : r.getItems()) {
                    BigDecimal amt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
                    if (item.getItemType() == RepairItemType.MATERIAL) {
                        materialSubtotal = materialSubtotal.add(amt);
                    } else if (item.getItemType() == RepairItemType.MANO_DE_OBRA) {
                        laborSubtotal = laborSubtotal.add(amt);
                    }
                }
            }

            groups.add(RepairReportAreaGroupDTO.builder()
                    .projectAreaId(areaIdDTO)
                    .projectAreaName(areaName)
                    .projectAreaColor(areaColor)
                    .subtotalAmount(materialSubtotal.add(laborSubtotal))
                    .repairCount(areaRepairs.size())
                    .materialSubtotal(materialSubtotal)
                    .laborSubtotal(laborSubtotal)
                    .vehicleGroups(vehicleGroups)
                    .build());
        }

        groups.sort((a, b) -> {
            if (a.projectAreaId() == null) return 1;
            if (b.projectAreaId() == null) return -1;
            return a.projectAreaName().compareToIgnoreCase(b.projectAreaName());
        });

        return groups;
    }

    private List<RepairReportVehicleGroupDTO> buildRepairVehicleGroups(List<Repair> areaRepairs, String areaName) {
        Map<Long, List<Repair>> byVehicle = new LinkedHashMap<>();
        for (Repair r : areaRepairs) {
            Long vehicleId = r.getVehicle() != null ? r.getVehicle().getId() : -1L;
            byVehicle.computeIfAbsent(vehicleId, k -> new ArrayList<>()).add(r);
        }

        List<RepairReportVehicleGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<Repair>> entry : byVehicle.entrySet()) {
            Long vehicleId = entry.getKey();
            List<Repair> vehicleRepairs = entry.getValue();

            String licensePlate;
            String brand;
            String model;
            Long vehicleIdDTO;

            if (vehicleId == -1L) {
                vehicleIdDTO = null;
                licensePlate = "Sin vehículo";
                brand = null;
                model = null;
            } else {
                vehicleIdDTO = vehicleId;
                var vehicle = vehicleRepairs.get(0).getVehicle();
                licensePlate = vehicle.getLicensePlate();
                brand = vehicle.getBrand();
                model = vehicle.getModel();
            }

            BigDecimal materialSubtotal = BigDecimal.ZERO;
            BigDecimal laborSubtotal = BigDecimal.ZERO;
            for (Repair r : vehicleRepairs) {
                for (RepairItem item : r.getItems()) {
                    BigDecimal amt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
                    if (item.getItemType() == RepairItemType.MATERIAL) {
                        materialSubtotal = materialSubtotal.add(amt);
                    } else if (item.getItemType() == RepairItemType.MANO_DE_OBRA) {
                        laborSubtotal = laborSubtotal.add(amt);
                    }
                }
            }

            List<RepairReportItemDTO> items = vehicleRepairs.stream()
                    .sorted(Comparator.comparing(Repair::getDate, Comparator.reverseOrder()))
                    .map(r -> {
                        BigDecimal matCost = BigDecimal.ZERO;
                        BigDecimal labCost = BigDecimal.ZERO;
                        boolean hasLinked = false;
                        for (RepairItem ri : r.getItems()) {
                            BigDecimal amt = ri.getAmount() != null ? ri.getAmount() : BigDecimal.ZERO;
                            if (ri.getItemType() == RepairItemType.MATERIAL) {
                                matCost = matCost.add(amt);
                            } else if (ri.getItemType() == RepairItemType.MANO_DE_OBRA) {
                                labCost = labCost.add(amt);
                            }
                            if (ri.getTransactionalDocument() != null) {
                                hasLinked = true;
                            }
                        }
                        return new RepairReportItemDTO(
                                r.getId(),
                                r.getDate(),
                                r.getDescription(),
                                r.getMileage(),
                                matCost,
                                labCost,
                                matCost.add(labCost),
                                r.getSupplier() != null ? r.getSupplier().getLegalName() : null,
                                r.getItems().size(),
                                hasLinked,
                                r.getVehicle() != null ? r.getVehicle().getLicensePlate() : null,
                                r.getVehicle() != null ? buildVehicleDescription(r.getVehicle()) : null,
                                areaName
                        );
                    })
                    .toList();

            groups.add(RepairReportVehicleGroupDTO.builder()
                    .vehicleId(vehicleIdDTO)
                    .vehicleLicensePlate(licensePlate)
                    .vehicleBrand(brand)
                    .vehicleModel(model)
                    .totalAmount(materialSubtotal.add(laborSubtotal))
                    .repairCount(vehicleRepairs.size())
                    .materialSubtotal(materialSubtotal)
                    .laborSubtotal(laborSubtotal)
                    .repairs(items)
                    .build());
        }

        groups.sort((a, b) -> {
            if (a.vehicleId() == null) return 1;
            if (b.vehicleId() == null) return -1;
            return a.vehicleLicensePlate().compareToIgnoreCase(b.vehicleLicensePlate());
        });

        return groups;
    }

    private void validateRepairFilters(RepairReportFilterDTO filters) {
        if (filters.startDate() != null && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.date.range.invalid"));
        }
        if (filters.minAmount() != null && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.amount.range.invalid"));
        }
    }

    private String buildRepairPeriodDescription(RepairReportFilterDTO filters) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filters.startDate() != null && filters.endDate() != null) {
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

    // ═══════════════════════════════════════════════════════════════════════
    // STOCK PURCHASE REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public StockPurchaseReportDTO generateStockPurchaseReport(StockPurchaseReportFilterDTO filters) {
        log.info("Generating stock purchase report with filters: {}", filters);

        validateStockPurchaseFilters(filters);

        Pageable pageable = PageRequest.of(0, 10000);

        List<StockPurchase> allPurchases = stockPurchaseRepository.findAllWithFilters(
                filters.startDate(), filters.endDate(),
                filters.stockId(), null, null,
                null, null,
                filters.minAmount(), filters.maxAmount(),
                null, null,
                pageable
        ).getContent();

        // Filter by stock categories if specified
        List<String> catFilter = filters.stockCategories();
        if (catFilter != null && !catFilter.isEmpty()) {
            Set<String> catSet = new HashSet<>(catFilter);
            allPurchases = allPurchases.stream()
                    .filter(sp -> {
                        Stock stock = stockRepository.findById(sp.getStockId()).orElse(null);
                        return stock != null && stock.getStockCategory() != null
                                && catSet.contains(stock.getStockCategory().name());
                    })
                    .toList();
        }

        List<StockPurchaseReportCategoryGroupDTO> categoryGroups = buildStockPurchaseCategoryGroups(allPurchases);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        Map<String, BigDecimal> totalsByCategory = new LinkedHashMap<>();

        for (StockPurchaseReportCategoryGroupDTO group : categoryGroups) {
            totalAmount = totalAmount.add(group.subtotalAmount());
            totalQuantity = totalQuantity.add(group.subtotalQuantity());
            totalsByCategory.put(group.categoryName(), group.subtotalAmount());
        }

        String periodDesc = buildStockPurchasePeriodDescription(filters);

        return StockPurchaseReportDTO.builder()
                .filters(filters)
                .categoryGroups(categoryGroups)
                .totalAmount(totalAmount)
                .totalCount(allPurchases.size())
                .totalQuantity(totalQuantity)
                .totalsByCategory(totalsByCategory)
                .generatedAt(LocalDateTime.now())
                .reportName("Reporte de Compras de Stock")
                .periodDescription(periodDesc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateStockPurchaseReportFile(StockPurchaseReportFilterDTO filters, ReportFormat format) {
        log.info("Generating stock purchase report file: format={}", format);

        StockPurchaseReportDTO report = generateStockPurchaseReport(filters);

        byte[] content;
        switch (format) {
            case EXCEL -> content = stockPurchaseExcelExporter.export(report);
            case PDF -> content = stockPurchasePdfExporter.export(report);
            default -> throw new InvalidReportFormatException(format.name());
        }

        String filename = "reporte_compras_stock_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + format.getFileExtension() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, format.getContentType())
                .body(content);
    }

    private List<StockPurchaseReportCategoryGroupDTO> buildStockPurchaseCategoryGroups(List<StockPurchase> purchases) {
        // Load all stocks needed (batch via stockId)
        Map<Long, Stock> stockCache = new HashMap<>();
        for (StockPurchase sp : purchases) {
            stockCache.computeIfAbsent(sp.getStockId(),
                    id -> stockRepository.findById(id).orElse(null));
        }

        // Group by category
        Map<String, List<StockPurchase>> byCategory = new LinkedHashMap<>();
        for (StockPurchase sp : purchases) {
            Stock stock = stockCache.get(sp.getStockId());
            String catKey = (stock != null && stock.getStockCategory() != null)
                    ? stock.getStockCategory().name() : "OTROS";
            byCategory.computeIfAbsent(catKey, k -> new ArrayList<>()).add(sp);
        }

        List<StockPurchaseReportCategoryGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<String, List<StockPurchase>> entry : byCategory.entrySet()) {
            String catKey = entry.getKey();
            List<StockPurchase> catPurchases = entry.getValue();

            String catName;
            try {
                catName = PSG.backEnd.model.enums.StockCategory.valueOf(catKey).getDisplayName();
            } catch (IllegalArgumentException e) {
                catName = catKey;
            }

            List<StockPurchaseReportStockGroupDTO> stockGroups = buildStockPurchaseStockGroups(
                    catPurchases, stockCache, catName);

            BigDecimal subtotalAmount = BigDecimal.ZERO;
            BigDecimal subtotalQuantity = BigDecimal.ZERO;
            for (StockPurchase sp : catPurchases) {
                subtotalAmount = subtotalAmount.add(
                        sp.getTotalAmount() != null ? sp.getTotalAmount() : BigDecimal.ZERO);
                subtotalQuantity = subtotalQuantity.add(
                        sp.getQuantity() != null ? sp.getQuantity() : BigDecimal.ZERO);
            }

            groups.add(StockPurchaseReportCategoryGroupDTO.builder()
                    .categoryName(catName)
                    .categoryKey(catKey)
                    .subtotalAmount(subtotalAmount)
                    .purchaseCount(catPurchases.size())
                    .subtotalQuantity(subtotalQuantity)
                    .stockGroups(stockGroups)
                    .build());
        }

        groups.sort((a, b) -> a.categoryName().compareToIgnoreCase(b.categoryName()));
        return groups;
    }

    private List<StockPurchaseReportStockGroupDTO> buildStockPurchaseStockGroups(
            List<StockPurchase> catPurchases, Map<Long, Stock> stockCache, String categoryName) {

        Map<Long, List<StockPurchase>> byStock = new LinkedHashMap<>();
        for (StockPurchase sp : catPurchases) {
            byStock.computeIfAbsent(sp.getStockId(), k -> new ArrayList<>()).add(sp);
        }

        List<StockPurchaseReportStockGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<StockPurchase>> entry : byStock.entrySet()) {
            Long stockId = entry.getKey();
            List<StockPurchase> stockPurchases = entry.getValue();

            Stock stock = stockCache.get(stockId);
            String stockName = (stock != null) ? stock.getName() : "Item #" + stockId;

            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalQuantity = BigDecimal.ZERO;
            for (StockPurchase sp : stockPurchases) {
                totalAmount = totalAmount.add(
                        sp.getTotalAmount() != null ? sp.getTotalAmount() : BigDecimal.ZERO);
                totalQuantity = totalQuantity.add(
                        sp.getQuantity() != null ? sp.getQuantity() : BigDecimal.ZERO);
            }

            List<StockPurchaseReportItemDTO> items = stockPurchases.stream()
                    .sorted(Comparator.comparing(StockPurchase::getDate, Comparator.reverseOrder()))
                    .map(sp -> new StockPurchaseReportItemDTO(
                            sp.getId(),
                            sp.getDate(),
                            stockName,
                            categoryName,
                            sp.getQuantity(),
                            sp.getUnitPrice(),
                            sp.getTotalAmount() != null ? sp.getTotalAmount() : BigDecimal.ZERO,
                            sp.getNotes(),
                            sp.getTransactionalDocumentId() != null
                    ))
                    .toList();

            groups.add(StockPurchaseReportStockGroupDTO.builder()
                    .stockId(stockId)
                    .stockName(stockName)
                    .totalAmount(totalAmount)
                    .purchaseCount(stockPurchases.size())
                    .totalQuantity(totalQuantity)
                    .purchases(items)
                    .build());
        }

        groups.sort((a, b) -> a.stockName().compareToIgnoreCase(b.stockName()));
        return groups;
    }

    private void validateStockPurchaseFilters(StockPurchaseReportFilterDTO filters) {
        if (filters.startDate() != null && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.date.range.invalid"));
        }
        if (filters.minAmount() != null && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.amount.range.invalid"));
        }
    }

    private String buildStockPurchasePeriodDescription(StockPurchaseReportFilterDTO filters) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filters.startDate() != null && filters.endDate() != null) {
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

    // ═══════════════════════════════════════════════════════════════════════
    // POLICY PAYMENT REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PolicyPaymentReportDTO generatePolicyPaymentReport(PolicyPaymentReportFilterDTO filters) {
        log.info("Generating policy payment report with filters: {}", filters);

        validatePolicyPaymentFilters(filters);

        Pageable pageable = PageRequest.of(0, 10000);

        List<InsurancePolicyPaymentDetail> allPayments = insurancePolicyPaymentDetailRepository.findAllWithFilters(
                filters.insurancePolicyId(),
                filters.startDate(), filters.endDate(),
                filters.minAmount(), filters.maxAmount(),
                pageable
        ).getContent();

        // Filter by policy types in memory (Option B from spec)
        List<String> typeFilter = filters.policyTypes();
        if (typeFilter != null && !typeFilter.isEmpty()) {
            Set<String> typeSet = new HashSet<>(typeFilter);
            allPayments = allPayments.stream()
                    .filter(ippd -> ippd.getInsurancePolicy() != null
                            && ippd.getInsurancePolicy().getPolicyType() != null
                            && typeSet.contains(ippd.getInsurancePolicy().getPolicyType().name()))
                    .toList();
        }

        List<PolicyPaymentReportTypeGroupDTO> typeGroups = buildPolicyPaymentTypeGroups(allPayments);

        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        BigDecimal totalExpectedAmount = BigDecimal.ZERO;
        int totalPaymentCount = 0;
        Set<Long> distinctPolicies = new HashSet<>();

        for (PolicyPaymentReportTypeGroupDTO tg : typeGroups) {
            totalPaidAmount = totalPaidAmount.add(tg.subtotalPaid());
            totalExpectedAmount = totalExpectedAmount.add(tg.subtotalExpected());
            totalPaymentCount += tg.paymentCount();
            for (PolicyPaymentReportPolicyGroupDTO pg : tg.policyGroups()) {
                distinctPolicies.add(pg.insurancePolicyId());
            }
        }

        BigDecimal totalDifference = totalPaidAmount.subtract(totalExpectedAmount);
        String periodDesc = buildPolicyPaymentPeriodDescription(filters);

        return PolicyPaymentReportDTO.builder()
                .filters(filters)
                .typeGroups(typeGroups)
                .totalPaidAmount(totalPaidAmount)
                .totalExpectedAmount(totalExpectedAmount)
                .totalDifference(totalDifference)
                .totalPaymentCount(totalPaymentCount)
                .totalPolicyCount(distinctPolicies.size())
                .generatedAt(LocalDateTime.now())
                .reportName("Reporte de Pagos de Póliza")
                .periodDescription(periodDesc)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generatePolicyPaymentReportFile(PolicyPaymentReportFilterDTO filters, ReportFormat format) {
        log.info("Generating policy payment report file: format={}", format);

        PolicyPaymentReportDTO report = generatePolicyPaymentReport(filters);

        byte[] content;
        switch (format) {
            case EXCEL -> content = policyPaymentExcelExporter.export(report);
            case PDF -> content = policyPaymentPdfExporter.export(report);
            default -> throw new InvalidReportFormatException(format.name());
        }

        String filename = "reporte_pagos_poliza_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + format.getFileExtension() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, format.getContentType())
                .body(content);
    }

    private List<PolicyPaymentReportTypeGroupDTO> buildPolicyPaymentTypeGroups(List<InsurancePolicyPaymentDetail> payments) {
        // Group by policy type
        Map<String, List<InsurancePolicyPaymentDetail>> byType = new LinkedHashMap<>();
        for (InsurancePolicyPaymentDetail ippd : payments) {
            InsurancePolicy policy = ippd.getInsurancePolicy();
            String typeKey = (policy != null && policy.getPolicyType() != null)
                    ? policy.getPolicyType().name() : "OTROS";
            byType.computeIfAbsent(typeKey, k -> new ArrayList<>()).add(ippd);
        }

        List<PolicyPaymentReportTypeGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<String, List<InsurancePolicyPaymentDetail>> entry : byType.entrySet()) {
            String typeKey = entry.getKey();
            List<InsurancePolicyPaymentDetail> typePayments = entry.getValue();

            String typeName;
            try {
                typeName = PolicyType.valueOf(typeKey).getDisplayName();
            } catch (IllegalArgumentException e) {
                typeName = typeKey;
            }

            List<PolicyPaymentReportPolicyGroupDTO> policyGroups = buildPolicyPaymentPolicyGroups(typePayments, typeName);

            BigDecimal subtotalPaid = BigDecimal.ZERO;
            BigDecimal subtotalExpected = BigDecimal.ZERO;
            int paymentCount = 0;

            for (PolicyPaymentReportPolicyGroupDTO pg : policyGroups) {
                subtotalPaid = subtotalPaid.add(pg.totalPaid());
                subtotalExpected = subtotalExpected.add(pg.expectedAmount());
                paymentCount += pg.paymentCount();
            }

            groups.add(PolicyPaymentReportTypeGroupDTO.builder()
                    .policyTypeName(typeName)
                    .policyTypeKey(typeKey)
                    .subtotalPaid(subtotalPaid)
                    .subtotalExpected(subtotalExpected)
                    .subtotalDifference(subtotalPaid.subtract(subtotalExpected))
                    .paymentCount(paymentCount)
                    .policyCount(policyGroups.size())
                    .policyGroups(policyGroups)
                    .build());
        }

        groups.sort((a, b) -> a.policyTypeName().compareToIgnoreCase(b.policyTypeName()));
        return groups;
    }

    private List<PolicyPaymentReportPolicyGroupDTO> buildPolicyPaymentPolicyGroups(
            List<InsurancePolicyPaymentDetail> typePayments, String typeName) {

        // Group by insurance policy
        Map<Long, List<InsurancePolicyPaymentDetail>> byPolicy = new LinkedHashMap<>();
        for (InsurancePolicyPaymentDetail ippd : typePayments) {
            Long policyId = ippd.getInsurancePolicy() != null ? ippd.getInsurancePolicy().getId() : -1L;
            byPolicy.computeIfAbsent(policyId, k -> new ArrayList<>()).add(ippd);
        }

        List<PolicyPaymentReportPolicyGroupDTO> groups = new ArrayList<>();

        for (Map.Entry<Long, List<InsurancePolicyPaymentDetail>> entry : byPolicy.entrySet()) {
            Long policyId = entry.getKey();
            List<InsurancePolicyPaymentDetail> policyPayments = entry.getValue();

            InsurancePolicy policy = policyPayments.get(0).getInsurancePolicy();

            String policyNumber = policy != null ? policy.getPolicyNumber() : "Sin póliza";
            String termNumber = policy != null ? policy.getTermNumber() : null;
            String policyStatus = (policy != null && policy.getPolicyStatus() != null)
                    ? policy.getPolicyStatus().getDisplayName() : null;
            String paymentFrequency = (policy != null && policy.getPaymentFrequency() != null)
                    ? policy.getPaymentFrequency().getDisplayName() : null;
            BigDecimal premioMensual = (policy != null && policy.getPremioMensual() != null)
                    ? policy.getPremioMensual() : BigDecimal.ZERO;

            BigDecimal totalPaid = BigDecimal.ZERO;
            for (InsurancePolicyPaymentDetail ippd : policyPayments) {
                PaymentDetails pd = ippd.getPaymentDetails();
                totalPaid = totalPaid.add(pd.getAmount() != null ? pd.getAmount() : BigDecimal.ZERO);
            }

            // Expected = premioMensual × number of payments
            BigDecimal expectedAmount = premioMensual.multiply(BigDecimal.valueOf(policyPayments.size()));
            BigDecimal difference = totalPaid.subtract(expectedAmount);

            List<PolicyPaymentReportPaymentDTO> paymentDTOs = policyPayments.stream()
                    .sorted(Comparator.comparing(ippd -> ippd.getPaymentDetails().getPaymentDate(), Comparator.reverseOrder()))
                    .map(ippd -> {
                        PaymentDetails pd = ippd.getPaymentDetails();
                        return new PolicyPaymentReportPaymentDTO(
                            ippd.getId(),
                            pd.getPaymentDate(),
                            pd.getAmount(),
                            ippd.getPeriodFrom(),
                            ippd.getPeriodTo(),
                            pd.getComment(),
                            policyNumber,
                            typeName,
                            premioMensual,
                            (pd.getAmount() != null ? pd.getAmount() : BigDecimal.ZERO).subtract(premioMensual)
                        );
                    })
                    .toList();

            // Build vehicle list for AUTOMOTOR policies
            List<PolicyPaymentReportVehicleDTO> insuredVehicles = null;
            if (policy != null && policy.getPolicyType() == PolicyType.AUTOMOTOR
                    && policy.getAutoPolicy() != null
                    && policy.getAutoPolicy().getPolicyVehicles() != null) {
                insuredVehicles = policy.getAutoPolicy().getPolicyVehicles().stream()
                        .filter(pv -> !Boolean.TRUE.equals(pv.getDeleted()))
                        .map(pv -> {
                            var vehicle = pv.getVehicle();
                            return new PolicyPaymentReportVehicleDTO(
                                    vehicle != null ? vehicle.getId() : null,
                                    vehicle != null ? vehicle.getLicensePlate() : null,
                                    vehicle != null ? vehicle.getBrand() : null,
                                    vehicle != null ? vehicle.getModel() : null,
                                    pv.getPremioMensual(),
                                    pv.getPremioTotal(),
                                    pv.getSumInsured(),
                                    (vehicle != null && vehicle.getProjectArea() != null)
                                            ? vehicle.getProjectArea().getName() : null
                            );
                        })
                        .toList();
            }

            groups.add(PolicyPaymentReportPolicyGroupDTO.builder()
                    .insurancePolicyId(policyId == -1L ? null : policyId)
                    .policyNumber(policyNumber)
                    .termNumber(termNumber)
                    .policyStatus(policyStatus)
                    .paymentFrequency(paymentFrequency)
                    .premioMensual(premioMensual)
                    .totalPaid(totalPaid)
                    .expectedAmount(expectedAmount)
                    .difference(difference)
                    .paymentCount(policyPayments.size())
                    .payments(paymentDTOs)
                    .insuredVehicles(insuredVehicles)
                    .build());
        }

        groups.sort((a, b) -> {
            if (a.policyNumber() == null) return 1;
            if (b.policyNumber() == null) return -1;
            return a.policyNumber().compareToIgnoreCase(b.policyNumber());
        });

        return groups;
    }

    private void validatePolicyPaymentFilters(PolicyPaymentReportFilterDTO filters) {
        if (filters.startDate() != null && filters.endDate() != null
                && filters.startDate().isAfter(filters.endDate())) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.date.range.invalid"));
        }
        if (filters.minAmount() != null && filters.maxAmount() != null
                && filters.minAmount().compareTo(filters.maxAmount()) > 0) {
            throw new InvalidReportFilterException(
                    messageSourceHelper.getMessage("report.filter.amount.range.invalid"));
        }
    }

    private String buildPolicyPaymentPeriodDescription(PolicyPaymentReportFilterDTO filters) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (filters.startDate() != null && filters.endDate() != null) {
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
