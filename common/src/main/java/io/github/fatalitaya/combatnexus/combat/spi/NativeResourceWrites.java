package io.github.fatalitaya.combatnexus.combat.spi;

import io.github.fatalitaya.combatnexus.combat.api.platform.HealthTarget;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;

public final class NativeResourceWrites {
    private static final ThreadLocal<Token> CURRENT = new ThreadLocal<>();
    private NativeResourceWrites() {}
    public static Token health(HealthTarget target) {
        CombatExecutionScope.requireNativeWritePhase();
        var previous = CURRENT.get();
        for (var active = previous; active != null; active = active.previous)
            if (active.target.equals(target)) active.uncontended = false;
        var token = new Token(target, previous); CURRENT.set(token); return token;
    }
    public static void requireWritePhase() { CombatExecutionScope.requireNativeWritePhase(); }
    public static final class Token implements AutoCloseable {
        private final HealthTarget target;
        private final Token previous;
        private final Thread thread = Thread.currentThread();
        private boolean uncontended = true, closed;
        private Token(HealthTarget target, Token previous) { this.target = target; this.previous = previous; }
        public boolean uncontended() { return uncontended; }
        @Override public void close() {
            if (closed) return;
            if (Thread.currentThread() != thread || CURRENT.get() != this) throw new IllegalStateException("Native write scope order");
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
            closed = true;
        }
    }
}
