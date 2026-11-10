package io.github.fatalitaya.combatnexus.attribute.api;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.LinkedHashMap;

/** Assembles authoritative captured values; never reapplies Effect modifiers. */
public final class AttributeReader {
    private AttributeReader() {}
    public static AttributeSnapshot capture(AttributeObservation observed, long revision) {
        var values = new LinkedHashMap<NexusId, AttributeValueFrame.Value>();
        var sources = new LinkedHashMap<NexusId, java.util.List<AttributeSnapshot.Contribution>>();
        for (var entry : observed.entries()) {
            values.put(entry.attribute(), entry.value());
            sources.put(entry.attribute(), entry.contributions());
        }
        return new AttributeSnapshot(new AttributeValueFrame(observed.entity(), observed.gameTick(), revision, values),
                new AttributeProvenance(observed.entity(), observed.gameTick(), revision, sources));
    }
}
