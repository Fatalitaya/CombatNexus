package io.github.fatalitaya.combatnexus.combat.healing.api;

import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import io.github.fatalitaya.combatnexus.formula.Amounts;
import java.util.Objects;
import java.util.UUID;

/** Independent healing command; never interpreted as negative damage. */
public record HealingRequest(UUID requestId, UUID target, CombatSource source, Reason reason, double amount,
                             java.util.Optional<UUID> healer, java.util.Optional<io.github.fatalitaya.combatnexus.combat.api.transaction.ParticipantIdentity> targetInstance) {
    public HealingRequest(UUID requestId, UUID target, CombatSource source, Reason reason, double amount) {
        this(requestId, target, source, reason, amount, source.owner());
    }
    public HealingRequest(UUID requestId, UUID target, CombatSource source, Reason reason, double amount, java.util.Optional<UUID> healer) {
        this(requestId, target, source, reason, amount, healer, java.util.Optional.empty());
    }
    public HealingRequest {
        Objects.requireNonNull(healer, "healer");
        Objects.requireNonNull(targetInstance, "targetInstance");
        if (targetInstance.isPresent() && !targetInstance.get().entity().equals(target))
            throw new IllegalArgumentException("Healing target instance UUID mismatch");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(reason, "reason");
        Amounts.nonNegative(amount, "amount");
    }

    public enum Reason { NATURAL_REGENERATION, EFFECT, ITEM, LIFESTEAL, SKILL, CRYSTAL, UNKNOWN_EXTERNAL }
}
