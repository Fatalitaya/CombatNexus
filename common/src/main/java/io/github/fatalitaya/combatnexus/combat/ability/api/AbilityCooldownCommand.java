package io.github.fatalitaya.combatnexus.combat.ability.api;

import io.github.fatalitaya.combatnexus.combat.api.CombatSource;
import java.util.*;

public record AbilityCooldownCommand(UUID requestId, UUID actor, CombatSource source,
        AbilityCooldownSlot slot, Operation operation, int ticks) {
    public enum Operation { START, CLEAR, RESET }
    public AbilityCooldownCommand {
        Objects.requireNonNull(requestId); Objects.requireNonNull(actor); Objects.requireNonNull(source);
        Objects.requireNonNull(slot); Objects.requireNonNull(operation);
        if(ticks<0||ticks>1200000||(operation==Operation.CLEAR&&ticks!=0))throw new IllegalArgumentException("Invalid cooldown duration");
    }
}
