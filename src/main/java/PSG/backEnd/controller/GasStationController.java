package PSG.backEnd.controller;

import PSG.backEnd.model.dto.gasStation.GasStationDTO;
import PSG.backEnd.model.dto.gasStation.GasStationFilterDTO;
import PSG.backEnd.model.dto.gasStation.GasStationResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IGasStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gas-stations")
@RequiredArgsConstructor
public class GasStationController {

    private final IGasStationService gasStationService;

    @PostMapping
    public ResponseEntity<GasStationResponseDTO> createGasStation(
            @Validated(OnCreate.class) @RequestBody GasStationDTO gasStationDTO) {
        GasStationResponseDTO createdGasStation = gasStationService.createGasStation(gasStationDTO);
        return new ResponseEntity<>(createdGasStation, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<GasStationResponseDTO>> getGasStations(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) List<String> fuelTypes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        GasStationFilterDTO filterDTO = new GasStationFilterDTO(supplierId, fuelTypes);

        return ResponseEntity.ok(gasStationService.getAllGasStations(filterDTO, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GasStationResponseDTO> getGasStationById(@PathVariable Long id) {
        return ResponseEntity.ok(gasStationService.getGasStationById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<GasStationResponseDTO> updateGasStation(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody GasStationDTO gasStationDTO) {
        return ResponseEntity.ok(gasStationService.updateGasStation(id, gasStationDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGasStation(@PathVariable Long id) {
        gasStationService.deleteGasStation(id);
        return ResponseEntity.noContent().build();
    }
}
