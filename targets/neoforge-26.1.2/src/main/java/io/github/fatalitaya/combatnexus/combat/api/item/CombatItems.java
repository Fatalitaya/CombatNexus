package io.github.fatalitaya.combatnexus.combat.api.item;
import net.minecraft.world.item.*;
import io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents;
import java.util.*;
import java.util.function.Consumer;
/** Native item declaration and authorized server instance edits. */
public final class CombatItems {
    private static final Map<Item.Properties,CombatItemBindingSpec> DECLARATIONS=new WeakHashMap<>();
    private CombatItems(){}
    public static synchronized Item.Properties defaults(Item.Properties properties,Consumer<CombatItemBindingBuilder> declaration){
        Objects.requireNonNull(properties);var builder=new CombatItemBindingBuilder();declaration.accept(builder);var spec=builder.build();
        var merged=new CombatItemBindingPatch(false,spec.bindings().stream().map(CombatItemBindingPatch.Operation::add).toList()).apply(DECLARATIONS.getOrDefault(properties,CombatItemBindingSpec.EMPTY));
        DECLARATIONS.put(properties,merged);return properties.component(CombatDataComponents.BINDINGS.get(),merged);
    }
    public static CombatItemBindingSpec defaults(ItemStack stack){return stack.getItem().components().getOrDefault(CombatDataComponents.BINDINGS.get(),CombatItemBindingSpec.EMPTY);}
    public static CombatItemBindingPatch patch(ItemStack stack){return stack.getOrDefault(CombatDataComponents.PATCH.get(),CombatItemBindingPatch.EMPTY);}
    public static void patch(net.minecraft.server.level.ServerLevel level, ItemStack stack, CombatItemBindingPatch patch){
        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        if(!level.getServer().isSameThread())throw new IllegalStateException("Item patch requires server thread");
        var definitions=io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions.forRuntime(io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime.of((io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding)level.getServer()));
        definitions.validateBindings(patch.apply(definitions.itemBindings().getOrDefault(io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())),defaults(stack))));
        stack.set(CombatDataComponents.PATCH.get(),patch);
    }
    public static void inherit(net.minecraft.server.level.ServerLevel level,ItemStack stack){patch(level,stack,CombatItemBindingPatch.EMPTY);stack.remove(CombatDataComponents.PATCH.get());}
}
