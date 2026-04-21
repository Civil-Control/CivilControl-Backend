package PSG.backEnd.controller;

import PSG.backEnd.model.constants.AppPermissions;
import PSG.backEnd.model.dto.reference.EmployeeReferenceItem;
import PSG.backEnd.model.dto.reference.GasStationReferenceItem;
import PSG.backEnd.model.dto.reference.ReferenceItem;
import PSG.backEnd.model.dto.reference.ServiceAssignmentReferenceItem;
import PSG.backEnd.model.dto.reference.VehicleReferenceItem;
import PSG.backEnd.model.enums.ItemType;
import PSG.backEnd.service.port.IReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Serves lightweight reference data (id + label) for form dropdowns/search modals.
 *
 * <h3>Why this exists</h3>
 * Creating an entity often requires selecting related entities (e.g. a Repair Order
 * requires picking a Vehicle). The full list endpoints for those related entities are
 * protected by domain-specific READ permissions. A user who can <em>create</em>
 * Repair Orders but does <b>not</b> have VEHICLE_READ must still be able to pick a
 * vehicle in the form.
 *
 * <h3>Permission model</h3>
 * Each endpoint below lists every permission whose holders need reference data for
 * that entity. If the caller holds <b>any one</b> of those permissions, access is
 * granted. This keeps the principle of least privilege while eliminating the
 * cross-permission dependency problem.
 *
 * <h3>Adding a new entity or dependency</h3>
 * When a new form depends on reference data from entity X, simply add the create/write
 * permission of the new form's entity to the {@code @PreAuthorize} expression of
 * the X reference endpoint. No frontend changes needed beyond calling the existing
 * {@code /api/v1/references/x} endpoint.
 */
@RestController
@RequestMapping("/api/v1/references")
@RequiredArgsConstructor
@Tag(name = "Reference Data",
     description = "Lightweight lookups (id + label) for form dropdowns. "
             + "Permissions are relaxed: any user who can create or edit an entity "
             + "that depends on the referenced entity is granted access.")
public class ReferenceDataController {

    private final IReferenceDataService referenceDataService;

