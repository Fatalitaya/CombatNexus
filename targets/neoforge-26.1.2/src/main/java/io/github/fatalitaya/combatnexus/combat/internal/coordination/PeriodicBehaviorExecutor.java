package io.github.fatalitaya.combatnexus.combat.internal.coordination;
import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;

import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.behavior.*;
import io.github.fatalitaya.combatnexus.combat.api.scheduling.DueQueue;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatFailures;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import java.util.*;
import java.util.function.Predicate;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.LivingEntity;

/** All periodic behaviors share installation identity, deadlines, cancellation and saved remaining time. */
final class PeriodicBehaviorExecutor {
    private record Key(long installation, NexusId type) {}
    private record Work(BehaviorSource source, PeriodicBehavior behavior) { Key key() { return new Key(source.installation(), behavior.type()); } }
    private final CombatRuntime world;
    private final Map<Key, Long> tasks = new HashMap<>(), deadlines = new HashMap<>();
    private final DueQueue<Work> queue = new DueQueue<>(4096);
    private final io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time;
    PeriodicBehaviorExecutor(CombatRuntime world, io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time) { this.world = world; this.time = Objects.requireNonNull(time); }
    void validate(Collection<BehaviorSource> sources, long now) {
        long count = 0;
        for (var source : sources) for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic) {
            if (periodic.intervalTicks() < 1 || periodic.intervalTicks() > 1200000) throw new IllegalArgumentException("Periodic interval out of range");
            if (periodic.initialDelayTicks() < 0 || periodic.initialDelayTicks() > 1200000) throw new IllegalArgumentException("Initial delay out of range");
            Math.addExact(time.now(source.target(), source.source()), periodic.intervalTicks()); count++;
        }
        if (count > 4096) throw new IllegalStateException("Behavior task capacity exceeded");
    }
    private void schedule(Work work, long due) {
        if (tasks.containsKey(work.key()) || work.source().expiresAtTick().isPresent() && due >= work.source().expiresAtTick().getAsLong()) return;
        deadlines.put(work.key(), due); tasks.put(work.key(), queue.schedule(due, work));
    }
    void install(BehaviorSource source, long now) {
        for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic)
            schedule(new Work(source, periodic), Math.addExact(now,
                    periodic.phase().firstDelay(now, source.expiresAtTick(), periodic.intervalTicks(), periodic.initialDelayTicks())));
    }
    void refresh(BehaviorSource source, Map<NexusId, Long> remaining, long now, boolean preserveAll) {
        for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic
                && (preserveAll || periodic.phase() == PeriodicPhase.PRESERVE) && remaining.containsKey(component.type())) {
            var work = new Work(source, periodic);
            var task = tasks.remove(work.key());
            if (task != null) queue.cancel(task);
            deadlines.remove(work.key());
            schedule(work, Math.addExact(now, remaining.get(component.type())));
        }
    }
    void cancel(long installation) {
        for (var key : List.copyOf(tasks.keySet())) if (key.installation() == installation) { queue.cancel(tasks.remove(key)); deadlines.remove(key); }
    }
    private long nextDeadline(Work work,long now) {
        long delay=work.behavior().intervalTicks();
        if(work.behavior().phase()==PeriodicPhase.REMAINING_DURATION) {
            delay=work.behavior().phase().firstDelay(now,work.source().expiresAtTick(),work.behavior().intervalTicks(),(int)delay);
            if(delay==0)delay=work.behavior().intervalTicks();
        }
        return Math.addExact(now,delay);
    }
    /** An admitted external activation consumes the same periodic opportunity as automatic execution. */
    void consumeExternalAbility(BehaviorSource source,NexusId contribution) {
        var behavior=source.plan().components().stream()
            .filter(io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.Periodic.class::isInstance)
            .map(io.github.fatalitaya.combatnexus.combat.ability.internal.AbilityBehaviors.Periodic.class::cast)
            .filter(component->component.contribution().equals(contribution)).findFirst()
            .orElseThrow(()->new IllegalArgumentException("Periodic ability contribution absent"));
        var work=new Work(source,behavior);long next=nextDeadline(work,time.now(source.target(),source.source()));
        var task=tasks.remove(work.key());if(task!=null)queue.cancel(task);deadlines.remove(work.key());
        schedule(work,next);
    }
    void tick(long now, Predicate<BehaviorSource> present, Predicate<BehaviorSource> valid) {
        while (true) {
            var next=queue.firstDue(work -> time.advancing(work.source().target(),work.source().source())
                && world.driverControl().allowsAutomatic(work.source().target(),io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope.PERIODIC,work.source().source())
                ? time.now(work.source().target(), work.source().source()) : Long.MIN_VALUE);
            if(next.isEmpty())break;
            if(!time.mayExecute())break;
            var task=next.get();
            queue.cancel(task.id());
            var work = task.value(); var source = work.source();
            now = time.now(source.target(), source.source()); tasks.remove(work.key()); deadlines.remove(work.key());
            if (!present.test(source) || !(world.level().getEntity(source.target()) instanceof LivingEntity living) || !living.isAlive()) continue;
            schedule(work,nextDeadline(work,now));
            // A skipped world tick consumes the missed modulo trigger without moving
            // the effect onto a different phase or issuing a catch-up command.
            if (work.behavior().phase() == PeriodicPhase.REMAINING_DURATION && source.expiresAtTick().isPresent()
                    && (source.expiresAtTick().getAsLong() - now) % work.behavior().intervalTicks() != 0) continue;
            if(!valid.test(source))continue;
            var execution = new Execution(source, valid);
            try { work.behavior().execute(execution); }
            catch (Throwable failure) {
                CombatFailures.rethrowFatal(failure);
            } finally { execution.open = false; }
        }
    }
    private final class Execution implements PeriodicExecution {
        final BehaviorSource source; final Predicate<BehaviorSource> valid; boolean open = true;
        Execution(BehaviorSource source, Predicate<BehaviorSource> valid) { this.source = source; this.valid = valid; }
        private void check() { world.requireActive(); if (!open || !valid.test(source)) throw new IllegalStateException("Periodic command capability expired"); }
        public BehaviorSource source() { check(); return source; }
        public io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult ability(NexusId contribution,io.github.fatalitaya.combatnexus.combat.api.action.TriggerLimit limit){
            check();
            if(!world.driverControl().allowsAutomatic(source.target(),io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope.PERIODIC,source.source()))
                return new io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult(Optional.empty(),CommandResult.rejected(CommandRejection.Code.INVALID_PHASE,"Periodic ability is externally owned"));
            var refusal=world.claimAbilityPeriodic(source,contribution,limit);
            if(refusal.isPresent())return new io.github.fatalitaya.combatnexus.combat.ability.api.AbilityResult(Optional.empty(),CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,refusal.orElseThrow()));
            return world.abilityControl().activate(source.target(),source.source(),contribution,null,io.github.fatalitaya.combatnexus.combat.driver.api.DriverScope.PERIODIC);
        }
        public void environmentDamage(io.github.fatalitaya.combatnexus.effect.api.EnvironmentDamage parameters) {
            check();
            io.github.fatalitaya.combatnexus.effect.internal.EnvironmentEffects.scheduled(
                (LivingEntity)world.level().getEntity(source.target()), source.effect(), parameters);
        }
        public void effectCapability(NexusId capability) {
            check();
            var effect = world.effects().snapshot(source.target()).stream().filter(e -> e.snapshot().effect().equals(source.effect())
                    && e.snapshot().source().equals(source.source())).findFirst().orElseThrow();
            var entity = (LivingEntity) world.level().getEntity(source.target());
            if (!io.github.fatalitaya.combatnexus.effect.internal.EffectCapabilities.execute(capability,
                    new io.github.fatalitaya.combatnexus.effect.api.EffectCapability.Context(entity, effect,
                        io.github.fatalitaya.combatnexus.effect.api.EffectCapability.Trigger.TICK, null, 0)))
                world.effects().remove(source.target(), source.effect(), source.source(), io.github.fatalitaya.combatnexus.effect.api.EffectService.Removal.CONDITION_ENDED);
        }
        public io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame input() {
            check();
            return new io.github.fatalitaya.combatnexus.combat.internal.WorldServices(world).facts(source.target()).orElseThrow();
        }
        public io.github.fatalitaya.combatnexus.combat.api.CommandOutcome<?> execute(io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledIntent intent) {
            check();return io.github.fatalitaya.combatnexus.combat.internal.transaction.DomainCommandExecutor.execute(world,
                new io.github.fatalitaya.combatnexus.combat.api.DomainCommand.Intent(UUID.randomUUID(),source.target(),
                    new io.github.fatalitaya.combatnexus.combat.api.CombatSource(source.source(),source.owner(),Optional.empty()),intent));
        }
        public HealingSettlement heal(double amount) {
            check();
            var request=new HealingRequest(UUID.randomUUID(),source.target(),
                new io.github.fatalitaya.combatnexus.combat.api.CombatSource(source.source(),source.owner(),Optional.empty()),HealingRequest.Reason.EFFECT,amount);
            return io.github.fatalitaya.combatnexus.combat.internal.transaction.DomainCommandExecutor.execute(world,
                new io.github.fatalitaya.combatnexus.combat.api.DomainCommand.Heal(request)).result(HealingSettlement.class).orElseThrow();
        }
        public DamageResult damage(double amount, NexusId damageType) {
            return damage(amount, damageType, 0);
        }
        public DamageResult damage(double amount, NexusId damageType, float minimumHealth) {
            check();
            return io.github.fatalitaya.combatnexus.combat.internal.transaction.DomainCommandExecutor.damage(world,source.target(),
                new io.github.fatalitaya.combatnexus.combat.api.CombatSource(source.source(),source.owner(),Optional.empty()),amount,damageType,minimumHealth).result(DamageResult.class).orElseThrow();
        }
    }

    CommandResult<Boolean> instant(BehaviorSource source) {
        var results = new ArrayList<CommandOutcome<?>>();
        var execution = new InstantExecution() {
            boolean open = true;
            private void check() { world.requireActive(); if (!open) throw new IllegalStateException("Instant capability expired"); }
            public BehaviorSource source() { check(); return source; }
            public CommandOutcome<?> damage(double amount, NexusId type, Optional<UUID> direct) {
                check();
                var entity = (LivingEntity)world.level().getEntity(source.target());
                var holder = world.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getOrThrow(ContentAccess.get().damageKey(type));
                var nativeSource = new net.minecraft.world.damagesource.DamageSource(holder,
                    direct.map(world.level()::getEntity).orElse(null), source.owner().map(world.level()::getEntity).orElse(null));
                return submit(new io.github.fatalitaya.combatnexus.combat.damage.api.damage.NativeDamageCommand(UUID.randomUUID(), DamageRequest.builder(world.level(), entity,
                    io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().describeDamageSource(nativeSource), (float)amount).build()));
            }
            public CommandOutcome<?> submit(DomainCommand command) {
                check();
                var result = io.github.fatalitaya.combatnexus.combat.internal.transaction.DomainCommandExecutor.execute(world, command);
                results.add(result);
                if (!result.completed()) { open = false; throw new IllegalStateException("Instant command incomplete"); }
                return result;
            }
        };
        boolean complete = true;
        try {
            for (var component : source.plan().components()) ((InstantBehavior) component).execute(execution);
        } catch (RuntimeException failure) {
            complete = false;
        } finally { execution.open = false; }
        var journal = new ApplicationJournal(results.stream().flatMap(r -> r.journal().entries().stream()).toList(),
                complete && results.stream().allMatch(CommandOutcome::completed));
        var outcome = complete && results.stream().allMatch(CommandOutcome::applied) ? CommandResult.applied(true)
                : CommandResult.<Boolean>rejected(complete ? CommandRejection.Code.NATIVE_REFUSED : CommandRejection.Code.INCOMPLETE_APPLICATION,
                    "Instant effect child did not apply");
        return outcome.withJournal(journal);
    }

    Map<NexusId, Long> remaining(long installation, long now) {
        var result = new HashMap<NexusId, Long>();
        deadlines.forEach((key, deadline) -> { if (key.installation() == installation) result.put(key.type(), Math.max(0, deadline - now)); });
        return Map.copyOf(result);
    }
    void restore(BehaviorSource source, Map<NexusId, Long> remaining, long now) {
        cancel(source.installation());
        for (var component : source.plan().components()) if (component instanceof PeriodicBehavior periodic && remaining.containsKey(component.type())) {
            long delay = remaining.get(component.type());
            if (delay < 0 || delay > 1200000) throw new IllegalArgumentException("Invalid saved periodic deadline");
            schedule(new Work(source, periodic), Math.addExact(now, delay));
        }
    }
    void clear() { deadlines.clear(); tasks.clear(); queue.clear(); }
}
