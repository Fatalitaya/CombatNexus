package io.github.fatalitaya.combatnexus.combat.internal.transaction;
import io.github.fatalitaya.combatnexus.combat.internal.definition.DefinitionGeneration;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;

import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.api.*;
import java.util.*;
import io.github.fatalitaya.combatnexus.combat.spi.ServerRuntimeBinding;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;


/** One execution kernel per server session; world services share its quotas and completion FIFO. */
public final class CombatServerRuntime implements AutoCloseable {
    public record ProcessorFailure(io.github.fatalitaya.combatnexus.api.id.NexusId processor,
            io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase phase,
            String callback, long request, String exceptionType, String message) {}
    private final Deque<ProcessorFailure> processorFailures = new ArrayDeque<>();
    public List<ProcessorFailure> processorFailures() { requireThread(); return List.copyOf(processorFailures); }
    public void recordProcessorFailure(io.github.fatalitaya.combatnexus.api.id.NexusId processor,
            io.github.fatalitaya.combatnexus.combat.damage.api.enums.DamagePhase phase, String callback, long request, Exception failure) {
        requireThread();
        String message = failure.getMessage();
        if (message == null) message = "";
        if (message.length() > 512) message = message.substring(0, 509) + "...";
        if (processorFailures.size() == 128) processorFailures.removeFirst();
        processorFailures.addLast(new ProcessorFailure(processor, phase, callback, request, failure.getClass().getName(), message));
    }
    private volatile CombatLimits configuredLimits = CombatLimits.DEFAULT;
    private final ServerSession session;
    private final BooleanSupplier owningThread;
    private final IntSupplier serverTick;
    public ServerSession session() { return session; }
    private final CombatAdmissionBudget budget = new CombatAdmissionBudget();
    private final CombatCompletionQueue completions = new CombatCompletionQueue();
    private final Map<WorldSession, CombatRuntime> worlds = new IdentityHashMap<>();
    private final Map<CombatOperation, CombatSubmissionGate<Object>> submissions = new EnumMap<>(CombatOperation.class);
    private volatile boolean closed;
    public boolean isClosed() { return closed; }
    public boolean isOwningThread() { return owningThread.getAsBoolean(); }
    private DefinitionGeneration definitions;
    public Optional<DefinitionGeneration> definitions() { requireThread(); return Optional.ofNullable(definitions); }
    public void adoptDefinitions(DefinitionGeneration candidate) {
        requireThread(); Objects.requireNonNull(candidate);
        if (definitions != null && candidate.generation() <= definitions.generation()) throw new IllegalArgumentException("Generations must increase");
        configuredLimits = candidate.settings().safety().combatLimits();
        definitions = candidate;
        for(var world:List.copyOf(worlds.values()))lifecycle(world,world::definitionsAdopted);
        io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.definitions(session,new DefinitionView(candidate.generation(),candidate.domains().effects().keySet(),candidate.behaviors().keySet(),candidate.domains().shields().keySet(),candidate.domains().regeneration().keySet(),candidate.domains().healingRules().keySet()));
    }
    public void clearDefinitions() { requireThread(); definitions = null; }

    public CombatServerRuntime(ServerSession session, BooleanSupplier owningThread, IntSupplier serverTick) {
        this.session = Objects.requireNonNull(session);
        this.owningThread = Objects.requireNonNull(owningThread);
        this.serverTick = Objects.requireNonNull(serverTick);
        requireThread();
    }
    public static CombatServerRuntime of(ServerRuntimeBinding binding) {
        return existing(binding).orElseThrow(() -> new IllegalStateException("Server combat session unavailable"));
    }
    public static Optional<CombatServerRuntime> existing(ServerRuntimeBinding binding) {
        return Objects.requireNonNull(binding).combatnexus$serverRuntime().filter(runtime -> !runtime.closed);
    }
    public static void stop(ServerRuntimeBinding binding) { existing(binding).ifPresent(CombatServerRuntime::close); }
    public CombatLimits limits() { return configuredLimits; }
    public void requireThread() {
        if (closed || !owningThread.getAsBoolean()) throw new IllegalStateException("Combat requires a live owning server thread");
    }
    public CombatAdmissionBudget.Result admit(CombatLineage lineage) {
        requireThread();
        lineage.restrictOperations(io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions.allowedOperations());
        if (!lineage.bindServer(session)) throw new IllegalArgumentException("Combat lineage belongs to another server");
        if (tickets.containsKey(lineage)) throw new IllegalStateException("Operation already admitted");
        var slots = new ArrayDeque<CombatCompletionQueue.Slot>();
        try {
            for (int index = 0; index < (lineage.operation() == CombatOperation.ABILITY ? 4 : lineage.operation() == CombatOperation.DAMAGE ? 2 : 1); index++)
                slots.addLast(completions.reserve());
        } catch (IllegalStateException exhausted) {
            slots.forEach(CombatCompletionQueue.Slot::close);
            return new CombatAdmissionBudget.Result(CombatAdmissionBudget.Rejection.COMPLETION_CAPACITY,
                    lineage.derivedCount(), budget.count(serverTick.getAsInt()));
        }
        var result = budget.admit(lineage, serverTick.getAsInt(), limits());
        if (result.admitted()) tickets.put(lineage, new AdmissionTicket(lineage, slots));
        else slots.forEach(CombatCompletionQueue.Slot::close);
        return result;
    }

