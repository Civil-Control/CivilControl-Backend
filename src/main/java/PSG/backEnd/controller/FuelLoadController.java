package PSG.backEnd.controller;

import PSG.backEnd.model.dto.gasStation.FuelLoadDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadFilterDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadResponseDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchDTO;
import PSG.backEnd.model.dto.gasStation.FuelLoadBatchResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IFuelLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/fuel-loads")
@RequiredArgsConstructor
public class FuelLoadController {

    private final IFuelLoadService fuelLoadService;

    @PostMapping
    public ResponseEntity<FuelLoadResponseDTO> createFuelLoad(
            @Validated(OnCreate.class) @RequestBody FuelLoadDTO fuelLoadDTO) {
        FuelLoadResponseDTO createdFuelLoad = fuelLoadService.createFuelLoad(fuelLoadDTO);
        return new ResponseEntity<>(createdFuelLoad, HttpStatus.CREATED);
    }

    @PostMapping("/batch")
    public ResponseEntity<FuelLoadBatchResponseDTO> createFuelLoadBatch(
            @Validated @RequestBody FuelLoadBatchDTO fuelLoadBatchDTO) {
        FuelLoadBatchResponseDTO result = fuelLoadService.createFuelLoadBatch(fuelLoadBatchDTO);

        HttpStatus status = result.totalSuccessful() > 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;

        return new ResponseEntity<>(result, status);
    }

    @GetMapping
    public ResponseEntity<Page<FuelLoadResponseDTO>> getFuelLoads(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String ticketNumber,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String vehicleLicensePlate,
            @RequestParam(required = false) Long projectAreaId,
            @RequestParam(required = false) String projectAreaName,
            @RequestParam(required = false) Long gasStationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        FuelLoadFilterDTO filterDTO = new FuelLoadFilterDTO(
                dateFrom, dateTo, branchCode, ticketNumber, fuelType,
                vehicleId, vehicleLicensePlate, projectAreaId, projectAreaName, gasStationId
        );

        return ResponseEntity.ok(fuelLoadService.getAllFuelLoads(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuelLoadResponseDTO> getFuelLoadById(@PathVariable Long id) {
        return ResponseEntity.ok(fuelLoadService.getFuelLoadById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FuelLoadResponseDTO> updateFuelLoad(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody FuelLoadDTO fuelLoadDTO) {
        return ResponseEntity.ok(fuelLoadService.updateFuelLoad(id, fuelLoadDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFuelLoad(@PathVariable Long id) {
        fuelLoadService.deleteFuelLoad(id);
        return ResponseEntity.noContent().build();
    }
}
