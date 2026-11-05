package PSG.backEnd.model.dto.reference;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight DTO for form dropdowns and search modals.
 * Contains only the minimal data needed: an ID plus a human-readable label.
 *
 * <p>Served by {@code /api/v1/references/{entity}} with relaxed permissions,
 * so users who can <em>create</em> an entity can also look up its related entities
 * without needing full READ permissions on those entities.</p>
 */
@Schema(description = "Minimal reference item for form selections (dropdowns, search modals)")
public record ReferenceItem(
        @Schema(description = "Unique identifier", example = "42")
        Long id,
        @Schema(description = "Human-readable label for display", example = "ABC-123 · Toyota Corolla")
        String label
) {}
