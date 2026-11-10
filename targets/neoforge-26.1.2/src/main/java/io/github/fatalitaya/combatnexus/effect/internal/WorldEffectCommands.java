package io.github.fatalitaya.combatnexus.effect.internal;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.CommandRejection;
import io.github.fatalitaya.combatnexus.combat.api.CommandResult;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatOperation;
import io.github.fatalitaya.combatnexus.combat.api.transaction.CombatParent;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.StateCommands;
import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.effect.api.EffectApplication;
import io.github.fatalitaya.combatnexus.effect.api.EffectBinding;
import io.github.fatalitaya.combatnexus.effect.api.EffectCleanse;
import io.github.fatalitaya.combatnexus.effect.api.EffectCommands;
import io.github.fatalitaya.combatnexus.effect.api.EffectFact;
import io.github.fatalitaya.combatnexus.effect.api.EffectLifetime;
import io.github.fatalitaya.combatnexus.effect.api.EffectOwnership;
import io.github.fatalitaya.combatnexus.effect.api.EffectResult;
import io.github.fatalitaya.combatnexus.effect.api.EffectService;
import io.github.fatalitaya.combatnexus.effect.api.EffectView;
import io.github.fatalitaya.combatnexus.effect.api.NativeEffectCommand;
import io.github.fatalitaya.combatnexus.effect.api.NativeEffectReceipt;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

/** World-owned domain commands; mutations enter through StateCommands. */
public final class WorldEffectCommands implements EffectCommands {
    private final CombatRuntime world;
    public WorldEffectCommands(CombatRuntime world) { this.world = Objects.requireNonNull(world); }