    private final Map<CombatLineage, AdmissionTicket> tickets = new IdentityHashMap<>();
    public boolean hasTicket(CombatLineage lineage) { requireThread(); return tickets.containsKey(lineage); }
    public AdmissionTicket ticket(CombatLineage lineage) {
        requireThread();
        return Objects.requireNonNull(tickets.get(lineage), "No live admission ticket");
    }
    public final class AdmissionTicket implements AutoCloseable, CombatExecutionScope.ApplicationPermit {
        public ServerSession serverIdentity() { return session; }
        private final CombatLineage lineage;
        private DefinitionGeneration definitions = CombatServerRuntime.this.definitions;
        private boolean boundDefinitions;
        private boolean entered;
        public void bindDefinitions(DefinitionGeneration definitions) {
            requireActive();
            if (entered || boundDefinitions && this.definitions != definitions)
                throw new IllegalStateException("Admission definitions already fixed");
            this.definitions = Objects.requireNonNull(definitions); boundDefinitions = true;
        }
        public boolean hasDefinitionBinding() { requireActive(); return boundDefinitions; }
        public DefinitionGeneration definitions() { requireActive(); return Objects.requireNonNull(definitions, "No adopted definitions"); }
        private boolean active = true;
        private final Deque<CombatCompletionQueue.Slot> slots;
        private WorldSession level;
        private ParticipantIdentity target;
        private UUID requestedTarget;
        private CombatRuntime world;
        public void bindWorld(WorldSession level) {
            requireActive();
            Objects.requireNonNull(level);
            if (level.server() != session || this.level != null && this.level != level)
                throw new IllegalArgumentException("Ticket belongs to a different world");
            var world = Objects.requireNonNull(worlds.get(level), "World combat session unavailable");
            this.level = level;
            this.world = world;
        }
        public void bindSession(WorldSession level, ParticipantIdentity target) {
            bindWorld(level);
            Objects.requireNonNull(target);
            bindTargetId(target.entity());
            if (this.target != null && !this.target.equals(target))
                throw new IllegalArgumentException("Ticket belongs to a different participant instance");
            this.target = target;
        }
        private void bindTargetId(UUID target) {
            Objects.requireNonNull(target);
            if (requestedTarget != null && !requestedTarget.equals(target))
                throw new IllegalArgumentException("Ticket belongs to a different target");
            requestedTarget = target;
        }
        public void bindTarget(CombatRuntime world, UUID target) {
            bindWorld(world.worldSession());
            bindTargetId(target);
            world.participantIdentity(target).ifPresent(identity -> bindSession(world.worldSession(), identity));
        }
        private AdmissionTicket(CombatLineage lineage, Deque<CombatCompletionQueue.Slot> slots) {
            this.lineage = lineage; this.slots = slots;
            // Every request selects its own source binding before entering a phase.
            // An unbound child starts with the currently adopted definitions.
        }
        public void requireActive() {
            requireThread();
            if (!active || tickets.get(lineage) != this || level != null && worlds.get(level) != world) throw new IllegalStateException("Admission expired");
        }
        public CombatExecutionScope enter(CombatExecutionScope.Phase phase) {
            requireActive(); entered = true; return CombatExecutionScope.enter(phase, new ApplicationPermit(this));
        }
        public CombatLineage lineage() { requireActive(); return lineage; }
        public void close() { requireThread(); if (active) { active = false; slots.forEach(CombatCompletionQueue.Slot::close); slots.clear(); tickets.remove(lineage, this); } }
    }
    private final class ApplicationPermit implements CombatExecutionScope.ApplicationPermit {
        private final AdmissionTicket ticket;
        private boolean active = true;
        private ApplicationPermit(AdmissionTicket ticket) { this.ticket = Objects.requireNonNull(ticket); }
        public ServerSession serverIdentity() { return session; }
        public CombatLineage lineage() { return ticket.lineage; }
        public void requireActive() {
            if (!active) throw new IllegalStateException("Application session expired");
            ticket.requireActive();
        }
        public void close() { active = false; }
    }
    /** A native method starts only after its independent ticket is admitted and bound. */
    public CombatExecutionScope nativeApplication(WorldSession level, ParticipantIdentity target, CombatLineage lineage) {
        var ticket = ticket(lineage);
        ticket.bindSession(level, target);
        return ticket.enter(CombatExecutionScope.Phase.APPLICATION);
    }
    public void finish(CombatLineage lineage) {
        requireThread(); var ticket = tickets.get(lineage); if (ticket != null) ticket.close();
    }

