package PSG.backEnd.controller;

import PSG.backEnd.model.dto.employee.SalaryPaymentDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentFilterDTO;
import PSG.backEnd.model.dto.employee.SalaryPaymentResponseDTO;
import PSG.backEnd.model.enums.employee.SalaryFrecuency;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.ISalaryPaymentService;
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
@RequestMapping("/api/v1/salary-payments")
@RequiredArgsConstructor
public class SalaryPaymentController {

    private final ISalaryPaymentService iSalaryPaymentService;

    @PostMapping
    public ResponseEntity<SalaryPaymentResponseDTO> createSalaryPayment(
            @Validated(OnCreate.class) @RequestBody SalaryPaymentDTO salaryPaymentDTO) {
        SalaryPaymentResponseDTO createdSalaryPayment = iSalaryPaymentService.createSalaryPayment(salaryPaymentDTO);
        return new ResponseEntity<>(createdSalaryPayment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<SalaryPaymentResponseDTO>> getSalaryPayments(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) SalaryFrecuency salaryFrequency,
            @RequestParam(required = false) LocalDate paymentDateFrom,
            @RequestParam(required = false) LocalDate paymentDateTo,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        SalaryPaymentFilterDTO filterDTO = new SalaryPaymentFilterDTO(
                employeeId, salaryFrequency, paymentDateFrom, paymentDateTo,
                minAmount, maxAmount
        );

        return ResponseEntity.ok(iSalaryPaymentService.getAllSalaryPayments(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryPaymentResponseDTO> getSalaryPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(iSalaryPaymentService.getSalaryPaymentById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SalaryPaymentResponseDTO> updateSalaryPayment(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody SalaryPaymentDTO salaryPaymentDTO) {
        return ResponseEntity.ok(iSalaryPaymentService.updateSalaryPayment(id, salaryPaymentDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalaryPayment(@PathVariable Long id) {
        iSalaryPaymentService.deleteSalaryPayment(id);
        return ResponseEntity.noContent().build();
    }
}

