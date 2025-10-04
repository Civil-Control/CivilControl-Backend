package PSG.backEnd.controller;

import PSG.backEnd.model.dto.vehicle.VehicleDTO;
import PSG.backEnd.model.dto.vehicle.VehicleFilterDTO;
import PSG.backEnd.model.dto.vehicle.VehicleResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final IVehicleService iVehicleService;

    @PostMapping
    public ResponseEntity<VehicleResponseDTO> createVehicle(
            @Validated(OnCreate.class) @RequestBody VehicleDTO vehicleDTO) {
        VehicleResponseDTO createdVehicle = iVehicleService.createVehicle(vehicleDTO);
        return new ResponseEntity<>(createdVehicle, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<VehicleResponseDTO>> getVehicles(
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) String projectAreaName,
            @RequestParam(required = false) String storedIn,
            @RequestParam(required = false) LocalDate vtvExpirationDate,
            @RequestParam(required = false) String jurisdictionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        VehicleFilterDTO filterDTO = new VehicleFilterDTO(
                licensePlate, brand, model, year, color, nickName,
                vehicleType, projectAreaName, storedIn, vtvExpirationDate, jurisdictionType
        );

        return ResponseEntity.ok(iVehicleService.getAllVehicles(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(iVehicleService.getVehicleById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> updateVehicle(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody VehicleDTO vehicleDTO) {
        return ResponseEntity.ok(iVehicleService.updateVehicle(id, vehicleDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        iVehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}
