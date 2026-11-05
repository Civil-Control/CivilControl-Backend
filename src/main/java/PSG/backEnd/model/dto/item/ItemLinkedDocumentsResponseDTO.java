package PSG.backEnd.model.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Active purchase and sales documents that reference an item. Used to preview a cascade delete.")
public record ItemLinkedDocumentsResponseDTO(
    List<ItemLinkedDocumentDTO> purchaseDocuments,
    List<ItemLinkedDocumentDTO> salesDocuments
) {}
