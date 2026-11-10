package io.github.fatalitaya.combatnexus.combat.damage.platform.registry;

import com.mojang.serialization.Codec;
import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DamageModule.MODID);

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Boolean>
            > TEST_ITEM =
            COMPONENTS.register(
                    "test_item",
                    () -> DataComponentType
                            .<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .build()
            );

    private ModDataComponents() {
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
