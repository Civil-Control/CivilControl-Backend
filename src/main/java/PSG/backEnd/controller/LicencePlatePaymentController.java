package PSG.backEnd.controller;

import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentFilterDTO;
import PSG.backEnd.model.dto.vehicle.LicencePlatePaymentResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ILicencePlatePaymentService;
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
@RequestMapping("/api/v1/licence-plate-payments")
@RequiredArgsConstructor
public class LicencePlatePaymentController {

    private final ILicencePlatePaymentService licencePlatePaymentService;

    @PostMapping
    public ResponseEntity<LicencePlatePaymentResponseDTO> createLicencePlatePayment(
            @Validated(OnCreate.class) @RequestBody LicencePlatePaymentDTO licencePlatePaymentDTO) {
        LicencePlatePaymentResponseDTO createdPayment = licencePlatePaymentService.createLicencePlatePayment(licencePlatePaymentDTO);
        return new ResponseEntity<>(createdPayment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<LicencePlatePaymentResponseDTO>> getLicencePlatePayments(
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String vehicleLicensePlate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) String jurisdictionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        LicencePlatePaymentFilterDTO filterDTO = new LicencePlatePaymentFilterDTO(
                dateFrom, dateTo, vehicleId, vehicleLicensePlate,
                minAmount, maxAmount, year, period, jurisdictionType
        );

        return ResponseEntity.ok(licencePlatePaymentService.getAllLicencePlatePayments(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LicencePlatePaymentResponseDTO> getLicencePlatePaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(licencePlatePaymentService.getLicencePlatePaymentById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LicencePlatePaymentResponseDTO> updateLicencePlatePayment(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody LicencePlatePaymentDTO licencePlatePaymentDTO) {
        return ResponseEntity.ok(licencePlatePaymentService.updateLicencePlatePayment(id, licencePlatePaymentDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLicencePlatePayment(@PathVariable Long id) {
        licencePlatePaymentService.deleteLicencePlatePayment(id);
        return ResponseEntity.noContent().build();
    }
}

