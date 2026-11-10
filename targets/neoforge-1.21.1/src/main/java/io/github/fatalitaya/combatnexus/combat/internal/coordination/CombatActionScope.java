package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import io.github.fatalitaya.combatnexus.combat.api.action.CombatAction;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import java.util.*;
/** Native adapter scope; never inferred from a tick or reused after attack returns. */
public final class CombatActionScope implements AutoCloseable {
    private static final ThreadLocal<Deque<CombatActionScope>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private final CombatAction action;
    private final io.github.fatalitaya.combatnexus.combat.platform.NativeEntityCapture.Participant captured;
    private final CombatRuntime world;
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private final DecisionSamples.Owner samplesOwner=new DecisionSamples.Owner(UUID.randomUUID(),0);
    private CombatActionScope(CombatRuntime world, UUID actor, CombatAction restored) {
        this.world=world;
        action = restored == null ? new CombatAction(UUID.randomUUID(), world.sessionId(), actor) : restored;
        if(!action.session().equals(world.sessionId())||!action.actor().equals(actor))throw new IllegalArgumentException("Foreign ability action");
        if (STACK.get().size() >= 64) throw new IllegalStateException("Action nesting capacity exceeded");
        captured = world.participantCapture(actor,
                io.github.fatalitaya.combatnexus.combat.internal.definition.CombatDefinitions.forRuntime(world.serverRuntime()).generation()).orElseThrow(() -> new IllegalArgumentException("Action participant is unavailable"));
        world.decisionSamples().retain(action.id(),samplesOwner);
        STACK.get().addLast(this);
    }
    public static CombatActionScope open(CombatRuntime world, UUID actor) { world.requireActive(); return new CombatActionScope(world, actor, null); }
    public static CombatActionScope resume(CombatRuntime world,CombatAction action){world.requireActive();return new CombatActionScope(world,action.actor(),action);}
    /** Current invocation identity only; this read grants no command authority. */
    public static Optional<CombatAction> current(UUID session) {
        var current=STACK.get().peekLast();
        return current!=null && current.action.session().equals(session) ? Optional.of(current.action) : Optional.empty();
    }
    public static Optional<CombatAction> current(UUID session, UUID actor) {
        var current = STACK.get().peekLast();
        return current != null && current.action.session().equals(session) && current.action.actor().equals(actor)
                ? Optional.of(current.action) : Optional.empty();
    }
    public static Optional<io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame> input(UUID session, UUID actor) {
        var current = STACK.get().peekLast();
        return current != null && current.action.session().equals(session) && current.action.actor().equals(actor)
                ? Optional.of(current.captured.frame()) : Optional.empty();
    }
    public static Optional<io.github.fatalitaya.combatnexus.combat.api.input.CapturedItem> weapon(UUID session, UUID actor) {
        var current = STACK.get().peekLast();
        return current != null && current.action.session().equals(session) && current.action.actor().equals(actor)
            ? Optional.of(current.captured.nativeFacts().weapon()) : Optional.empty();
    }
    public void close() {
        if (closed) return;
        if (Thread.currentThread() != owner || STACK.get().peekLast() != this) throw new IllegalStateException("Action scope close order");
        STACK.get().removeLast(); if (STACK.get().isEmpty()) STACK.remove(); closed = true;
        if(world.decisionSamples().release(action.id(),samplesOwner))world.participantStates().actionFinished(action.actor(), action.id());
    }
}
