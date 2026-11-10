package io.github.fatalitaya.combatnexus.combat.damage.platform.entity;
/** Pure ownership reconciliation; native lookup and application belong to the target. */
public final class DamageDummyOwnership {
    public enum Decision { KEEP, DEFER, TERMINATE, ADOPT, SPAWN }
    private DamageDummyOwnership() {}
    public static Decision decide(boolean bound, boolean keeperPresent, boolean localPresent, boolean ready) {
        if (keeperPresent) return Decision.KEEP;
        if (bound) return ready ? Decision.TERMINATE : Decision.DEFER;
        if (localPresent) return Decision.ADOPT;
        return ready ? Decision.SPAWN : Decision.DEFER;
    }
}