    // ── Vehicles ──────────────────────────────────────────────────
    // Needed by: Repair, RepairOrder, FuelLoad, ServiceAssignment (vehicle-based),
    //            ServicePayment, InsurancePolicy, Vehicle (self-ref for types)
    @GetMapping("/vehicles")
    @Operation(summary = "Vehicle references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of vehicles (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.VEHICLE_READ + "',"
        + "'" + AppPermissions.REPAIR_WRITE + "',"
        + "'" + AppPermissions.REPAIR_ORDER_CREATE + "',"
        + "'" + AppPermissions.FUEL_LOAD_WRITE + "',"
        + "'" + AppPermissions.SERVICE_ASSIGNMENT_WRITE + "',"
        + "'" + AppPermissions.SERVICE_PAYMENT_WRITE + "',"
        + "'" + AppPermissions.INSURANCE_POLICY_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<VehicleReferenceItem>> getVehicleReferences() {
        return ResponseEntity.ok(referenceDataService.getVehicleReferences());
    }

    // ── Employees ─────────────────────────────────────────────────
    // Needed by: SalaryPayment, DisciplinaryAction, Vacation, EppDelivery
    @GetMapping("/employees")
    @Operation(summary = "Employee references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of employees (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.EMPLOYEE_READ + "',"
        + "'" + AppPermissions.SALARY_PAYMENT_WRITE + "',"
        + "'" + AppPermissions.DISCIPLINARY_ACTION_WRITE + "',"
        + "'" + AppPermissions.EMPLOYEE_VACATION_WRITE + "',"
        + "'" + AppPermissions.EPP_DELIVERY_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<EmployeeReferenceItem>> getEmployeeReferences() {
        return ResponseEntity.ok(referenceDataService.getEmployeeReferences());
    }

    // ── Suppliers ─────────────────────────────────────────────────
    // Needed by: TransactionalDocument, Payment, GasStation, RepairOrder (complete),
    //            ServiceSupplier
    @GetMapping("/suppliers")
    @Operation(summary = "Supplier references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of suppliers (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.SUPPLIER_READ + "',"
        + "'" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "',"
        + "'" + AppPermissions.PAYMENT_WRITE + "',"
        + "'" + AppPermissions.GAS_STATION_WRITE + "',"
        + "'" + AppPermissions.REPAIR_ORDER_WRITE + "',"
        + "'" + AppPermissions.SERVICE_SUPPLIER_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<PSG.backEnd.model.dto.reference.SupplierReferenceItem>> getSupplierReferences() {
        return ResponseEntity.ok(referenceDataService.getSupplierReferences());
    }

    // ── Buildings ─────────────────────────────────────────────────
    // Needed by: Stock, ServicePayment, ServiceAssignment, Vehicle
    @GetMapping("/buildings")
    @Operation(summary = "Building references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of buildings (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.BUILDING_READ + "',"
        + "'" + AppPermissions.STOCK_WRITE + "',"
        + "'" + AppPermissions.SERVICE_PAYMENT_WRITE + "',"
        + "'" + AppPermissions.SERVICE_ASSIGNMENT_WRITE + "',"
        + "'" + AppPermissions.VEHICLE_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getBuildingReferences() {
        return ResponseEntity.ok(referenceDataService.getBuildingReferences());
    }

    // ── Project Areas ─────────────────────────────────────────────
    // Needed by: Vehicle, FuelLoad, Employee, Building, TransactionalDocument,
    //            SalaryPayment, ServiceAssignment, ServicePayment
    @GetMapping("/project-areas")
    @Operation(summary = "Project area references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of project areas (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.PROJECT_AREA_READ + "',"
        + "'" + AppPermissions.VEHICLE_WRITE + "',"
        + "'" + AppPermissions.FUEL_LOAD_WRITE + "',"
        + "'" + AppPermissions.EMPLOYEE_WRITE + "',"
        + "'" + AppPermissions.BUILDING_WRITE + "',"
        + "'" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "',"
        + "'" + AppPermissions.SALARY_PAYMENT_WRITE + "',"
        + "'" + AppPermissions.SERVICE_ASSIGNMENT_WRITE + "',"
        + "'" + AppPermissions.SERVICE_PAYMENT_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getProjectAreaReferences() {
        return ResponseEntity.ok(referenceDataService.getProjectAreaReferences());
    }

    // ── Gas Stations ──────────────────────────────────────────────
    // Needed by: FuelLoad
    @GetMapping("/gas-stations")
    @Operation(summary = "Gas station references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of gas stations (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.GAS_STATION_READ + "',"
        + "'" + AppPermissions.FUEL_LOAD_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<GasStationReferenceItem>> getGasStationReferences() {
        return ResponseEntity.ok(referenceDataService.getGasStationReferences());
    }

    // ── Items ─────────────────────────────────────────────────────
    // Needed by: TransactionalDocument, SalesDocument
    @GetMapping("/items")
    @Operation(summary = "Item references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of items (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.ITEM_READ + "',"
        + "'" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "',"
        + "'" + AppPermissions.SALES_DOCUMENT_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getItemReferences() {
        return ResponseEntity.ok(referenceDataService.getItemReferences());
    }

    // Needed by: TransactionalDocument (COMPRA), SalesDocument (VENTA)
    @GetMapping("/items/type/{itemType}")
    @Operation(summary = "Item references filtered by type (COMPRA or VENTA)")
    @ApiResponse(responseCode = "200", description = "List of items of the given type (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.ITEM_READ + "',"
        + "'" + AppPermissions.TRANSACTIONAL_DOCUMENT_WRITE + "',"
        + "'" + AppPermissions.SALES_DOCUMENT_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getItemReferencesByType(@PathVariable ItemType itemType) {
        return ResponseEntity.ok(referenceDataService.getItemReferencesByType(itemType));
    }

    // ── Service Suppliers ─────────────────────────────────────────
    // Needed by: ServicePayment, ServiceAssignment
    @GetMapping("/service-suppliers")
    @Operation(summary = "Service supplier references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of service suppliers (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.SERVICE_SUPPLIER_READ + "',"
        + "'" + AppPermissions.SERVICE_PAYMENT_WRITE + "',"
        + "'" + AppPermissions.SERVICE_ASSIGNMENT_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getServiceSupplierReferences() {
        return ResponseEntity.ok(referenceDataService.getServiceSupplierReferences());
    }

    // ── Clients ───────────────────────────────────────────────────
    // Needed by: SalesDocument, WorkContract
    @GetMapping("/clients")
    @Operation(summary = "Client references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of clients (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.CLIENT_READ + "',"
        + "'" + AppPermissions.SALES_DOCUMENT_WRITE + "',"
        + "'" + AppPermissions.WORK_CONTRACT_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getClientReferences() {
        return ResponseEntity.ok(referenceDataService.getClientReferences());
    }

    // ── Work Contracts ────────────────────────────────────────────
    // Needed by: Certification
    @GetMapping("/work-contracts")
    @Operation(summary = "Work contract references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of work contracts (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.WORK_CONTRACT_READ + "',"
        + "'" + AppPermissions.CERTIFICATION_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getWorkContractReferences() {
        return ResponseEntity.ok(referenceDataService.getWorkContractReferences());
    }

    // ── Sales Documents ───────────────────────────────────────────
    // Needed by: Certification (optional salesDocumentId field)
    @GetMapping("/sales-documents")
    @Operation(summary = "Sales document references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of sales documents (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.SALES_DOCUMENT_READ + "',"
        + "'" + AppPermissions.CERTIFICATION_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ReferenceItem>> getSalesDocumentReferences() {
        return ResponseEntity.ok(referenceDataService.getSalesDocumentReferences());
    }

    // ── Service Assignments ───────────────────────────────────────
    // Needed by: ServicePayment
    @GetMapping("/service-assignments")
    @Operation(summary = "Service assignment references for form dropdowns")
    @ApiResponse(responseCode = "200", description = "List of service assignments (id + label)")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.SERVICE_ASSIGNMENT_READ + "',"
        + "'" + AppPermissions.SERVICE_PAYMENT_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<ServiceAssignmentReferenceItem>> getServiceAssignmentReferences() {
        return ResponseEntity.ok(referenceDataService.getServiceAssignmentReferences());
    }

    // ── Linked Supplier IDs ───────────────────────────────────────
    // Needed by: ServiceSupplier form (to filter already-linked suppliers)
    @GetMapping("/linked-supplier-ids")
    @Operation(summary = "IDs of suppliers already linked to a service supplier")
    @ApiResponse(responseCode = "200", description = "List of supplier IDs")
    @PreAuthorize(
        "hasAnyAuthority("
        + "'" + AppPermissions.SERVICE_SUPPLIER_READ + "',"
        + "'" + AppPermissions.SERVICE_SUPPLIER_WRITE + "'"
        + ")"
    )
    public ResponseEntity<List<Long>> getLinkedSupplierIds() {
        return ResponseEntity.ok(referenceDataService.getLinkedSupplierIds());
    }
}
