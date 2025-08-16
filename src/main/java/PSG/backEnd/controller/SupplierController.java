package PSG.backEnd.controller;

import PSG.backEnd.model.dto.SupplierDTO;
import PSG.backEnd.model.dto.SupplierFilterDTO;
import PSG.backEnd.model.dto.SupplierResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ISupplierService;
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

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierController {

    private final ISupplierService supplierService;

    @PostMapping
    public ResponseEntity<SupplierResponseDTO> createSupplier(
            @Validated(OnCreate.class) @RequestBody SupplierDTO supplierDTO) {
        SupplierResponseDTO createdSupplier = supplierService.createSupplier(supplierDTO);
        return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<SupplierResponseDTO>> getSuppliers(
            @RequestParam(required = false) String cuit,
            @RequestParam(required = false) String legalName,
            @RequestParam(required = false) String tradeName,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minDiscountPercentage,
            @RequestParam(required = false) BigDecimal maxDiscountPercentage,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        SupplierFilterDTO filterDTO = new SupplierFilterDTO(
                cuit, legalName, tradeName, city,
                minDiscountPercentage, maxDiscountPercentage, active
        );

        return ResponseEntity.ok(supplierService.getAllSuppliers(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody SupplierDTO supplierDTO) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
