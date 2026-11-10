package io.github.fatalitaya.combatnexus.presentation.platform;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.presentation.internal.AttributeRowFormatter;
import io.github.fatalitaya.combatnexus.attribute.api.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Formatting only; the caller supplies already observed values and modifier facts. */
public final class EntityAttributeRows {
    private EntityAttributeRows() {}
    public static JsonObject value(NexusId id, double base, double value) {
        return AttributeRowFormatter.value(id, base, value, io.github.fatalitaya.combatnexus.attribute.spi.AttributeCatalog.get().descriptionId(id),
                AttributeUnits.unit(id).map(Enum::name).orElse(""),
                AttributeCatalog.definition(id).map(definition -> definition.reserved()));
    }
    public static JsonArray sources(List<AttributeSnapshot.Contribution> sources) {
        return AttributeRowFormatter.sources(sources);
    }
    public static void effective(JsonObject row, double raw, double effective, NexusId reason) {
        AttributeRowFormatter.effective(row, raw, effective, reason);
    }
}
