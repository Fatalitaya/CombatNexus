package io.github.fatalitaya.combatnexus.combat.damage.core;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;

/** Immutable index order captured when the platform bucket registry freezes. */
public final class PreMultiplierLayout {
    private final List<NexusId> ids;
    private final Map<NexusId, Integer> indices;
    public PreMultiplierLayout(List<NexusId> ids) {
        this.ids = List.copyOf(ids);
        var indices = new LinkedHashMap<NexusId, Integer>();
        for (int i = 0; i < ids.size(); i++)
            if (indices.put(ids.get(i), i) != null) throw new IllegalArgumentException("Duplicate pre-multiplier bucket");
        this.indices = Map.copyOf(indices);
    }
    public List<NexusId> ids() { return ids; }
    public int size() { return ids.size(); }
    public int indexOf(NexusId id) { return indices.getOrDefault(id, -1); }
}
