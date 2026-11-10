package io.github.fatalitaya.combatnexus.combat.ability.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import java.util.Objects;

public record SavedAbilityCooldown(String key,NexusId clock,long remaining) {
    public SavedAbilityCooldown {Objects.requireNonNull(key);Objects.requireNonNull(clock);if(key.length()>1024||remaining<0||remaining>1200000)throw new IllegalArgumentException("Invalid saved ability cooldown");}
}
