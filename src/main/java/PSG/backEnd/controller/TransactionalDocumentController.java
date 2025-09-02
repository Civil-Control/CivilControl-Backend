package PSG.backEnd.controller;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentFilterDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups;
import PSG.backEnd.service.port.ITransactionalDocumentService;
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
@RequestMapping("/api/v1/transactional-documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionalDocumentController {

    private final ITransactionalDocumentService iTransactionalDocumentService;

    @PostMapping
    public ResponseEntity<TransactionalDocumentResponseDTO> createTransactionalDocument(
            @Validated(ValidationGroups.OnCreate.class) @RequestBody TransactionalDocumentDTO transactionalDocumentDTO) {
        TransactionalDocumentResponseDTO response = iTransactionalDocumentService.createTransactionalDocument(transactionalDocumentDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionalDocumentResponseDTO>> getTransactionalDocuments(
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) String supplierCuit,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) Long projectAreaId,
            @RequestParam(required = false) String projectAreaName,
            @RequestParam(required = false) BigDecimal minTotalAmount,
            @RequestParam(required = false) BigDecimal maxTotalAmount,
            @RequestParam(required = false) BigDecimal totalAmount,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        TransactionalDocumentFilterDTO filter = new TransactionalDocumentFilterDTO(
                documentNumber, supplierCuit, supplierName, projectAreaId, projectAreaName, minTotalAmount,
                maxTotalAmount, totalAmount, fromDate, toDate, paid
        );
        return ResponseEntity.ok(iTransactionalDocumentService.getAllTransactionalDocuments(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionalDocumentResponseDTO> getTransactionalDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(iTransactionalDocumentService.getTransactionalDocumentById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TransactionalDocumentResponseDTO> updateTransactionalDocument(
            @PathVariable Long id,
            @Validated(ValidationGroups.OnUpdate.class) @RequestBody TransactionalDocumentDTO transactionalDocumentDTO) {
        return ResponseEntity.ok(iTransactionalDocumentService.updateTransactionalDocument(id, transactionalDocumentDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransactionalDocument(@PathVariable Long id) {
        iTransactionalDocumentService.deleteTransactionalDocument(id);
        return ResponseEntity.noContent().build();
    }
}