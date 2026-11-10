package io.github.fatalitaya.combatnexus.combat.damage.api.damage;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.DomainCommand;
import java.util.Objects;
import java.util.UUID;

/** Immediate native damage request; retains the captured entity identities. */
public record NativeDamageCommand(UUID requestId, DamageRequest request) implements DomainCommand.Native {
    public NativeDamageCommand { Objects.requireNonNull(requestId); Objects.requireNonNull(request); }
    @Override public NexusId type() { return NexusId.fromNamespaceAndPath("combatnexus", "damage"); }
}