    public int currentTickCount() { requireThread(); return budget.count(serverTick.getAsInt()); }
    public synchronized boolean claim(CombatOperation operation, Object requestIdentity) {
        if (closed) return false;
        return submissions.computeIfAbsent(operation, ignored -> new CombatSubmissionGate<>()).claim(requestIdentity);
    }
    /** Public services call this before admission. Mutable cross-domain recursion must not invent new roots. */
    public CombatLineage requestLineage(CombatOperation operation, CombatParent parent) {
        requireThread();
        CombatExecutionScope.requireSubmissionPhase();
        io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions.require(operation);
        if (CombatExecutionScope.isActive()) {
            if (parent != null) throw new IllegalStateException("Cannot replace the active application parent");
            var active = CombatExecutionScope.currentLineage(session).orElseThrow();
            ticket(active).requireActive();
            if (!active.permissions().contains(operation)) throw new IllegalStateException("Operation restricted by parent");
            return active.child(operation);
        }
        if (parent != null) return parent.child(session, operation);
        if (CombatDispatchScope.isActive()) return CombatDispatchScope.implicitChild(session, operation);
        return CombatLineage.root(operation);
    }
    /** Shield consumption enlisted in a live application shares the already admitted application authority; it is not another request. */
    public CombatLineage enlistedLineage(CombatOperation operation, long applicationId) {
        requireThread();
        var current = CombatExecutionScope.currentLineage(session);
        if (current.isEmpty() || current.get().id() != applicationId
                || operation != CombatOperation.SHIELD || current.get().operation() != CombatOperation.DAMAGE
                || !CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION)) {
            throw new IllegalStateException("Shield enlistment requires the exact active application");
        }
        return current.get();
    }
    public CombatCompletionQueue.Slot reserveCompletion(CombatLineage lineage) {
        var ticket = ticket(lineage); ticket.requireActive();
        if (ticket.slots.isEmpty()) throw new IllegalStateException("Admitted completion envelope exhausted");
        return ticket.slots.removeFirst();
    }
    public Optional<CombatCompletionQueue.Slot> rejectionCompletion() {
        requireThread();
        try { return Optional.of(completions.reserve()); } catch (IllegalStateException full) { return Optional.empty(); }
    }
    public void drain() { requireThread(); CombatCompletionQueue.drainIfSafe(); }
    public void lifecycle(CombatRuntime world, Runnable action) {
        requireThread();
        if (!worlds.containsValue(world)) throw new IllegalStateException("World session unavailable");
        WorldLifecycleScope.run(action);
    }
    public Optional<CombatRuntime> world(WorldSession level) { requireThread(); return Optional.ofNullable(worlds.get(level)); }
    public void attach(WorldSession level, CombatRuntime world) {
        requireThread();
        if (level.server() != session || world.serverSession() != session || world.worldSession() != level || worlds.putIfAbsent(level, world) != null) {
            throw new IllegalStateException("World already attached or belongs to another server");
        }
    }
    public void detach(WorldSession level, CombatRuntime world) { requireThread(); worlds.remove(level, world); }
    @Override public void close() {
        if (closed) return;
        requireThread();
        if (CombatExecutionScope.isActive() || CombatDispatchScope.isActive()) {
            throw new IllegalStateException("Cannot close combat server during a transaction or delivery");
        }
        drain();
        for (CombatRuntime world : List.copyOf(worlds.values())) io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess.close(world);
        worlds.clear();
        completions.clear();
        processorFailures.clear();
        definitions = null;
        tickets.values().forEach(value -> value.active = false);
        tickets.clear();
        synchronized (this) {
            submissions.values().forEach(CombatSubmissionGate::clear);
            submissions.clear();
            closed = true;
        }
    }
}

