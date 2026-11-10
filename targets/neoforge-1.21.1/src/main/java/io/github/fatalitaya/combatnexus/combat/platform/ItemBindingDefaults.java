package io.github.fatalitaya.combatnexus.combat.platform;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import net.minecraft.core.HolderLookup;
import java.util.*;
/** Capture adopted item prototypes when each reload candidate is constructed. */
public final class ItemBindingDefaults {
    private ItemBindingDefaults() {}
    public static Map<NexusId, CombatItemBindingSpec> take(HolderLookup.Provider context) {
        var result = new LinkedHashMap<NexusId, CombatItemBindingSpec>();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id = io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
            result.put(id, item.components().getOrDefault(CombatDataComponents.BINDINGS.get(), CombatItemBindingSpec.EMPTY));
        }
        return Map.copyOf(result);
    }
}