    public EffectResult fact(UUID request, EffectFact fact) {
        var result = StateCommands.submit(world, request, fact.effect().snapshot().target(), CombatOperation.EFFECT, null, () -> {
            boolean satisfied;
            try (var computation = CombatExecutionScope.computation()) {
                satisfied = EffectMappings.satisfied(fact);
            }
            return world.effects().bind(new EffectBinding(fact.effect(), satisfied, fact.stacking())) ? CommandResult.applied(satisfied)
                : CommandResult.<Boolean>rejected(CommandRejection.Code.IMMUNE, "Condition binding refused");
        });
        return new EffectResult(result.applied() ? (result.value().orElseThrow() ? EffectResult.Outcome.APPLIED : EffectResult.Outcome.REMOVED)
            : EffectResult.Outcome.REJECTED, result.rejection(), result.journal(), result.context());
    }
    public CommandResult<Integer> clear(UUID request, UUID target) {
        return StateCommands.submit(world, request, target, CombatOperation.EFFECT, null, () -> {
            int removed = 0;
            for (var effect : world.effects().snapshot(target)) if (effect.policy().dispellable()) {
                var s = effect.snapshot(); world.effects().remove(target, s.effect(), s.source(), EffectService.Removal.DISPELLED); removed++;
            }
            return CommandResult.applied(removed);
        });
    }
    public CommandResult<Boolean> remove(UUID request, UUID target, NexusId effect, NexusId source, EffectService.Removal reason) {
        return StateCommands.submit(world, request, target, CombatOperation.EFFECT, null, () -> {
            world.effects().remove(target, effect, source, reason); return CommandResult.applied(true);
        });
    }
    public CommandResult<NativeEffectReceipt> nativeEffect(NativeEffectCommand command) { return nativeEffect(command, null); }
    public CommandResult<NativeEffectReceipt> nativeEffect(NativeEffectCommand command, CombatParent parent) {
        return StateCommands.submit(world, command.requestId(), command.target(), CombatOperation.EFFECT, Optional.of(command.source()), parent,
                () -> NativeEffects.apply((LivingEntity) world.level().getEntity(command.target()), command));
    }
    public List<EffectView> snapshot(UUID target) {
        var entity = world.level().getEntity(target);
        return entity instanceof LivingEntity living ? ContentAccess.get().captureEffects(living, ignored -> {}).views() : List.of();
    }
    public CommandResult<List<EffectCleanse.Entry>> cleanse(EffectCleanse request){return cleanse(request,null);}
    public CommandResult<List<EffectCleanse.Entry>> cleanse(EffectCleanse request,CombatParent parent){return StateCommands.submit(world,request.requestId(),request.target(),CombatOperation.EFFECT,parent,()->CommandResult.applied(world.effects().cleanse(request)));}
    public EffectResult apply(EffectApplication request) { return apply(request, null); }
    public EffectResult bind(UUID request, EffectBinding binding) { return bind(request, binding, null); }
    public EffectResult bind(UUID request, EffectBinding binding, CombatParent parent) {
        var result = StateCommands.submit(world, request, binding.effect().snapshot().target(), CombatOperation.EFFECT, parent,
                () -> world.effects().bind(binding) ? CommandResult.applied(true)
                    : CommandResult.<Boolean>rejected(CommandRejection.Code.IMMUNE, "Effect binding rejected"));
        return new EffectResult(result.applied() ? (binding.satisfied() ? EffectResult.Outcome.APPLIED : EffectResult.Outcome.REMOVED)
                : EffectResult.Outcome.REJECTED, result.rejection(), result.journal(), result.context());
    }
    public EffectResult apply(EffectApplication request, CombatParent parent) {
        var result = StateCommands.submit(world, request.requestId(), request.effect().snapshot().target(), CombatOperation.EFFECT, parent, () -> {
            if (request.effect().ownership() != EffectOwnership.MANAGED)
                return CommandResult.<Boolean>rejected(CommandRejection.Code.INVALID_INPUT, "Observed effects are not installable");
            if (request.effect().snapshot().lifetime() == EffectLifetime.Kind.INSTANT)
                return world.effects().applyInstant(request.effect());
            var expiry = request.effect().snapshot().expiresAtTick();
            if (expiry.isPresent() && expiry.getAsLong() <= world.businessTick(request.effect().snapshot().target(), request.effect().snapshot().source()))
                return CommandResult.<Boolean>rejected(CommandRejection.Code.EXPIRED, "Effect duration already elapsed");
            try { return world.effects().apply(request.effect(), request.stacking()) ? CommandResult.applied(true)
                : CommandResult.<Boolean>rejected(CommandRejection.Code.IMMUNE, "Effect immunity rejected application"); }
            catch (RuntimeException failure) {
                return CommandResult.<Boolean>rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Effect contribution incomplete");
            }
        });
        return new EffectResult(result.applied() ? EffectResult.Outcome.APPLIED : EffectResult.Outcome.REJECTED, result.rejection(), result.journal(), result.context());
    }
    public EffectResult dispel(UUID request, EffectView effect) { return dispel(request, effect, null); }
    public EffectResult dispel(UUID request, EffectView effect, CombatParent parent) {
        var result = StateCommands.submit(world, request, effect.target(), CombatOperation.EFFECT, parent, () -> {
            var current = snapshot(effect.target()).stream().filter(e -> e.definition().equals(effect.definition()) && e.source().equals(effect.source()) && e.ownership() == effect.ownership()).findFirst();
            if (current.isEmpty()) return CommandResult.<Boolean>rejected(CommandRejection.Code.NOT_FOUND, "Effect absent");
            if (!current.get().operations().contains(EffectView.Operation.DISPEL)) return CommandResult.<Boolean>rejected(CommandRejection.Code.NOT_DISPELLABLE, "Owner rejects dispel");
            if (effect.ownership() == EffectOwnership.MANAGED)
                world.effects().remove(effect.target(), effect.definition(), effect.source().orElseThrow(), EffectService.Removal.DISPELLED);
            else {
                var entity = (LivingEntity)world.level().getEntity(effect.target());
                var holder = ContentAccess.get().effect(effect.definition()).orElseThrow();
                if (!entity.removeEffect(holder)) return CommandResult.<Boolean>rejected(CommandRejection.Code.NOT_DISPELLABLE, "Native removal refused");
            }
            return CommandResult.applied(true);
        });
        return new EffectResult(result.applied() ? EffectResult.Outcome.REMOVED : EffectResult.Outcome.REJECTED, result.rejection(), result.journal(), result.context());
    }

}
