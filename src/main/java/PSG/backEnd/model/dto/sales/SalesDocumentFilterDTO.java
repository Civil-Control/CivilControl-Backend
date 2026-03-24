package PSG.backEnd.model.dto.sales;

import PSG.backEnd.model.enums.documents.SalesDocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesDocumentFilterDTO(
    Long clientId,
    SalesDocumentType documentType,
    String documentNumber,
    LocalDate dateFrom,
    LocalDate dateTo,
    BigDecimal minTotal,
    BigDecimal maxTotal,
    Boolean paid,
    Long projectAreaId,
    String search
) {}
