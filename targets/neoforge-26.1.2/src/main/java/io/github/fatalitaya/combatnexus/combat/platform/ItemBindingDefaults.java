package io.github.fatalitaya.combatnexus.combat.platform;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec;
import net.minecraft.core.HolderLookup;
import java.util.*;
/** Observe final pending prototypes, before vanilla adopts them, scoped to the reload lookup. */
public final class ItemBindingDefaults {
    private static final Map<HolderLookup.Provider,Map<NexusId,CombatItemBindingSpec>> PENDING=new WeakHashMap<>();
    private ItemBindingDefaults(){}
    public static void observe(net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event){
        event.modifyMatching((item,components)->true,(components,context,item)->components.addValidator(finalComponents->{
            var id=io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
            var spec=finalComponents.getOrDefault(CombatDataComponents.BINDINGS.get(),CombatItemBindingSpec.EMPTY);
            synchronized(PENDING){PENDING.computeIfAbsent(context,ignored->new LinkedHashMap<>()).put(id,spec);}
        }));
    }
    public static Map<NexusId,CombatItemBindingSpec> take(HolderLookup.Provider context){
        synchronized(PENDING){var result=PENDING.remove(context);if(result==null)throw new IllegalStateException("Pending item defaults unavailable");return Map.copyOf(result);}
    }
}
