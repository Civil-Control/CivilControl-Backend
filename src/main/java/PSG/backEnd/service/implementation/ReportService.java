package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.report.InvalidReportFilterException;
import PSG.backEnd.exception.report.InvalidReportFormatException;
import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.MoneyOutflowReportPreviewDTO;
import PSG.backEnd.model.dto.report.ReportFilterDTO;
import PSG.backEnd.model.dto.report.ReportItemDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.entity.insurance.InsurancePolicy;
import PSG.backEnd.model.entity.insurance.PolicyVehicle;
import PSG.backEnd.model.entity.serviceSupplier.ServicePayment;
import PSG.backEnd.model.entity.vehicle.LicencePlatePayment;
import PSG.backEnd.model.entity.vehicle.Repair;
import PSG.backEnd.model.enums.MoneyOutflowCategory;
import PSG.backEnd.model.enums.ReportFormat;
import PSG.backEnd.model.enums.vehicle.PolicyStatus;
import PSG.backEnd.model.enums.vehicle.PolicyType;
import PSG.backEnd.model.enums.vehicle.RepairType;
import PSG.backEnd.repository.*;
import PSG.backEnd.service.export.IReportExporter;
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

    // Repositories for data collection
    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final ServicePaymentRepository servicePaymentRepository;
    private final LicencePlatePaymentRepository licencePlatePaymentRepository;
    private final FuelLoadRepository fuelLoadRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final PolicyVehicleRepository policyVehicleRepository;
    private final RepairRepository repairRepository;
    private final ProjectAreaRepository projectAreaRepository;
    private final MessageSourceHelper messageSourceHelper;

    /**
     * Constructor that automatically maps exporters by their format.
     * Spring will inject all implementations of IReportExporter and all repositories.
     */
    public ReportService(
            List<IReportExporter> exporterList,
            TransactionalDocumentRepository transactionalDocumentRepository,
            SalaryPaymentRepository salaryPaymentRepository,
            ServicePaymentRepository servicePaymentRepository,
            LicencePlatePaymentRepository licencePlatePaymentRepository,
            FuelLoadRepository fuelLoadRepository,
            InsurancePolicyRepository insurancePolicyRepository,
            PolicyVehicleRepository policyVehicleRepository,
            RepairRepository repairRepository,
            ProjectAreaRepository projectAreaRepository,
            MessageSourceHelper messageSourceHelper) {

        this.exporters = exporterList.stream()
                .collect(Collectors.toMap(
                        IReportExporter::getFormat,
                        exporter -> exporter
                ));

        this.transactionalDocumentRepository = transactionalDocumentRepository;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.servicePaymentRepository = servicePaymentRepository;
        this.licencePlatePaymentRepository = licencePlatePaymentRepository;
        this.fuelLoadRepository = fuelLoadRepository;
        this.insurancePolicyRepository = insurancePolicyRepository;
        this.policyVehicleRepository = policyVehicleRepository;
        this.repairRepository = repairRepository;
        this.projectAreaRepository = projectAreaRepository;
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
                    .paymentMethod(doc.getPaid() ? "PAGADO" : "PENDIENTE")
                    .beneficiary(doc.getSupplier().getLegalName())
                    .reference(reference)
                    .comment(doc.getComment())
                    .projectAreaName(doc.getProjectArea() != null ? doc.getProjectArea().getName() : null)
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
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (SalaryPayment sp : salaryPayments) {
            String employeeName = sp.getEmployee().getLastName() + " " + sp.getEmployee().getName();

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
                null, // serviceSupplierId
                null, // buildingId
                getEffectiveAreaId(filters), // projectAreaId - from filter
                null, // serviceType
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
            String description = "Pago de servicio: " + sp.getServiceType().getDisplayName() +
                               " - " + sp.getBuilding().getName();

            items.add(ReportItemDTO.builder()
                    .id(sp.getId())
                    .date(sp.getPaymentDate())
                    .category(MoneyOutflowCategory.SERVICE)
                    .description(description)
                    .amount(sp.getAmount())
                    .paymentMethod(null)
                    .beneficiary(sp.getServiceSupplier().getSupplier().getLegalName())
                    .reference(sp.getReferenceNumber())
                    .comment(sp.getComment())
                    .projectAreaName(sp.getBuilding() != null && sp.getBuilding().getProjectArea() != null ? sp.getBuilding().getProjectArea().getName() : null)
                    .build());
        }

        log.debug("Collected {} service payment items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from LicencePlatePayment entities.
     */
    private List<ReportItemDTO> collectFromLicencePlatePayments(ReportFilterDTO filters) {
        log.debug("Collecting from licence plate payments");

        Pageable pageable = PageRequest.of(0, 10000);

        var licencePlatePayments = licencePlatePaymentRepository.findAllWithFilters(
                filters.startDate(),
                filters.endDate(),
                null, // vehicleId
                null, // vehicleLicensePlate
                getEffectiveAreaId(filters), // projectAreaId - from filter
                filters.minAmount(),
                filters.maxAmount(),
                null, // year
                null, // period
                null, // jurisdictionType
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (LicencePlatePayment lpp : licencePlatePayments) {
            String description = "Pago de patente " + lpp.getYear() + " - Período " + lpp.getPeriod();

            items.add(ReportItemDTO.builder()
                    .id(lpp.getId())
                    .date(lpp.getDate())
                    .category(MoneyOutflowCategory.LICENCE_PLATE)
                    .description(description)
                    .amount(lpp.getAmount())
                    .paymentMethod(null)
                    .beneficiary(lpp.getJurisdictionType().getDisplayName())
                    .reference("Vehículo ID: " + lpp.getVehicleId())
                    .comment(null)
                    .projectAreaName(lpp.getProjectAreaId() != null
                            ? projectAreaRepository.findById(lpp.getProjectAreaId())
                                    .map(a -> a.getName()).orElse(null)
                            : null)
                    .build());
        }

        log.debug("Collected {} licence plate payment items", items.size());
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

            items.add(ReportItemDTO.builder()
                    .id(fl.getId())
                    .date(fl.getDate())
                    .category(MoneyOutflowCategory.FUEL)
                    .description(description)
                    .amount(fl.getTotalAmount())
                    .paymentMethod(null)
                    .beneficiary(beneficiary)
                    .reference("Ticket: " + fl.getTicketNumber())
                    .comment("Vehículo: " + fl.getVehicle().getLicensePlate())
                    .projectAreaName(fl.getProjectArea() != null ? fl.getProjectArea().getName() : null)
                    .build());
        }

        log.debug("Collected {} fuel load items", items.size());
        return items;
    }

    /**
     * Collects money outflow items from InsurancePolicy entities.
     * For AUTOMOTOR policies: generates one report item per active insured vehicle
     * whose date range overlaps the report period, using each vehicle's premioMensual.
     * For other policy types: uses the policy-level premioMensual (or premioTotal / installments).
     */
    private List<ReportItemDTO> collectFromInsurances(ReportFilterDTO filters) {
        log.debug("Collecting from insurance policies");

        Pageable pageable = PageRequest.of(0, 10000);

        // Only retrieve active policies
        var insurancePolicies = insurancePolicyRepository.findAllWithFilters(
                null, // policyNumber
                null, // termNumber
                null, // policyType
                PolicyStatus.ACTIVO.name(), // only active policies
                null, // paymentFrequency
                null, // issueDateFrom
                null, // issueDateTo
                null, // effectiveFromStart
                null, // effectiveFromEnd
                null, // effectiveToStart
                null, // effectiveToEnd
                null, // isCancelled
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        // Determine report period boundaries
        LocalDate reportStart = filters.startDate();
        LocalDate reportEnd = filters.endDate();

        for (InsurancePolicy ip : insurancePolicies) {
            if (ip.getPolicyType() == PolicyType.AUTOMOTOR) {
                // For AUTOMOTOR: collect from each active insured vehicle
                items.addAll(collectFromAutomotorPolicy(ip, reportStart, reportEnd, filters));
            } else {
                // For other types: use policy-level premio
                BigDecimal paymentAmount = ip.getPremioMensual();
                if (paymentAmount == null && ip.getPremioTotal() != null
                        && ip.getNumberOfInstallments() != null && ip.getNumberOfInstallments() > 0) {
                    paymentAmount = ip.getPremioTotal()
                            .divide(BigDecimal.valueOf(ip.getNumberOfInstallments()), 2, java.math.RoundingMode.HALF_UP);
                }
                if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

                if (filters.minAmount() != null && paymentAmount.compareTo(filters.minAmount()) < 0) continue;
                if (filters.maxAmount() != null && paymentAmount.compareTo(filters.maxAmount()) > 0) continue;

                items.add(ReportItemDTO.builder()
                        .id(ip.getId())
                        .date(ip.getIssueDate())
                        .category(MoneyOutflowCategory.INSURANCE)
                        .description("Pago de seguro: " + ip.getPolicyType().getDisplayName() +
                                   " - Póliza N° " + ip.getPolicyNumber())
                        .amount(paymentAmount)
                        .paymentMethod(null)
                        .beneficiary("Aseguradora")
                        .reference(ip.getPolicyNumber())
                        .comment("Premio mensual")
                        .projectAreaName(null)
                        .build());
            }
        }

        log.debug("Collected {} insurance policy items", items.size());
        return items;
    }

    /**
     * Collects report items from an AUTOMOTOR policy's insured vehicles.
     * Only includes vehicles whose coverage period overlaps the report period
     * and whose installment months cover that period.
     */
    private List<ReportItemDTO> collectFromAutomotorPolicy(
            InsurancePolicy ip, LocalDate reportStart, LocalDate reportEnd, ReportFilterDTO filters) {

        List<ReportItemDTO> items = new ArrayList<>();

        if (ip.getAutoPolicy() == null) return items;

        List<PolicyVehicle> vehicles = policyVehicleRepository.findByAutoPolicyId(ip.getAutoPolicy().getId());

        for (PolicyVehicle pv : vehicles) {
            if (pv.getDeleted() || pv.getCancellationDate() != null) continue;
            if (pv.getPremioMensual() == null || pv.getPremioMensual().compareTo(BigDecimal.ZERO) <= 0) continue;

            // Check date range overlap with report period
            if (reportEnd != null && pv.getEffectiveFrom() != null && pv.getEffectiveFrom().isAfter(reportEnd)) continue;
            if (reportStart != null && pv.getEffectiveTo() != null && pv.getEffectiveTo().isBefore(reportStart)) continue;

            // Check installments cover the report period
            if (pv.getNumberOfInstallments() != null && pv.getNumberOfInstallments() > 0 && pv.getEffectiveFrom() != null) {
                LocalDate lastInstallmentMonth = pv.getEffectiveFrom().plusMonths(pv.getNumberOfInstallments() - 1);
                if (reportStart != null && lastInstallmentMonth.isBefore(reportStart)) continue;
            }

            BigDecimal amount = pv.getPremioMensual();

            if (filters.minAmount() != null && amount.compareTo(filters.minAmount()) < 0) continue;
            if (filters.maxAmount() != null && amount.compareTo(filters.maxAmount()) > 0) continue;

            String vehicleInfo = pv.getVehicle() != null ? pv.getVehicle().getLicensePlate() : "Vehículo ID: " + pv.getId();

            items.add(ReportItemDTO.builder()
                    .id(pv.getId())
                    .date(ip.getIssueDate())
                    .category(MoneyOutflowCategory.INSURANCE)
                    .description("Seguro automotor - Póliza N° " + ip.getPolicyNumber() + " - " + vehicleInfo)
                    .amount(amount)
                    .paymentMethod(null)
                    .beneficiary("Aseguradora")
                    .reference(ip.getPolicyNumber())
                    .comment("Premio mensual vehículo " + vehicleInfo)
                    .projectAreaName(pv.getVehicle() != null && pv.getVehicle().getProjectArea() != null
                            ? pv.getVehicle().getProjectArea().getName() : null)
                    .build());
        }

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
                null, // employee
                null, // supplierId
                null, // supplierName
                null, // repairType
                null, // search
                pageable
        ).getContent();

        List<ReportItemDTO> items = new ArrayList<>();

        for (Repair r : repairs) {
            if (r.getCost() == null) {
                continue; // Skip repairs without cost
            }

            String repairTypeNames = r.getRepairTypes().stream()
                    .map(RepairType::getDisplayName)
                    .collect(java.util.stream.Collectors.joining(", "));
            String description = "Reparación: " + repairTypeNames +
                               " - Vehículo: " + r.getVehicle().getLicensePlate();

            String beneficiary;
            if (r.getSupplier() != null) {
                beneficiary = r.getSupplier().getLegalName();
            } else if (r.getEmployee() != null) {
                beneficiary = r.getEmployee();
            } else {
                beneficiary = "No especificado";
            }

            items.add(ReportItemDTO.builder()
                    .id(r.getId())
                    .date(r.getDate())
                    .category(MoneyOutflowCategory.REPAIR)
                    .description(description)
                    .amount(r.getCost())
                    .paymentMethod(null)
                    .beneficiary(beneficiary)
                    .reference(null)
                    .comment(r.getDescription())
                    .projectAreaName(r.getVehicle().getProjectArea() != null ? r.getVehicle().getProjectArea().getName() : null)
                    .build());
        }

        log.debug("Collected {} repair items", items.size());
        return items;
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
}

