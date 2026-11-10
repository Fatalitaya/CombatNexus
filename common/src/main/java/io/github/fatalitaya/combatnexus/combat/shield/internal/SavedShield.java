package io.github.fatalitaya.combatnexus.combat.shield.internal;

import java.util.Objects;
import io.github.fatalitaya.combatnexus.combat.shield.api.ShieldGrant;
import io.github.fatalitaya.combatnexus.formula.Amounts;

public record SavedShield(ShieldGrant grant, double balance, long damageAge, boolean damaged) {
    public SavedShield {
        Objects.requireNonNull(grant); Amounts.nonNegative(balance, "saved balance");
        if (balance > grant.definition().capacity() || damageAge < 0) throw new IllegalArgumentException("Invalid saved shield state");
    }
}
