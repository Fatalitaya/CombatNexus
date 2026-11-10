package io.github.fatalitaya.combatnexus.combat.platform;
import io.github.fatalitaya.combatnexus.combat.api.item.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class CombatDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS=DeferredRegister.create(Registries.DATA_COMPONENT_TYPE,"combatnexus");
    public static final java.util.function.Supplier<DataComponentType<CombatItemBindingSpec>> BINDINGS=COMPONENTS.register("item_bindings",()->DataComponentType.<CombatItemBindingSpec>builder().persistent(CombatItemBindingSpec.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CombatItemBindingSpec.CODEC)).build());
    public static final java.util.function.Supplier<DataComponentType<CombatItemBindingPatch>> PATCH=COMPONENTS.register("item_binding_patch",()->DataComponentType.<CombatItemBindingPatch>builder().persistent(CombatItemBindingPatch.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CombatItemBindingPatch.CODEC)).build());
    private CombatDataComponents(){}
    public static void register(IEventBus bus){COMPONENTS.register(bus);bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,ItemBindingDefaults::observe);}
}
