package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.operation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.RuleTraceIds;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleOperationIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

public record OverrideFinalDamageOperation(
        float value
) implements DamageRuleOperation {

    public static final MapCodec<OverrideFinalDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.FLOAT
                            .fieldOf("value")
                            .forGetter(OverrideFinalDamageOperation::value)
            ).apply(instance, OverrideFinalDamageOperation::new));

    @Override
    public NexusId type() {
        return DamageModuleOperationIds.OVERRIDE_FINAL_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryOverrideFinalDamage(
                value,
                RuleTraceIds.OVERRIDE_FINAL_DAMAGE
        );
    }

    @Override
    public java.util.Set<DamagePhase> supportedPhases() {
        return java.util.Set.of(DamagePhase.FINAL_OVERRIDE);
    }

    @Override
    public float stackingValue() {
        return value;
    }
}
