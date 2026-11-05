package PSG.backEnd.service.implementation;

import PSG.backEnd.model.dto.batch.BatchItemErrorDTO;
import PSG.backEnd.model.dto.batch.BatchResponseDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generic batch processor that applies a function to each item in a list,
 * capturing per-item errors without rolling back successful items.
 */
@Component
public class BatchProcessor {

    public <I, O> BatchResponseDTO<O> process(List<I> items, Function<I, O> processor) {
        List<O> successful = new ArrayList<>();
        List<BatchItemErrorDTO> failed = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            try {
                successful.add(processor.apply(items.get(i)));
            } catch (Exception ex) {
                failed.add(new BatchItemErrorDTO(i, ex.getMessage()));
            }
        }

        return new BatchResponseDTO<>(successful, failed, items.size(), successful.size(), failed.size());
    }
}
