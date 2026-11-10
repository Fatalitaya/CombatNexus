package io.github.fatalitaya.combatnexus.combat.api.scheduling;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
/** A composite retains child evidence even if its own application was rejected or incomplete. */
public record IntentReceipt(List<CommandOutcome<?>> steps, CommandResult<Boolean> result) {
    public IntentReceipt { steps=List.copyOf(steps);Objects.requireNonNull(result); }
}
