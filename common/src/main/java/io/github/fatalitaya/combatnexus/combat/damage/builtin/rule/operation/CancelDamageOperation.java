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

import java.util.Set;

public record CancelDamageOperation(
        String sourceId
) implements DamageRuleOperation {

    public static final MapCodec<CancelDamageOperation> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING
                            .optionalFieldOf(
                                    "source",
                                    RuleTraceIds.CANCEL_DAMAGE
                            )
                            .forGetter(CancelDamageOperation::sourceId)
            ).apply(instance, CancelDamageOperation::new));

    public CancelDamageOperation() {
        this(RuleTraceIds.CANCEL_DAMAGE);
    }

    @Override
    public NexusId type() {
        return DamageModuleOperationIds.CANCEL_DAMAGE;
    }

    @Override
    public DamageMutationResult apply(DamageRuleContext ctx) {
        return ctx.tryCancelDamage(sourceId);
    }

    @Override
    public Set<DamagePhase> supportedPhases() {
        return Set.of(DamagePhase.FINAL_OVERRIDE);
    }

    @Override
    public float stackingValue() {
        return 1.0f;
    }
}
