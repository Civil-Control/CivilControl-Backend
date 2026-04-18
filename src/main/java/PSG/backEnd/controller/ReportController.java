package PSG.backEnd.controller;

import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.MoneyOutflowReportPreviewDTO;
import PSG.backEnd.model.dto.report.ReportFilterDTO;
import PSG.backEnd.model.dto.report.salary.SalaryReportDTO;
import PSG.backEnd.model.dto.report.salary.SalaryReportFilterDTO;
import PSG.backEnd.model.dto.report.invoice.InvoiceReportDTO;
import PSG.backEnd.model.dto.report.invoice.InvoiceReportFilterDTO;
import PSG.backEnd.model.dto.report.servicePayment.ServicePaymentReportDTO;
import PSG.backEnd.model.dto.report.servicePayment.ServicePaymentReportFilterDTO;
import PSG.backEnd.model.dto.report.fuelLoad.FuelLoadReportDTO;
import PSG.backEnd.model.dto.report.fuelLoad.FuelLoadReportFilterDTO;
import PSG.backEnd.model.dto.report.repair.RepairReportDTO;
import PSG.backEnd.model.dto.report.repair.RepairReportFilterDTO;
import PSG.backEnd.model.dto.report.stockPurchase.StockPurchaseReportDTO;
import PSG.backEnd.model.dto.report.stockPurchase.StockPurchaseReportFilterDTO;
import PSG.backEnd.model.enums.MoneyOutflowCategory;
import PSG.backEnd.model.enums.ReportFormat;
import PSG.backEnd.model.enums.ServiceType;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.documents.DocumentType;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.enums.vehicle.FuelType;
import PSG.backEnd.service.port.IReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import PSG.backEnd.model.constants.AppPermissions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for money outflow reports.
 * Provides endpoints to generate and download reports in various formats.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "API for generating money outflow reports. " +
        "Allows filtering by date range, category, and amount. " +
        "Supports export to PDF and Excel formats with detailed information and summaries.")
public class ReportController {

