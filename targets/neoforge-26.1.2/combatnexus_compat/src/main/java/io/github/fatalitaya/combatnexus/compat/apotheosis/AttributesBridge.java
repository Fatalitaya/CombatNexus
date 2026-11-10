package io.github.fatalitaya.combatnexus.compat.apotheosis;

import io.github.fatalitaya.combatnexus.combat.api.CombatRegistrar;

public final class AttributesBridge {
    private static io.github.fatalitaya.combatnexus.combat.api.platform.NativeDamageAccess nativeDamage;
    private AttributesBridge() {}
    public static boolean manages(net.minecraft.world.damagesource.DamageSource source) {
        return nativeDamage != null && nativeDamage.managed(source);
    }
    public static void register(CombatRegistrar registrar) {
        nativeDamage = registrar.nativeDamage();
        var lifeSteal = io.github.fatalitaya.combatnexus.api.id.NexusId.parse("apothic_attributes:life_steal");
        registrar.settlementPolicy(new io.github.fatalitaya.combatnexus.combat.api.behavior.SettlementPolicy(lifeSteal, 0,
                java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.DAMAGE),
                java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.HEALING), context -> {
            var settlement = context.settlement().result(io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot.class).orElseThrow();
            var live = io.github.fatalitaya.combatnexus.combat.damage.api.event.DamageSettlementContext.current(settlement);
            var attacker = live.logicalAttacker();
            // Preserve native melee/physical eligibility, then apply the shared primary-hit policy.
            if (attacker == null || live.directEntity() != attacker || context.authority().get().isEmpty()
                    || !live.evidence().reactionInputs().damageTags().contains(io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(net.neoforged.neoforge.common.Tags.DamageTypes.IS_PHYSICAL.location()))
                    || live.evidence().reactionInputs().damageTags().contains(io.github.fatalitaya.combatnexus.platform.NativeIds.nexus(dev.shadowsoffire.apothic_attributes.api.ALObjects.Tags.IS_NON_PHYSICAL.location())))
                return new io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult("ineligible_lifesteal", java.util.Optional.empty());
            var attributes = settlement.evidence().attributes().get(attacker.getUUID());
            double ratio = attributes == null ? 0 : attributes.valueOrZero(lifeSteal);
            var request = io.github.fatalitaya.combatnexus.combat.healing.api.Lifesteal.fromPrimaryHealthLoss(
                    settlement, lifeSteal, ratio > 0.001 ? ratio : 0);
            if (request.isEmpty()) return new io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult("ineligible_lifesteal", java.util.Optional.empty());
            var result = context.world().healing().submit(request.orElseThrow(), context.authority().get().orElseThrow());
            return new io.github.fatalitaya.combatnexus.combat.api.reaction.ReactionResult("child_" + result.outcome().name(),
                    java.util.Optional.of("healing/" + result.request().requestId()));
        }));
        var critical = io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("apothic_attributes", "multi_critical");
        registrar.damageFormula(critical, (input, samples) -> {
            var attacker = input.attacker();
            if (attacker.isEmpty()) return new io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy.Output(input.amount(), java.util.List.of());
            var values = attacker.orElseThrow().attributes();
            var chanceValue = values.values().get(io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("apothic_attributes", "crit_chance"));
            double chance = chanceValue == null ? 0 : chanceValue.effective();
            float multiplier = values.valueOrZero(io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("apothic_attributes", "crit_damage"));
            float base = (float)input.amount(), damage = base;
            var multipliers = new java.util.ArrayList<Double>();
            // Preserve native sample order, float arithmetic, additive overcrits and 15% decay.
            while (samples.nextFloat() <= chance && multiplier > 1.0F) {
                chance--; damage += base * (multiplier - 1); multipliers.add((double)multiplier); multiplier *= 0.85F;
            }
            return new io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy.Output(damage, multipliers);
        });
        registrar.bridges().register(new io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability(critical, critical, critical,
                io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("combatnexus_compat", "apothic_attributes"),
                io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability.Mode.REPLACE, java.util.Set.of(),
                "Frozen multi-critical formula with host-recorded samples and multiplier sequence"));
        var overheal = io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("apothic_attributes", "overheal");
        registrar.bridges().register(new io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability(overheal, overheal, overheal,
                io.github.fatalitaya.combatnexus.api.id.NexusId.fromNamespaceAndPath("combatnexus_compat", "apothic_attributes"),
                io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCapability.Mode.REPLACE,
                java.util.Set.of(io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation.ABSORPTION),
                "Native absorption grant with source-owned capacity and native-first consumption"));
    }
}
