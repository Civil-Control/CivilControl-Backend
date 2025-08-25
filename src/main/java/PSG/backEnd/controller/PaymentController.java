package PSG.backEnd.controller;

import PSG.backEnd.model.dto.payment.*;
import PSG.backEnd.model.enums.PaymentMethod;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/cash")
    public CashPaymentResponseDTO createCash(
            @Validated(OnCreate.class) @RequestBody CashPaymentDTO dto) {
        return paymentService.createCash(dto);
    }

    @PostMapping("/transfer")
    public TransferPaymentResponseDTO createTransfer(
            @Validated(OnCreate.class) @RequestBody TransferPaymentDTO dto) {
        return paymentService.createTransfer(dto);
    }

    @PostMapping("/check")
    public CheckPaymentResponseDTO createCheck(
            @Validated(OnCreate.class) @RequestBody CheckPaymentDTO dto) {
        return paymentService.createCheck(dto);
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponseDTO>> getAll(
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String transactionNumber,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        PaymentFilterDTO filter = new PaymentFilterDTO(
                paymentMethod, startDate, endDate, minAmount, maxAmount, transactionNumber, supplierId
        );
        return ResponseEntity.ok(paymentService.findAll(filter, pageable));
    }

    @GetMapping("/cash/{id}")
    public CashPaymentResponseDTO getCash(@PathVariable Long id) {
        return paymentService.getCash(id);
    }

    @GetMapping("/transfer/{id}")
    public TransferPaymentResponseDTO getTransfer(@PathVariable Long id) {
        return paymentService.getTransfer(id);
    }

    @GetMapping("/check/{id}")
    public CheckPaymentResponseDTO getCheck(@PathVariable Long id) {
        return paymentService.getCheck(id);
    }

    @PatchMapping("/cash/{id}")
    public CashPaymentResponseDTO updateCash(
            @Validated(OnUpdate.class) @PathVariable Long id,
            @RequestBody CashPaymentDTO dto) {
        return paymentService.updateCash(id, dto);
    }

    @PatchMapping("/transfer/{id}")
    public TransferPaymentResponseDTO updateTransfer(
            @Validated(OnUpdate.class) @PathVariable Long id,
            @RequestBody TransferPaymentDTO dto) {
        return paymentService.updateTransfer(id, dto);
    }

    @PatchMapping("/check/{id}")
    public CheckPaymentResponseDTO updateCheck(
            @Validated(OnUpdate.class) @PathVariable Long id,
            @RequestBody CheckPaymentDTO dto) {
        return paymentService.updateCheck(id, dto);
    }

    @DeleteMapping("/cash/{id}")
    public void deleteCash(@PathVariable Long id) {
        paymentService.deleteCash(id);
    }

    @DeleteMapping("/transfer/{id}")
    public void deleteTransfer(@PathVariable Long id) {
        paymentService.deleteTransfer(id);
    }

    @DeleteMapping("/check/{id}")
    public void deleteCheck(@PathVariable Long id) {
        paymentService.deleteCheck(id);
    }
}