package PSG.backEnd.service.implementation.forecast.applier;

/** Referencia tipada al registro real generado por un applier. */
public record AppliedEntityRef(String entityType, Long entityId) {
    public static AppliedEntityRef of(String type, Long id) {
        return new AppliedEntityRef(type, id);
    }
}
