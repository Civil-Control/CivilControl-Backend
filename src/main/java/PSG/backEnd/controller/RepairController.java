package PSG.backEnd.controller;

import PSG.backEnd.model.dto.vehicle.RepairDTO;
import PSG.backEnd.model.dto.vehicle.RepairFilterDTO;
import PSG.backEnd.model.dto.vehicle.RepairResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/repairs")
@RequiredArgsConstructor
public class RepairController {

    private final IRepairService repairService;

    @PostMapping
    public ResponseEntity<RepairResponseDTO> createRepair(
            @Validated(OnCreate.class) @RequestBody RepairDTO repairDTO) {
        RepairResponseDTO createdRepair = repairService.createRepair(repairDTO);
        return new ResponseEntity<>(createdRepair, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<RepairResponseDTO>> getRepairs(
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String vehicleLicensePlate,
            @RequestParam(required = false) BigDecimal minCost,
            @RequestParam(required = false) BigDecimal maxCost,
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String supplierLegalName,
            @RequestParam(required = false) String repairType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        RepairFilterDTO filterDTO = new RepairFilterDTO(
                dateFrom, dateTo, vehicleId, vehicleLicensePlate,
                minCost, maxCost, employee, supplierId, supplierLegalName, repairType
        );

        return ResponseEntity.ok(repairService.getAllRepairs(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepairResponseDTO> getRepairById(@PathVariable Long id) {
        return ResponseEntity.ok(repairService.getRepairById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RepairResponseDTO> updateRepair(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody RepairDTO repairDTO) {
        return ResponseEntity.ok(repairService.updateRepair(id, repairDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepair(@PathVariable Long id) {
        repairService.deleteRepair(id);
        return ResponseEntity.noContent().build();
    }
}
