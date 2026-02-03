package PSG.backEnd.controller;

import PSG.backEnd.model.dto.report.MoneyOutflowReportDTO;
import PSG.backEnd.model.dto.report.MoneyOutflowReportPreviewDTO;
import PSG.backEnd.model.dto.report.ReportFilterDTO;
import PSG.backEnd.model.enums.MoneyOutflowCategory;
import PSG.backEnd.model.enums.ReportFormat;
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
import org.springframework.web.bind.annotation.*;

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

            @Parameter(description = "Filter by sector ID (project area). If provided, only includes items related to this sector. Note: Insurance policies will be excluded when this filter is applied.",
                    example = "5")
            @RequestParam(required = false)
            Long projectAreaId,

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
        log.info("Generating money outflow report: startDate={}, endDate={}, categories={}, projectAreaId={}",
                startDate, endDate, categories, projectAreaId);

        ReportFilterDTO filters = new ReportFilterDTO(
                startDate,
                endDate,
                categories,
                projectAreaId,
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

            @Parameter(description = "Filter by sector ID (project area). If provided, only includes items related to this sector. Note: Insurance policies will be excluded when this filter is applied.",
                    example = "5")
            @RequestParam(required = false)
            Long projectAreaId,

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
        log.info("Downloading money outflow report: format={}, startDate={}, endDate={}, categories={}, projectAreaId={}",
                format, startDate, endDate, categories, projectAreaId);

        ReportFilterDTO filters = new ReportFilterDTO(
                startDate,
                endDate,
                categories,
                projectAreaId,
                minAmount,
                maxAmount,
                sortBy,
                sortOrder
        );

        ResponseEntity<byte[]> response = reportService.generateMoneyOutflowReportFile(filters, format);

        log.info("Report file generated successfully in format: {}", format);

        return response;
    }

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

            @Parameter(description = "Filter by sector ID (project area). If provided, only includes items related to this sector. Note: Insurance policies will be excluded when this filter is applied.",
                    example = "5")
            @RequestParam(required = false)
            Long projectAreaId,

            @Parameter(description = "Minimum amount to filter (inclusive)", example = "1000.00")
            @RequestParam(required = false)
            BigDecimal minAmount,

            @Parameter(description = "Maximum amount to filter (inclusive)", example = "50000.00")
            @RequestParam(required = false)
            BigDecimal maxAmount
    ) {
        log.info("Generating preview: startDate={}, endDate={}, categories={}, projectAreaId={}",
                startDate, endDate, categories, projectAreaId);

        ReportFilterDTO filters = new ReportFilterDTO(
                startDate,
                endDate,
                categories,
                projectAreaId,
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
}

