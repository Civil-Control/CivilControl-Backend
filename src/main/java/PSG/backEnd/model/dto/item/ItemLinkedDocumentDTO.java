package PSG.backEnd.model.dto.item;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "An active document (purchase or sales) that has at least one line using a given item.")
public record ItemLinkedDocumentDTO(
    Long documentId,
    String documentLabel,
    String counterpartName,
    LocalDate date,
    Integer lineCount,
    BigDecimal linesTotal,
    BigDecimal documentTotal,
    Boolean paid
) {}
