package io.github.fatalitaya.combatnexus.compat.apotheosis;

import dev.shadowsoffire.apothic_attributes.api.ALCombatRules;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.attribute.api.AttributeSnapshot;
import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import net.minecraft.world.entity.LivingEntity;

/** Only transforms inputs owned by CombatNexus. Native-owned reductions keep their native bypass. */
public final class AttributePenetration implements DamagePhaseProcessor {
    @Override public DamagePhase phase() { return DamagePhase.MITIGATION_SETUP; }
    @Override public int getPriority() { return DamageProcessorPriorities.VANILLA_ARMOR_EFFECTIVENESS + 1; }

    @Override public void apply(NativeDamageRuleContext context) {
        var attacker = context.logicalAttacker();
        var identity = attacker != null ? java.util.Optional.of(attacker.getUUID())
                : io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileFacts.launch(context.directEntity())
                    .map(launch -> launch.shooter().entity());
        if (identity.isEmpty()) return;
        var captured = context.attributeSnapshot(identity.orElseThrow());
        if (captured.isEmpty()) return;
        var attributes = captured.orElseThrow();
        if (context.nativeInput().armor() == NativeDamageInput.Reduction.COMBATNEXUS) {
            float toughness = context.input(context.victim().getUUID()).attributes().valueOrZero(CombatAttributes.armorToughness());
            float resistance = ALCombatRules.getBypassResistance(context.offensiveAmount(), context.defenseInput(DefenseInput.ARMOR), toughness);
            context.tryPenetrateDefense(DefenseInput.ARMOR,
                    positive(attributes, "armor_shred") * (1 - resistance),
                    positive(attributes, "armor_pierce") * (1 - resistance), id("armor_penetration"));
        }
        if (context.nativeInput().enchantments() == NativeDamageInput.Reduction.COMBATNEXUS) {
            context.tryPenetrateDefense(DefenseInput.PROTECTION,
                    positive(attributes, "prot_shred"), positive(attributes, "prot_pierce"), id("protection_penetration"));
        }
    }

    private static float positive(AttributeSnapshot values, String path) {
        float value = values.valueOrZero(id(path));
        return value > 0.001F ? value : 0;
    }
    private static NexusId id(String path) { return NexusId.fromNamespaceAndPath("apothic_attributes", path); }
}
