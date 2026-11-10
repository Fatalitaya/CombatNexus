package io.github.fatalitaya.combatnexus.combat.damage.api.rule;

import com.mojang.serialization.Codec;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.fatalitaya.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.fatalitaya.combatnexus.combat.damage.registry.rule.DamageRuleOperationTypes;
import io.github.fatalitaya.combatnexus.api.id.NexusId;

import java.util.Set;

/**
 * A rule operation supplied through the Java API.
 *
 * <p>Implementations must remain immutable after validation and must return
 * stable, immutable phase/reference metadata. DamageModule cannot defensively
 * copy arbitrary state owned by third-party executable code.</p>
 */
public interface DamageRuleOperation {

    Codec<DamageRuleOperation> CODEC =
            io.github.fatalitaya.combatnexus.api.id.NexusIdCodecs.CODEC.dispatch(
                    "type",
                    DamageRuleOperation::type,
                    DamageRuleOperationTypes::codec
            );

    NexusId type();

    DamageMutationResult apply(DamageRuleContext ctx);

    /** Numeric budget for stackingValue; extensions default to ordinary damage values. */
    enum NumericLimit { DAMAGE, MULTIPLIER }

    default NumericLimit numericLimit() { return NumericLimit.DAMAGE; }

    default float stackingValue() {
        return 0.0f;
    }

    default Set<DamagePhase> supportedPhases() {
        return Set.of();
    }

    default boolean supportsPhase(DamagePhase phase) {
        Set<DamagePhase> phases = supportedPhases();
        return phases.isEmpty() || phases.contains(phase);
    }
}