    private final IReportService reportService;

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
    @GetMapping("/money-outflows")
    @Operation(
            summary = "Generate money outflow report data",
            description = "Generates a structured report with all money outflows based on the provided filters. " +
                    "Returns JSON data with items, totals, and summary by category. " +
                    "This endpoint is useful for API clients that need the data in JSON format."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MoneyOutflowReportDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters (invalid date range, invalid amount range, etc.)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during report generation"
            )
    })
    public ResponseEntity<MoneyOutflowReportDTO> generateMoneyOutflowReport(
            @Parameter(description = "Start date of the report period (inclusive). Format: yyyy-MM-dd", example = "2024-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date of the report period (inclusive). Format: yyyy-MM-dd", example = "2024-12-31")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "List of categories to filter by. Can specify multiple categories. If not provided, includes all categories.",
                    example = "[\"PAYMENT\", \"SALARY\"]")
            @RequestParam(required = false)
            List<MoneyOutflowCategory> categories,

            @Parameter(description = "List of sector IDs (project areas) to filter by. If provided, only includes items related to those sectors. Note: Insurance policies will be excluded when this filter is applied.",
                    example = "5")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Minimum amount to filter (inclusive)", example = "1000.00")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount to filter (inclusive)", example = "50000.00")
            @RequestParam(required = false)
            BigDecimal maxAmount,

            @Parameter(description = "Field to sort by", example = "date")
            @RequestParam(required = false, defaultValue = "date")
            String sortBy,

            @Parameter(description = "Sort order: asc (ascending) or desc (descending)", example = "desc")
            @RequestParam(required = false, defaultValue = "desc")
            String sortOrder
    ) {
        log.info("Generating money outflow report: startDate={}, endDate={}, categories={}, projectAreaIds={}",
                startDate, endDate, categories, projectAreaIds);

        ReportFilterDTO filters = new ReportFilterDTO(
                startDate,
                endDate,
                categories,
                projectAreaIds,
                minAmount,
                maxAmount,
                sortBy,
                sortOrder
        );

        MoneyOutflowReportDTO report = reportService.generateMoneyOutflowReport(filters);

        log.info("Report generated with {} items, total amount: ${}",
                report.totalCount(), report.totalAmount());

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
    @GetMapping("/money-outflows/download")
    @Operation(
            summary = "Download money outflow report file",
            description = "Generates and downloads a money outflow report in the specified format (PDF or EXCEL). " +
                    "The file includes a formatted title, metadata, detailed items table, summary by category, " +
                    "and totals. The filename is automatically generated based on the filter criteria."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report file generated and ready for download",
                    content = {
                            @Content(
                                    mediaType = "application/pdf",
                                    schema = @Schema(type = "string", format = "binary")
                            ),
                            @Content(
                                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    schema = @Schema(type = "string", format = "binary")
                            )
                    }
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters (invalid format, invalid date range, etc.)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during report generation or export"
            )
    })
    public ResponseEntity<byte[]> downloadMoneyOutflowReport(
            @Parameter(description = "Export format: PDF or EXCEL", example = "PDF", required = true)
            @RequestParam
            ReportFormat format,

            @Parameter(description = "Start date of the report period (inclusive). Format: yyyy-MM-dd", example = "2024-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date of the report period (inclusive). Format: yyyy-MM-dd", example = "2024-12-31")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "List of categories to filter by. Can specify multiple categories. If not provided, includes all categories.",
                    example = "[\"PAYMENT\", \"SALARY\"]")
            @RequestParam(required = false)
            List<MoneyOutflowCategory> categories,

            @Parameter(description = "List of sector IDs (project areas) to filter by. If provided, only includes items related to those sectors. Note: Insurance policies will be excluded when this filter is applied.",
                    example = "5")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Minimum amount to filter (inclusive)", example = "1000.00")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount to filter (inclusive)", example = "50000.00")
            @RequestParam(required = false)
            BigDecimal maxAmount,

            @Parameter(description = "Field to sort by", example = "date")
            @RequestParam(required = false, defaultValue = "date")
            String sortBy,

            @Parameter(description = "Sort order: asc (ascending) or desc (descending)", example = "desc")
            @RequestParam(required = false, defaultValue = "desc")
            String sortOrder
    ) {
        log.info("Downloading money outflow report: format={}, startDate={}, endDate={}, categories={}, projectAreaIds={}",
                format, startDate, endDate, categories, projectAreaIds);

        ReportFilterDTO filters = new ReportFilterDTO(
                startDate,
                endDate,
                categories,
                projectAreaIds,
                minAmount,
                maxAmount,
                sortBy,
                sortOrder
        );

        ResponseEntity<byte[]> response = reportService.generateMoneyOutflowReportFile(filters, format);

        log.info("Report file generated successfully in format: {}", format);

        return response;
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_VIEW + "')")
    @GetMapping("/money-outflows/preview")
    @Operation(
            summary = "Preview report statistics",
            description = "Returns quick statistics about the report that would be generated with the given filters. " +
                    "Useful for showing users a preview before generating the full report. " +
                    "Returns total count, total amount, average amount, and summary by category WITHOUT the detailed items list. " +
                    "This endpoint is optimized for performance and returns only aggregated data."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Preview statistics generated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MoneyOutflowReportPreviewDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters"
            )
    })
    public ResponseEntity<MoneyOutflowReportPreviewDTO> previewReport(
            @Parameter(description = "Start date of the report period (inclusive). Format: yyyy-MM-dd", example = "2024-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date of the report period (inclusive). Format: yyyy-MM-dd", example = "2024-12-31")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "List of categories to filter by. Can specify multiple categories. If not provided, includes all categories.",
                    example = "[\"PAYMENT\", \"SALARY\"]")
            @RequestParam(required = false)
            List<MoneyOutflowCategory> categories,

            @Parameter(description = "List of sector IDs (project areas) to filter by. If provided, only includes items related to those sectors. Note: Insurance policies will be excluded when this filter is applied.",
                    example = "5")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Minimum amount to filter (inclusive)", example = "1000.00")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount to filter (inclusive)", example = "50000.00")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating preview: startDate={}, endDate={}, categories={}, projectAreaIds={}",
                startDate, endDate, categories, projectAreaIds);

        ReportFilterDTO filters = new ReportFilterDTO(
                startDate,
                endDate,
                categories,
                projectAreaIds,
                minAmount,
                maxAmount,
                "date",
                "desc"
        );

        MoneyOutflowReportPreviewDTO preview = reportService.generateMoneyOutflowReportPreview(filters);

        log.info("Preview generated: {} items, total: ${}, average: ${}",
                 preview.totalCount(), preview.totalAmount(), preview.averageAmount());

        return ResponseEntity.ok(preview);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SALARY REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
    @GetMapping("/salary")
    @Operation(
            summary = "Generate salary report data",
            description = "Generates a hierarchical salary report grouped by project area and employee. " +
                    "Includes subtotals per frequency (mensual, quincenal, semanal) at each level."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<SalaryReportDTO> generateSalaryReport(
            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by salary frequency")
            @RequestParam(required = false)
            SalaryFrecuency salaryFrequency,

            @Parameter(description = "Filter by payment method")
            @RequestParam(required = false)
            PaymentMethod paymentMethod,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating salary report: startDate={}, endDate={}, areas={}", startDate, endDate, projectAreaIds);

        SalaryReportFilterDTO filters = new SalaryReportFilterDTO(
                startDate, endDate, projectAreaIds, salaryFrequency, paymentMethod, minAmount, maxAmount
        );

        SalaryReportDTO report = reportService.generateSalaryReport(filters);

        log.info("Salary report generated: {} payments, total: ${}", report.totalCount(), report.totalAmount());

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
    @GetMapping("/salary/download")
    @Operation(
            summary = "Download salary report file",
            description = "Generates and downloads a salary report in the specified format (PDF or EXCEL)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report file generated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<byte[]> downloadSalaryReport(
            @Parameter(description = "Export format: PDF or EXCEL", required = true)
            @RequestParam
            ReportFormat format,

            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by salary frequency")
            @RequestParam(required = false)
            SalaryFrecuency salaryFrequency,

            @Parameter(description = "Filter by payment method")
            @RequestParam(required = false)
            PaymentMethod paymentMethod,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Downloading salary report: format={}", format);

        SalaryReportFilterDTO filters = new SalaryReportFilterDTO(
                startDate, endDate, projectAreaIds, salaryFrequency, paymentMethod, minAmount, maxAmount
        );

        return reportService.generateSalaryReportFile(filters, format);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INVOICE REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
    @GetMapping("/invoices")
    @Operation(
            summary = "Generate invoice report data",
            description = "Generates a hierarchical invoice report grouped by project area and supplier. " +
                    "Includes subtotals per document type at each level."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<InvoiceReportDTO> generateInvoiceReport(
            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by document type")
            @RequestParam(required = false)
            DocumentType documentType,

            @Parameter(description = "Filter by paid status")
            @RequestParam(required = false)
            Boolean paid,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating invoice report: startDate={}, endDate={}, areas={}", startDate, endDate, projectAreaIds);

        InvoiceReportFilterDTO filters = new InvoiceReportFilterDTO(
                startDate, endDate, projectAreaIds, documentType, paid, minAmount, maxAmount
        );

        InvoiceReportDTO report = reportService.generateInvoiceReport(filters);

        log.info("Invoice report generated: {} documents, total: ${}", report.totalCount(), report.totalAmount());

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
    @GetMapping("/invoices/download")
    @Operation(
            summary = "Download invoice report file",
            description = "Generates and downloads an invoice report in the specified format (PDF or EXCEL)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report file generated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<byte[]> downloadInvoiceReport(
            @Parameter(description = "Export format: PDF or EXCEL", required = true)
            @RequestParam
            ReportFormat format,

            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by document type")
            @RequestParam(required = false)
            DocumentType documentType,

            @Parameter(description = "Filter by paid status")
            @RequestParam(required = false)
            Boolean paid,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Downloading invoice report: format={}", format);

        InvoiceReportFilterDTO filters = new InvoiceReportFilterDTO(
                startDate, endDate, projectAreaIds, documentType, paid, minAmount, maxAmount
        );

        return reportService.generateInvoiceReportFile(filters, format);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SERVICE PAYMENT REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
    @GetMapping("/service-payments")
    @Operation(
            summary = "Generate service payment report data",
            description = "Generates a hierarchical service payment report grouped by project area and building. " +
                    "Includes subtotals per service type at each level."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<ServicePaymentReportDTO> generateServicePaymentReport(
            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by service type")
            @RequestParam(required = false)
            ServiceType serviceType,

            @Parameter(description = "Filter by subject type (BUILDING or VEHICLE)")
            @RequestParam(required = false)
            SubjectType subjectType,

            @Parameter(description = "Filter by payment method")
            @RequestParam(required = false)
            PaymentMethod paymentMethod,

            @Parameter(description = "Filter by year")
            @RequestParam(required = false)
            Integer year,

            @Parameter(description = "Filter by period/month (1-12)")
            @RequestParam(required = false)
            Integer period,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating service payment report: startDate={}, endDate={}, areas={}", startDate, endDate, projectAreaIds);

        ServicePaymentReportFilterDTO filters = new ServicePaymentReportFilterDTO(
                startDate, endDate, projectAreaIds, serviceType, subjectType, paymentMethod, year, period, minAmount, maxAmount
        );

        ServicePaymentReportDTO report = reportService.generateServicePaymentReport(filters);

        log.info("Service payment report generated: {} payments, total: ${}", report.totalCount(), report.totalAmount());

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
    @GetMapping("/service-payments/download")
    @Operation(
            summary = "Download service payment report file",
            description = "Generates and downloads a service payment report in the specified format (PDF or EXCEL)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report file generated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<byte[]> downloadServicePaymentReport(
            @Parameter(description = "Export format: PDF or EXCEL", required = true)
            @RequestParam
            ReportFormat format,

            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by service type")
            @RequestParam(required = false)
            ServiceType serviceType,

            @Parameter(description = "Filter by subject type (BUILDING or VEHICLE)")
            @RequestParam(required = false)
            SubjectType subjectType,

            @Parameter(description = "Filter by payment method")
            @RequestParam(required = false)
            PaymentMethod paymentMethod,

            @Parameter(description = "Filter by year")
            @RequestParam(required = false)
            Integer year,

            @Parameter(description = "Filter by period/month (1-12)")
            @RequestParam(required = false)
            Integer period,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Downloading service payment report: format={}", format);

        ServicePaymentReportFilterDTO filters = new ServicePaymentReportFilterDTO(
                startDate, endDate, projectAreaIds, serviceType, subjectType, paymentMethod, year, period, minAmount, maxAmount
        );

        return reportService.generateServicePaymentReportFile(filters, format);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FUEL LOAD REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
    @GetMapping("/fuel-loads")
    @Operation(
            summary = "Generate fuel load report data",
            description = "Generates a hierarchical fuel load report grouped by gas station, project area and vehicle. " +
                    "Includes subtotals per fuel type at each level."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<FuelLoadReportDTO> generateFuelLoadReport(
            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by fuel type")
            @RequestParam(required = false)
            FuelType fuelType,

            @Parameter(description = "Filter by gas station ID")
            @RequestParam(required = false)
            Long gasStationId,

            @Parameter(description = "Filter by vehicle ID")
            @RequestParam(required = false)
            Long vehicleId,

            @Parameter(description = "Filter by vehicle type ID")
            @RequestParam(required = false)
            Long vehicleTypeId,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating fuel load report: startDate={}, endDate={}, areas={}", startDate, endDate, projectAreaIds);

        FuelLoadReportFilterDTO filters = new FuelLoadReportFilterDTO(
                startDate, endDate, projectAreaIds, fuelType, gasStationId, vehicleId, vehicleTypeId, minAmount, maxAmount
        );

        FuelLoadReportDTO report = reportService.generateFuelLoadReport(filters);

        log.info("Fuel load report generated: {} loads, total: ${}", report.totalCount(), report.totalAmount());

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
    @GetMapping("/fuel-loads/download")
    @Operation(
            summary = "Download fuel load report file",
            description = "Generates and downloads a fuel load report in the specified format (PDF or EXCEL)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report file generated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<byte[]> downloadFuelLoadReport(
            @Parameter(description = "Export format: PDF or EXCEL", required = true)
            @RequestParam
            ReportFormat format,

            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by fuel type")
            @RequestParam(required = false)
            FuelType fuelType,

            @Parameter(description = "Filter by gas station ID")
            @RequestParam(required = false)
            Long gasStationId,

            @Parameter(description = "Filter by vehicle ID")
            @RequestParam(required = false)
            Long vehicleId,

            @Parameter(description = "Filter by vehicle type ID")
            @RequestParam(required = false)
            Long vehicleTypeId,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Downloading fuel load report: format={}", format);

        FuelLoadReportFilterDTO filters = new FuelLoadReportFilterDTO(
                startDate, endDate, projectAreaIds, fuelType, gasStationId, vehicleId, vehicleTypeId, minAmount, maxAmount
        );

        return reportService.generateFuelLoadReportFile(filters, format);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REPAIR REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
    @GetMapping("/repairs")
    @Operation(
            summary = "Generate repair report data",
            description = "Generates a hierarchical repair report grouped by project area and vehicle. " +
                    "Includes material and labor cost subtotals at each level."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<RepairReportDTO> generateRepairReport(
            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by vehicle ID")
            @RequestParam(required = false)
            Long vehicleId,

            @Parameter(description = "Filter by supplier ID (external repairs)")
            @RequestParam(required = false)
            Long supplierId,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating repair report: startDate={}, endDate={}, areas={}", startDate, endDate, projectAreaIds);

        RepairReportFilterDTO filters = new RepairReportFilterDTO(
                startDate, endDate, projectAreaIds, vehicleId, supplierId, minAmount, maxAmount
        );

        RepairReportDTO report = reportService.generateRepairReport(filters);

        log.info("Repair report generated: {} repairs, total: ${}", report.totalCount(), report.totalAmount());

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
    @GetMapping("/repairs/download")
    @Operation(
            summary = "Download repair report file",
            description = "Generates and downloads a repair report in the specified format (PDF or EXCEL)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report file generated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<byte[]> downloadRepairReport(
            @Parameter(description = "Export format: PDF or EXCEL", required = true)
            @RequestParam
            ReportFormat format,

            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Project area IDs to include")
            @RequestParam(required = false)
            List<Long> projectAreaIds,

            @Parameter(description = "Filter by vehicle ID")
            @RequestParam(required = false)
            Long vehicleId,

            @Parameter(description = "Filter by supplier ID (external repairs)")
            @RequestParam(required = false)
            Long supplierId,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Downloading repair report: format={}", format);

        RepairReportFilterDTO filters = new RepairReportFilterDTO(
                startDate, endDate, projectAreaIds, vehicleId, supplierId, minAmount, maxAmount
        );

        return reportService.generateRepairReportFile(filters, format);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STOCK PURCHASE REPORT
    // ═══════════════════════════════════════════════════════════════════════

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_FINANCIAL + "')")
    @GetMapping("/stock-purchases")
    @Operation(
            summary = "Generate stock purchase report data",
            description = "Generates a hierarchical stock purchase report grouped by category and stock item. " +
                    "Includes quantity and amount subtotals at each level."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<StockPurchaseReportDTO> generateStockPurchaseReport(
            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Stock category enum names to include")
            @RequestParam(required = false)
            List<String> stockCategories,

            @Parameter(description = "Filter by stock item ID")
            @RequestParam(required = false)
            Long stockId,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating stock purchase report: startDate={}, endDate={}, categories={}", startDate, endDate, stockCategories);

        StockPurchaseReportFilterDTO filters = new StockPurchaseReportFilterDTO(
                startDate, endDate, stockCategories, stockId, minAmount, maxAmount
        );

        StockPurchaseReportDTO report = reportService.generateStockPurchaseReport(filters);

        log.info("Stock purchase report generated: {} purchases, total: ${}", report.totalCount(), report.totalAmount());

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.REPORT_EXPORT + "')")
    @GetMapping("/stock-purchases/download")
    @Operation(
            summary = "Download stock purchase report file",
            description = "Generates and downloads a stock purchase report in the specified format (PDF or EXCEL)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report file generated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<byte[]> downloadStockPurchaseReport(
            @Parameter(description = "Export format: PDF or EXCEL", required = true)
            @RequestParam
            ReportFormat format,

            @Parameter(description = "Start date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @Parameter(description = "End date (inclusive). Format: yyyy-MM-dd")
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,

            @Parameter(description = "Stock category enum names to include")
            @RequestParam(required = false)
            List<String> stockCategories,

            @Parameter(description = "Filter by stock item ID")
            @RequestParam(required = false)
            Long stockId,

            @Parameter(description = "Minimum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount (inclusive)")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Downloading stock purchase report: format={}", format);

        StockPurchaseReportFilterDTO filters = new StockPurchaseReportFilterDTO(
                startDate, endDate, stockCategories, stockId, minAmount, maxAmount
        );

        return reportService.generateStockPurchaseReportFile(filters, format);
    }
}
