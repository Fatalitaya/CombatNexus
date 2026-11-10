package io.github.fatalitaya.combatnexus.combat.internal.transaction;

/** Ticket-bound state application; only the transaction executor enters this scope. */
public final class StateApplicationScope implements AutoCloseable {
    private static final ThreadLocal<CombatExecutionScope.ApplicationPermit> APPLYING = new ThreadLocal<>();
    private final CombatExecutionScope.ApplicationPermit previous;
    private final CombatExecutionScope.ApplicationPermit current;
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private StateApplicationScope(CombatExecutionScope.ApplicationPermit ticket) {
        ticket.requireActive(); current = ticket; previous = APPLYING.get(); APPLYING.set(ticket);
    }
    static StateApplicationScope enter(CombatExecutionScope.ApplicationPermit ticket) { return new StateApplicationScope(ticket); }
    public static boolean isApplying() {
        var ticket = APPLYING.get();
        if (ticket == null || !CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION)
                || CombatExecutionScope.isActive(CombatExecutionScope.Phase.COMPUTE)) return false;
        ticket.requireActive();
        return CombatExecutionScope.currentLineage(ticket.serverIdentity()).orElse(null) == ticket.lineage();
    }
    @Override public void close() {
        if (closed) return;
        if (Thread.currentThread() != owner || APPLYING.get() != current) throw new IllegalStateException("State application scope close order");
        if (previous == null) APPLYING.remove(); else APPLYING.set(previous);
        closed = true;
    }
}
