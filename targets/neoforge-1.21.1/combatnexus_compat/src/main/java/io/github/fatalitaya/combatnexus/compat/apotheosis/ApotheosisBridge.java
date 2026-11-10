package io.github.fatalitaya.combatnexus.compat.apotheosis;

import java.util.Optional;
import java.util.Set;
import dev.shadowsoffire.apotheosis.Apoth;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.player.Player;
import io.github.fatalitaya.combatnexus.attribute.platform.api.EffectiveAttributePolicy;
import io.github.fatalitaya.combatnexus.combat.api.CombatRegistrar;
import io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability;

/** Only loaded after NeoForge has confirmed the locked optional mod is present. */
public final class ApotheosisBridge {
    private ApotheosisBridge() {}
    public static void register(CombatRegistrar registrar, io.github.fatalitaya.combatnexus.attribute.platform.api.AttributeCaptureRegistrar attributes) {
        EquipmentFacts.register(registrar);
        attributes.register(id("attribute_toggle"), 100, (entity, attribute, raw, effective) -> {
            if (entity instanceof Player player && player.getData(Apoth.Attachments.ATTRIBUTE_TOGGLES).isSuppressed(attribute))
                return Optional.of(new EffectiveAttributePolicy.Reading(effective, id("attribute_toggle")));
            return Optional.empty();
        });
        registrar.bridges().register(new BridgeCapability(id("attribute_toggle"), id("attribute_read"),
            NexusId.fromNamespaceAndPath("minecraft", "living_entity/get_attribute_value"),
            NexusId.fromNamespaceAndPath("combatnexus_compat", "apotheosis"), BridgeCapability.Mode.OBSERVE,
            Set.of(), "Native effective value captured with Apotheosis toggle attribution"));
        registrar.bridges().register(new BridgeCapability(id("executing"), id("post_attack"), id("executing/do_post_attack"),
                NexusId.fromNamespaceAndPath("combatnexus_compat", "apotheosis"), BridgeCapability.Mode.REPLACE,
                Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.LETHAL), "Execution routes through the shared lethal coordinator"));
        registrar.bridges().register(new BridgeCapability(id("mageslayer"), id("on_hurt"), id("mageslayer/on_hurt"),
                NexusId.fromNamespaceAndPath("combatnexus_compat", "apotheosis"), BridgeCapability.Mode.REPLACE,
                Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.HEALING), "Frozen child healing before the reduced parent hit"));
        registrar.bridges().register(new BridgeCapability(id("bloody_arrow"),id("projectile_launch"),id("bloody_arrow/on_projectile_fired"),
                NexusId.fromNamespaceAndPath("combatnexus_compat","apotheosis"),BridgeCapability.Mode.REPLACE,
                Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE,
                        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.HEALING,
                        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.PROJECTILE,
                        io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.STATE),"Launch snapshot, damage cost, arrow multiplier and persistent cooldown"));
    }
    static NexusId id(String path) { return NexusId.fromNamespaceAndPath("apotheosis", path); }
    static void nativeCapability(CombatRegistrar registrar, String namespace, String name) {
        var id = NexusId.fromNamespaceAndPath(namespace, name);
        registrar.bridges().register(new BridgeCapability(id, id, id,
            NexusId.fromNamespaceAndPath(namespace, "native"), BridgeCapability.Mode.NATIVE,
            Set.of(), "Native execution retained; replacement not implemented"));
    }
}
