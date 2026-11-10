package io.github.fatalitaya.combatnexus.combat.damage.platform.entity;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Validates the whole batch, then applies it in request order. Writes are not rolled back. */
public final class DamageDummyAttributeBatch {
    public interface AttributeEditor {
        double sanitize(double requestedValue);
        void write(double value);
    }
    @FunctionalInterface
    public interface EditPort {
        Optional<AttributeEditor> find(NexusId attribute);
    }
    private record ValidatedEdit(AttributeEditor editor, double value) {}
    private DamageDummyAttributeBatch() {}

    public static boolean validateAndApply(EditPort port, List<DamageDummyAttributeEdit> edits) {
        Objects.requireNonNull(port, "port");
        if (edits == null || edits.size() > DamageDummyAttributeProtocol.MAX_ATTRIBUTES) return false;
        Set<NexusId> seen = new HashSet<>();
        for (var edit : edits) {
            if (edit == null || edit.attributeId() == null || !seen.add(edit.attributeId())) return false;
        }
        List<ValidatedEdit> validated = new ArrayList<>(edits.size());
        for (var edit : edits) {
            var editor = port.find(edit.attributeId()).orElse(null);
            if (editor == null || !Double.isFinite(edit.requestedBaseValue())) return false;
            double value = editor.sanitize(edit.requestedBaseValue());
            if (!Double.isFinite(value)) return false;
            validated.add(new ValidatedEdit(editor, value));
        }
        for (var edit : validated) edit.editor().write(edit.value());
        return true;
    }
}
