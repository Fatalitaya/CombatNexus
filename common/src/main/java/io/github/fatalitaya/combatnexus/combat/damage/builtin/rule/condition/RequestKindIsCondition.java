package io.github.fatalitaya.combatnexus.combat.damage.builtin.rule.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.fatalitaya.combatnexus.combat.damage.api.rule.DamageModuleConditionIds;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Objects;

public record RequestKindIsCondition(
        DamageRequestKind kind
) implements DamageRuleCondition {
    public static final MapCodec<RequestKindIsCondition> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DamageRequestKind.CODEC.fieldOf("kind")
                            .forGetter(RequestKindIsCondition::kind)
            ).apply(instance, RequestKindIsCondition::new));

    public RequestKindIsCondition {
        Objects.requireNonNull(kind, "kind");
    }

    @Override public NexusId type() {
        return DamageModuleConditionIds.REQUEST_KIND_IS;
    }

    @Override public boolean test(DamageContextView ctx) {
        return ctx.requestKind() == kind;
    }
}
