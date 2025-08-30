package PSG.backEnd.controller;

import PSG.backEnd.model.dto.item.ItemDTO;
import PSG.backEnd.model.dto.item.ItemResponseDTO;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.service.port.IItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@Validated
public class ItemController {

    private final IItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponseDTO> create(@Valid @Validated(OnCreate.class) @RequestBody ItemDTO dto) {
        ItemResponseDTO created = itemService.create(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> update(
            @PathVariable Long id,
            @Valid @Validated(OnUpdate.class) @RequestBody ItemDTO dto) {
        ItemResponseDTO updated = itemService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDTO> getById(@PathVariable Long id) {
        ItemResponseDTO item = itemService.getById(id);
        return ResponseEntity.ok(item);
    }

    @GetMapping
    public ResponseEntity<Page<ItemResponseDTO>> list(
            @RequestParam(value = "q", required = false) String nameFilter,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ItemResponseDTO> items = itemService.list(nameFilter, pageable);
        return ResponseEntity.ok(items);
    }
}
