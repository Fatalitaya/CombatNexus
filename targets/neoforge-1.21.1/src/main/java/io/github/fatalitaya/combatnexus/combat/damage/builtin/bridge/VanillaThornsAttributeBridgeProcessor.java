package io.github.fatalitaya.combatnexus.combat.damage.builtin.bridge;

import io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.fatalitaya.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.NativeDamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.DamageContributionSourceKind;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.display.VanillaContributionFacts;

import io.github.fatalitaya.combatnexus.attribute.api.CombatAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class VanillaThornsAttributeBridgeProcessor
        implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;
    private static final String TRACE_ID = "vanilla:thorns_attribute";

    @Override
    public void apply(NativeDamageRuleContext context) {
        var ctx = context;

        LivingEntity owner = thornsOwner(ctx);
        if (owner == null) {
            return;
        }

        float bonus = thornsBonus(ctx, owner);
        if (!Float.isFinite(bonus) || bonus <= EPSILON) {
            return;
        }

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.nativeInput().initialBaseBucket(),
                bonus,
                TRACE_ID
        );

        ctx.recordContribution(
                result,
                VanillaContributionFacts.vanillaBase(
                        DamageModuleIds.id("vanilla_thorns_attribute"),
                        DamageContributionSourceKind.VANILLA_ENCHANTMENT,
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.nativeInput().initialBaseBucket(),
                        bonus,
                        TRACE_ID
                )
        );
    }

    @Override
    public boolean canHandle(NativeDamageContextView context) {
        var ctx = context;

        if (!ctx.isManaged()
                || !ctx.source().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
            return false;
        }

        LivingEntity owner = thornsOwner(ctx);
        return owner != null && thornsBonus(ctx, owner) > EPSILON;
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_THORNS_ATTRIBUTE_BRIDGE;
    }

    private static LivingEntity thornsOwner(NativeDamageContextView ctx) {
        Entity sourceEntity = ctx.source().getEntity();
        return sourceEntity instanceof LivingEntity living ? living : null;
    }

    private static float thornsBonus(NativeDamageContextView context, LivingEntity owner) {
        return context.attributeSnapshot(owner.getUUID())
                .map(snapshot -> snapshot.valueOrZero(CombatAttributes.THORNS))
                .orElse(0.0f);
    }
}
