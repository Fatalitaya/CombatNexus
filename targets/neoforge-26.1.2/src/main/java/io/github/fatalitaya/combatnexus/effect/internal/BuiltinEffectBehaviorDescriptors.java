package io.github.fatalitaya.combatnexus.effect.internal;
import io.github.fatalitaya.combatnexus.combat.api.behavior.BuiltinBehaviors;
import io.github.fatalitaya.combatnexus.effect.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageText;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit;
import io.github.fatalitaya.combatnexus.presentation.api.*;
import io.github.fatalitaya.combatnexus.presentation.api.DisplayText;
import io.github.fatalitaya.combatnexus.presentation.api.GeneratedTexts;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.*;
public final class BuiltinEffectBehaviorDescriptors {
    public static void register() {
        EffectBehaviorDescriptors.installPeriodicDetails(value -> {
            if (!(value instanceof PeriodicBehavior periodic)) return List.of();
            var details = new ArrayList<DisplayText>();
            details.add(GeneratedTexts.translatable("tooltip.combatnexus.effect.phase." + periodic.phase().name().toLowerCase(Locale.ROOT)));
            if (periodic.phase() != PeriodicPhase.REMAINING_DURATION)
                details.add(GeneratedTexts.translatable("tooltip.combatnexus.effect.first_delay", TooltipNumbers.number(periodic.initialDelayTicks() / 20.0)));
            return details;
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:scheduled_environment_damage"), ScheduledEnvironmentDamage.class, (scheduled, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            var damage = scheduled.parameters();
                    summary.add(text("periodic_base_damage", TooltipNumbers.number(damage.intervalTicks() / 20.0), DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
                    details.add(text("independent_cooldown"));
                    if (damage.minimumHealth() > 0) details.add(text("minimum_health", TooltipNumbers.number(damage.minimumHealth())));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:environment_damage"), EnvironmentDamage.class, (damage, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(damage.intervalTicks() > 0 ? text("periodic_damage", TooltipNumbers.number(damage.intervalTicks() / 20.0), DamageText.amount(damage.amount(), DamageText.type(damage.damageType())))
                        : text("environment_damage", DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
                    if (damage.minimumHealth() > 0) details.add(text("minimum_health", TooltipNumbers.number(damage.minimumHealth())));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:lethal_protection"), BuiltinBehaviors.LethalProtection.class, (protection, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(protection.cooldownTicks() == 0 ? text("lethal") : text("lethal_cooldown", TooltipNumbers.number(protection.cooldownTicks() / 20.0)));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:periodic_healing"), BuiltinBehaviors.PeriodicHealing.class, (healing, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(text("periodic", TooltipNumbers.number(healing.intervalTicks() / 20.0), TooltipNumbers.number(healing.amount())));
                    if (healing.onlyWhenMissingHealth()) summary.add(text("missing_health"));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:periodic_damage"), BuiltinBehaviors.PeriodicDamage.class, (damage, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(text("periodic_damage", TooltipNumbers.number(damage.intervalTicks() / 20.0), DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
                    if (damage.minimumHealth() > 0) details.add(text("minimum_health", TooltipNumbers.number(damage.minimumHealth())));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:instant_healing"), BuiltinBehaviors.InstantHealing.class, (healing, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(text("instant_healing", TooltipNumbers.number(healing.amount())));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:instant_damage"), BuiltinBehaviors.InstantDamage.class, (damage, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(text("instant_damage", DamageText.amount(damage.amount(), DamageText.type(damage.damageType()))));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:effect_capability"), EffectCapabilityBehavior.class, (capability, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.addAll(io.github.fatalitaya.combatnexus.effect.internal.EffectCapabilities.describe(capability.capability(), level));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:healing_additive"), BuiltinBehaviors.HealingAdditive.class, (healing, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(text("healing", TooltipNumbers.number(healing.amount())));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:reflection"), BuiltinBehaviors.Reflection.class, (reflection, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(text("reflection", TooltipNumbers.percent(reflection.ratio())));
                    details.add(text("reflection.detail"));
                    details.add(limit(reflection.limit()));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:item_block_reflection"), BuiltinBehaviors.ItemBlockReflection.class,
            (reflection, level) -> new TooltipBlock(Optional.empty(),
                List.of(text("item_block_reflection", TooltipNumbers.percent(reflection.ratio()))),
                List.of(text("item_block_reflection.detail"), limit(reflection.limit()))));
        EffectBehaviorDescriptors.register(NexusId.parse("combatnexus:overflow_shield"), BuiltinBehaviors.OverflowShield.class, (shield, level) -> {
            var summary = new ArrayList<DisplayText>();
            var details = new ArrayList<DisplayText>();
            summary.add(text("overflow", TooltipNumbers.percent(shield.ratio()), TooltipNumbers.number(shield.capacity()), TooltipNumbers.number(shield.durationTicks() / 20.0)));
                    details.add(text("overflow.detail"));
                    details.add(limit(shield.limit()));
            return new TooltipBlock(java.util.Optional.<DisplayText>empty(), summary, details);
        });
    }
    private static DisplayText limit(TriggerLimit limit) {
        return text("limit", text("scope." + limit.scope().name().toLowerCase(Locale.ROOT)), limit.maximum());
    }
    private static DisplayText text(String key, Object... args) { return GeneratedTexts.translatable("tooltip.combatnexus.effect." + key, args); }
}
