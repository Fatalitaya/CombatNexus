package io.github.fatalitaya.combatnexus.presentation.internal;

import com.google.gson.*;
import io.github.fatalitaya.combatnexus.attribute.api.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Formatting only; the caller supplies already observed values and modifier facts. */
public final class AttributeRowFormatter {
    private AttributeRowFormatter() {}
    public static JsonObject value(NexusId id, double base, double value, String description, String unit, Optional<Boolean> reserved) {
        var row = new JsonObject();
        row.addProperty("attribute", id.toString()); row.addProperty("name", description);
        row.addProperty("base", base); row.addProperty("value", value);
        row.addProperty("unit", unit);
        reserved.ifPresent(valueReserved -> row.addProperty("reserved", valueReserved));
        return row;
    }
    public static JsonArray sources(List<AttributeSnapshot.Contribution> sources) {
        var rows = new JsonArray();
        for (var source : sources) rows.add("  " + source.source() + " : " + source.amount() + " (" + source.operation() + ")");
        return rows;
    }
    public static void effective(JsonObject row, double raw, double effective, NexusId reason) {
        row.addProperty("value", effective); row.addProperty("raw", raw); row.addProperty("effective_reason", reason.toString());
    }
}
