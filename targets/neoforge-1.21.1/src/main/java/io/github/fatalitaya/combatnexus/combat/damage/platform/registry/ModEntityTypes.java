package io.github.fatalitaya.combatnexus.combat.damage.platform.registry;

import io.github.fatalitaya.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import io.github.fatalitaya.combatnexus.combat.damage.platform.entity.DamageDummyEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageModule entity type registry.
 *
 * <p>Only the damage dummy is registered here; it is a development/testing
 * LivingEntity target, not a content feature.</p>
 */
public final class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DamageModule.MODID);

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<DamageDummyEntity>
            > DAMAGE_DUMMY =
            ENTITIES.register(
                    "damage_dummy",
                    () -> EntityType.Builder.of(DamageDummyEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("combatnexus:damage_dummy")
            );

    private ModEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
