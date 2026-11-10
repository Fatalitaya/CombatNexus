package io.github.fatalitaya.combatnexus.combat.damage.builtin.processor;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.config.VanillaCompatibilitySettings;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.core.DamageComponent;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.fatalitaya.combatnexus.combat.damage.core.pipeline.DamagePhaseAccess;
import io.github.fatalitaya.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.fatalitaya.combatnexus.combat.damage.api.VanillaDamageTags;

public class ArmorMitigationProcessor implements DamagePhaseProcessor {
    @Override
    public void apply(NativeDamageRuleContext context) {
        DamagePhaseAccess ctx = DamageInternalContexts.phase(
                context,
                "phase processor"
        );

        if (ctx.settings().values().vanillaCompatibility().executor(VanillaCompatibilitySettings.Reduction.ARMOR)
                != VanillaCompatibilitySettings.Executor.COMBATNEXUS) return;

        ctx.consumeDefenseInput(io.github.fatalitaya.combatnexus.combat.damage.api.context.DefenseInput.ARMOR);
        float effectiveness = ctx.getArmorEffectivenessMultiplier();
        float armor = Math.max(0.0f, ctx.penetratedArmor() * effectiveness);

        if (armor <= 0.0f) {
            ctx.setArmorHandled();
            return;
        }

        float toughness = ctx.getVictimAttrOrZero(io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes.armorToughness());
        float toughnessFactor = 2.0f + (toughness / 4.0f);
        float kValue = Math.max(
                0.0001f,
                ctx.settings().values().formulas().asymptoticKValue()
        );

        for (int i = 0; i < ctx.getActiveComponentCount(); i++) {
            DamageComponent component = ctx.getActiveComponent(i);

            DamageChannelRegistry.ChannelData channelData =
                    ctx.definitions().damage().channels().data(component.channel);

            if (!channelData.affectedByArmor()) {
                continue;
            }

            float currentDmg = component.getPostMitigationAmount();

            if (currentDmg <= 0.0f) {
                continue;
            }

            var mitigation = io.github.fatalitaya.combatnexus.formula.ArmorFormula.calculate(armor, currentDmg, toughnessFactor, kValue);
            float effectiveArmor = mitigation.effectiveArmor();
            float reductionPercent = mitigation.reduction();

            if (reductionPercent <= 0.0f) {
                continue;
            }

            ctx.tryAddChannelMitigation(
                    component.channel,
                    reductionPercent,
                    "dn:armor_reduction"
            );

        }

        ctx.setArmorHandled();
    }

    @Override
    public boolean canHandle(NativeDamageContextView context) {
        DamagePhaseAccess ctx = DamageInternalContexts.phase(
                context,
                "phase processor predicate"
        );

        return !io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().matchesDamageTag(ctx.source(), VanillaDamageTags.BYPASSES_ARMOR)
                && !ctx.isArmorHandled();
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.DN_ARMOR_MITIGATION;
    }
}

