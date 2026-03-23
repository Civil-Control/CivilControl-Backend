package PSG.backEnd.model.dto.transactionalDocument;

import java.util.List;

public record LinkedRecordsSummaryDTO(
        List<LinkedRecordItemDTO> repairs,
        List<LinkedRecordItemDTO> fuelLoads,
        List<LinkedRecordItemDTO> salaryPayments,
        List<LinkedRecordItemDTO> stocks
) {
    public boolean hasAny() {
        return !repairs.isEmpty() || !fuelLoads.isEmpty()
                || !salaryPayments.isEmpty() || !stocks.isEmpty();
    }
}
