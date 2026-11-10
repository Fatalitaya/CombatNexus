package io.github.fatalitaya.combatnexus.combat.internal;

import io.github.fatalitaya.combatnexus.combat.spi.ContentAccess;
import io.github.fatalitaya.combatnexus.combat.api.*;
import io.github.fatalitaya.combatnexus.combat.api.input.*;
import io.github.fatalitaya.combatnexus.combat.api.source.*;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.*;
import io.github.fatalitaya.combatnexus.combat.damage.api.damage.*;
import io.github.fatalitaya.combatnexus.combat.healing.api.*;
import io.github.fatalitaya.combatnexus.combat.shield.api.*;
import io.github.fatalitaya.combatnexus.combat.regeneration.api.*;
import io.github.fatalitaya.combatnexus.effect.api.*;
import java.util.*;
import io.github.fatalitaya.combatnexus.api.id.NexusId;
import net.minecraft.world.entity.LivingEntity;

/** Separate facade: no cast from a business service to the runtime grants application access. */
public final class WorldServices implements CombatWorld {
    private final CombatRuntime world;
    public WorldServices(CombatRuntime world) { this.world = world; }
    public io.github.fatalitaya.combatnexus.combat.driver.api.DriverService drivers() { return world.driverControl().businessView(); }
    public io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime time() { return new io.github.fatalitaya.combatnexus.combat.time.api.BusinessTime() {
        public long now(UUID target,NexusId source) { return world.businessTick(target,source); }
        public long phase(UUID target,NexusId source,long nativePhase) { return world.timeControl().phase(target,source,nativePhase); }
    }; }
    public io.github.fatalitaya.combatnexus.combat.ability.api.AbilityService abilities(){return world.abilities();}
    public java.util.List<io.github.fatalitaya.combatnexus.combat.api.item.EquipmentBindingFact> equipment(UUID actor){return world.equipmentFacts(actor);}
    public UUID session() { return world.sessionId(); }
    public Optional<io.github.fatalitaya.combatnexus.combat.api.action.CombatAction> currentAction(UUID actor) { world.level(); return io.github.fatalitaya.combatnexus.combat.internal.coordination.CombatActionScope.current(session(),actor); }
    public List<SourceDescription> sourceDescriptions(CombatInputFrame input) {
        world.level();if(!input.sessionId().equals(session()))throw new IllegalArgumentException("Foreign source snapshot");
        return io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.describe(input);
    }
    public Set<EffectSnapshot.RestrictedAction> restrictions(UUID target) {
        world.requireActive();
        int mask=world.effects().restrictionMask(Objects.requireNonNull(target));
        var actions=EnumSet.noneOf(EffectSnapshot.RestrictedAction.class);
        for(var action:EffectSnapshot.RestrictedAction.values())if((mask & (1 << action.ordinal()))!=0)actions.add(action);
        return Set.copyOf(actions);
    }
    public Optional<CombatInputFrame> facts(UUID target) {
        var entity = world.level().getEntity(target);
        return entity instanceof LivingEntity living ? Optional.of(world.captureInput(living, definitions().generation())) : Optional.empty();
    }
    public DefinitionView definitions() {
        var d = world.definitions();
        return new DefinitionView(d.generation(), d.domains().effects().keySet(), d.behaviors().keySet(), d.domains().shields().keySet(), d.domains().regeneration().keySet(), d.domains().healingRules().keySet());
    }
    public Optional<DefinitionDescription> definition(NexusId id,DefinitionDescription.Kind kind) {
        var d=world.definitions();
        Object value=switch(kind){case EFFECT->d.domains().effects().get(id);case BEHAVIOR->d.behaviors().get(id);case SHIELD->d.domains().shields().get(id);case REGENERATION->d.domains().regeneration().get(id);case HEALING_RULE->d.domains().healingRules().get(id);};
        if(value==null)return Optional.empty();
        Map<String,String> parameters=switch(kind) {
            case EFFECT -> {var v=d.domains().effects().get(id);yield Map.of("level",String.valueOf(v.level()),"lifetime",v.lifetime().kind().name(),"duration_ticks",String.valueOf(v.lifetime().ticks()),"stacking",v.stacking().toString());}
            case BEHAVIOR -> {var v=d.behaviors().get(id);yield Map.of("lifetime",String.valueOf(v.lifetime()),"components",String.valueOf(v.plan().components().size()));}
            case SHIELD -> {var v=d.domains().shields().get(id);yield Map.of("capacity",String.valueOf(v.capacity()),"priority",String.valueOf(v.priority()),"recovery_delay_ticks",String.valueOf(v.recoveryDelayTicks()),"recovery_per_second",String.valueOf(v.recoveryPerSecond()),"stacking",v.stacking().name());}
            case REGENERATION -> {var v=d.domains().regeneration().get(id);yield Map.of("health_per_second",String.valueOf(v.healthPerSecond()),"damage_delay_ticks",String.valueOf(v.damageDelayTicks()),"duration_ticks",String.valueOf(v.durationTicks()));}
            case HEALING_RULE -> {var v=d.domains().healingRules().get(id);yield Map.of("phase",v.phase().name(),"priority",String.valueOf(v.priority()),"additive",String.valueOf(v.additive()),"multiplier",String.valueOf(v.multiplier()));}
        };
        return Optional.of(new DefinitionDescription(d.generation(),id,kind,parameters));
    }
    public SourceResult install(DefinitionInstallation command) {
        var d=world.definitions();
        var id=command.definition();
        var effects=new LinkedHashMap<NexusId,EffectDefinition>();var shields=new LinkedHashMap<NexusId,SourcePlan.Shield>();var recovery=new LinkedHashMap<NexusId,RegenerationDefinition>();
        switch(command.kind()) {
            case EFFECT -> {var value=d.domains().effects().get(id);if(value!=null)effects.put(id,value);}
            case BEHAVIOR -> {var value=d.behaviors().get(id);if(value!=null)effects.put(id,new EffectDefinition(0,value.lifetime(),EffectService.Stacking.REPLACE,EffectPolicy.DEFAULT,List.of(),value.plan(),Set.of()));}
            case SHIELD -> {var value=d.domains().shields().get(id);if(value!=null)shields.put(id,new SourcePlan.Shield(value,value.capacity(),command.shieldDuration(),SourcePlan.ShieldRemoval.REVOKE));}
            case REGENERATION -> {var value=d.domains().regeneration().get(id);if(value!=null)recovery.put(id,value);}
            case HEALING_RULE -> throw new IllegalArgumentException("Global rule cannot be installed");
        }
        if(effects.isEmpty()&&shields.isEmpty()&&recovery.isEmpty()) {
            var rejected=StateCommands.<Boolean>submit(world,command.requestId(),command.target(),CombatOperation.SOURCE,Optional.of(new CombatSource(command.source().provider(),command.source().owner(),Optional.empty())),null,()->CommandResult.rejected(CommandRejection.Code.NOT_FOUND,"Definition absent from adopted generation"));
            return new SourceResult(Optional.empty(),rejected.rejection(),rejected.journal(),rejected.context());
        }
        return sources().install(new SourceInstallation(command.requestId(),command.target(),command.source(),new SourcePlan(id,effects,recovery,shields,Map.of(),command.persistent())));
    }
    public DamageResult damage(DamageRequest request) {
        if (request.level() != world.level()) throw new IllegalArgumentException("Foreign damage world");
        var result=io.github.fatalitaya.combatnexus.combat.damage.api.DamageModuleApi.submitDamage(request);
        var journal=result.settlement().map(s->s.evidence().journal()).orElse(new ApplicationJournal(List.of(),result.status()!=DamageSubmissionStatus.FAILED));
        io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions.evidence(new io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeInvocation.CommandEvidence(Optional.empty(),journal,
                result.status()==DamageSubmissionStatus.FAILED?Optional.of(new CommandRejection(CommandRejection.Code.INCOMPLETE_APPLICATION,"Damage application incomplete")):Optional.empty()));
        return result;
    }
    public HealingService healing() { return world.healing(); }
    public io.github.fatalitaya.combatnexus.combat.api.scheduling.CombatScheduler scheduler(){return new io.github.fatalitaya.combatnexus.combat.api.scheduling.CombatScheduler(){
        public List<io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledTask> snapshot(UUID target){var entity=world.level().getEntity(target);return entity instanceof io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules nativeTarget?nativeTarget.combatnexus$schedules().snapshot():List.of();}
        public CommandResult<io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledTask> schedule(UUID request,UUID target,io.github.fatalitaya.combatnexus.combat.api.scheduling.ScheduledTask task){
            return StateCommands.submit(world,request,target,CombatOperation.STATE,Optional.of(task.source()),null,()->{
                for(var operation : io.github.fatalitaya.combatnexus.combat.internal.registration.IntentTypes.type(task.intent()).permissions()) io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions.require(operation);
                if(!CombatExecutionScope.currentLineage(world.serverSession()).orElseThrow().permissions().containsAll(io.github.fatalitaya.combatnexus.combat.internal.registration.IntentTypes.type(task.intent()).permissions()))return CommandResult.rejected(CommandRejection.Code.INVALID_PHASE,"Scheduled operation exceeds parent permissions");
                ((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)world.level().getEntity(target)).combatnexus$schedules().add(task);
                return CommandResult.applied(task).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,task.source().id(),0,1,1,true)),true));
            });
        }
        public CommandResult<Boolean> cancel(UUID request,UUID target,UUID task){return StateCommands.submit(world,request,target,CombatOperation.STATE,null,()->CommandResult.applied(((io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules)world.level().getEntity(target)).combatnexus$schedules().remove(task)));}
    };}
    public io.github.fatalitaya.combatnexus.combat.state.api.RuntimeStates states() { return new io.github.fatalitaya.combatnexus.combat.state.api.RuntimeStates() {
        public <T> Optional<T> read(io.github.fatalitaya.combatnexus.combat.state.api.StateAddress<T> address) {
            var entity=world.level().getEntity(address.target());return entity instanceof io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates state?state.combatnexus$states().read(address):Optional.empty();
        }
        public <T> CommandResult<T> write(UUID request,io.github.fatalitaya.combatnexus.combat.state.api.StateAddress<T> address,CombatSource source,T value) {
            return StateCommands.submit(world,request,address.target(),CombatOperation.STATE,Optional.of(source),null,()->{
                var state=((io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates)world.level().getEntity(address.target())).combatnexus$states();
                boolean existed=state.read(address).isPresent();var detached=state.write(address,value);
                return CommandResult.applied(detached).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,address.key().id(),existed?1:0,1,1,true)),true));
            });
        }
        public CommandResult<Boolean> remove(UUID request,io.github.fatalitaya.combatnexus.combat.state.api.StateAddress<?> address,CombatSource source) {
            return StateCommands.submit(world,request,address.target(),CombatOperation.STATE,Optional.of(source),null,()->{
                boolean removed=((io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates)world.level().getEntity(address.target())).combatnexus$states().remove(address);
                return CommandResult.applied(removed).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(ApplicationJournal.Resource.STATE,address.key().id(),removed?1:0,0,removed?1:0,true)),true));
            });
        }
    }; }
    public io.github.fatalitaya.combatnexus.combat.action.api.EntityCommands entities() { return new io.github.fatalitaya.combatnexus.combat.action.api.EntityCommands() {
        public CommandResult<Boolean> submit(io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand command) { return submit(command,null); }
        public CommandResult<Boolean> submit(io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand command,CombatParent parent) {
            return StateCommands.submit(world,command.requestId(),command.target(),command instanceof io.github.fatalitaya.combatnexus.combat.action.api.EntityCommand.Ignite?CombatOperation.IGNITION:CombatOperation.MOVEMENT,
                    Optional.of(command.source()),parent,()->io.github.fatalitaya.combatnexus.combat.action.internal.EntityApplication.apply(world.motion(),command));
        }
    }; }
    public io.github.fatalitaya.combatnexus.combat.api.bridge.BridgeCalls bridges() { return world.bridges(); }
    public io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommands cooldowns() { return new io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommands() {
        public Optional<io.github.fatalitaya.combatnexus.combat.state.api.CooldownStamp> snapshot(UUID target, NexusId source) {
            var entity=world.level().getEntity(target);
            return entity instanceof io.github.fatalitaya.combatnexus.combat.state.spi.NativeCooldownTarget nativeTarget
                    ? Optional.ofNullable(nativeTarget.combatnexus$cooldowns().get(source)) : Optional.empty();
        }
        public long remaining(UUID target, NexusId source, int duration) {
            if(duration<0) throw new IllegalArgumentException("Negative cooldown duration");
            return snapshot(target, source).map(stamp -> stamp.remaining(world.businessTick(target, source), duration)).orElse(0L);
        }
        public CommandResult<Boolean> submit(io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommand command) { return submit(command, null); }
        public CommandResult<Boolean> submit(io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommand command, CombatParent parent) {
            return StateCommands.submit(world, command.requestId(), command.target(), CombatOperation.STATE, Optional.of(command.source()), parent, () -> {
                var entries=((io.github.fatalitaya.combatnexus.combat.state.spi.NativeCooldownTarget)world.level().getEntity(command.target())).combatnexus$cooldowns();
                var previous=entries.get(command.source().id());
                if(command.operation()==io.github.fatalitaya.combatnexus.combat.state.api.CooldownCommand.Operation.START) {
                    if(previous==null && entries.size()>=64) return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Cooldown source capacity");
                    entries.put(command.source().id(),new io.github.fatalitaya.combatnexus.combat.state.api.CooldownStamp(world.businessTick(command.target(),command.source().id()),command.deathPolicy()));
                } else entries.remove(command.source().id());
                return CommandResult.applied(true).withJournal(new ApplicationJournal(List.of(ApplicationJournal.Entry.write(
                        ApplicationJournal.Resource.COOLDOWN,command.source().id(),previous==null?0:1,entries.containsKey(command.source().id())?1:0,1,true)),true));
            });
        }
    }; }
    public io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileCommands projectiles() { return new io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileCommands() {
        public CommandResult<BeforeApplicationPlan.Result> execute(UUID request,UUID shooter,net.minecraft.world.entity.projectile.Projectile projectile,UUID action,BeforeApplicationPlan plan) {
            return StateCommands.submit(world,request,shooter,CombatOperation.PROJECTILE,null,()->{
                var launch=snapshot(projectile);
                if(launch.isEmpty() || projectile.isRemoved() || !launch.get().action().equals(action) || !launch.get().shooter().entity().equals(shooter) || launch.get().shooter().gameTick()!=world.gameTick())
                    return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT,"Launch plan identity/timing mismatch");
                var result=BeforeApplicationExecutor.execute(world,plan);
                return CommandResult.applied(result).withJournal(new ApplicationJournal(result.steps().stream().flatMap(step->step.journal().entries().stream()).toList(),result.completed()));
            });
        }
        public Optional<io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch> snapshot(net.minecraft.world.entity.projectile.Projectile projectile) {
            if (projectile.level() != world.level()) return Optional.empty();
            return ((io.github.fatalitaya.combatnexus.combat.projectile.spi.NativeProjectile)projectile).combatnexus$launch();
        }
        public CommandResult<io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch> capture(UUID request, UUID shooter, net.minecraft.world.entity.projectile.Projectile projectile) {
            return StateCommands.submit(world, request, shooter, CombatOperation.PROJECTILE, null, () -> {
                if (projectile.level() != world.level() || projectile.isRemoved() || projectile.getOwner() == null || !projectile.getOwner().getUUID().equals(shooter))
                    return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Projectile owner/world mismatch");
                var nativeProjectile = (io.github.fatalitaya.combatnexus.combat.projectile.spi.NativeProjectile)projectile;
                if (nativeProjectile.combatnexus$launch().isPresent()) {
                    io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectilePersistence.joined(projectile);
                    return CommandResult.applied(nativeProjectile.combatnexus$launch().orElseThrow());
                }
                var captured = world.participantCapture(shooter, world.definitions().generation()).orElseThrow();
                var input = captured.frame();
                var weapon = captured.nativeFacts().weapon();
                if(projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow&&arrow.getWeaponItem()!=null&&!arrow.getWeaponItem().isEmpty()){
                    var nativeWeapon=arrow.getWeaponItem();
                    if(projectile.getOwner() instanceof net.minecraft.world.entity.LivingEntity living){
                        var preferred=living.isUsingItem()&&living.getUsedItemHand()==net.minecraft.world.InteractionHand.OFF_HAND?net.minecraft.world.entity.EquipmentSlot.OFFHAND:net.minecraft.world.entity.EquipmentSlot.MAINHAND;
                        for(var slot:java.util.List.of(preferred,preferred==net.minecraft.world.entity.EquipmentSlot.MAINHAND?net.minecraft.world.entity.EquipmentSlot.OFFHAND:net.minecraft.world.entity.EquipmentSlot.MAINHAND)){
                            var held=living.getItemBySlot(slot);
                            if(held.getItem()==nativeWeapon.getItem()&&io.github.fatalitaya.combatnexus.combat.api.item.CombatItems.patch(held).equals(io.github.fatalitaya.combatnexus.combat.api.item.CombatItems.patch(nativeWeapon))){weapon=captured.nativeFacts().equipment().get(io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition.valueOf(slot.name()));break;}
                        }
                    }
                    var damageStack=nativeWeapon.copy();damageStack.remove(io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents.PATCH.get());damageStack.remove(io.github.fatalitaya.combatnexus.combat.platform.CombatDataComponents.BINDINGS.get());
                    boolean sameItem=weapon.itemId().equals(io.github.fatalitaya.combatnexus.combat.spi.ContentAccess.get().itemId(nativeWeapon.getItem()));
                    weapon=new io.github.fatalitaya.combatnexus.combat.api.input.CapturedItem(damageStack,io.github.fatalitaya.combatnexus.combat.api.item.CombatItemBindingSpec.EMPTY,sameItem?weapon.entries():Optional.of(java.util.List.of()),sameItem?weapon.affixes():Optional.of(java.util.List.of()));
                }
                var launch = new io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch(currentAction(shooter).map(io.github.fatalitaya.combatnexus.combat.api.action.CombatAction::id).orElse(projectile.getUUID()), projectile.getUUID(), captured,weapon);
                world.decisionSamples().retain(launch.action(),io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectilePersistence.owner(projectile));
                nativeProjectile.combatnexus$captureLaunch(launch);
                io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.moment(world.level(),new io.github.fatalitaya.combatnexus.combat.api.action.CombatMoment.Launched(launch));
                return CommandResult.applied(launch);
            });
        }
        public CommandResult<Double> modify(io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileChange command) { return modify(command, null); }
        public CommandResult<Double> modify(io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileChange command, CombatParent parent) {
            return StateCommands.submit(world, command.requestId(), command.target(), CombatOperation.PROJECTILE, Optional.of(command.source()), parent,
                    () -> io.github.fatalitaya.combatnexus.combat.projectile.internal.ProjectileApplication.modify(world.projectileDamage(), command));
        }
    }; }
    public Optional<BeforeApplicationPlan.Result> beforeApplication(UUID invocation, UUID target, BeforeApplicationPlan plan) {
        world.level();
        CombatExecutionScope.requireSubmissionPhase();
        if (!CombatExecutionScope.isCurrentPhase(CombatExecutionScope.Phase.APPLICATION)
                || !io.github.fatalitaya.combatnexus.combat.spi.NativeBeforeApplicationAccess.available(target)) return Optional.empty();
        var kernel = world.serverRuntime();
        if (!kernel.claim(CombatOperation.DAMAGE, invocation)) throw new IllegalStateException("Native plan already submitted");
        var result = io.github.fatalitaya.combatnexus.combat.internal.transaction.BeforeApplicationExecutor.execute(world, plan);
        io.github.fatalitaya.combatnexus.combat.spi.NativeBeforeApplicationAccess.record(target, result);
        return Optional.of(result);
    }
    public io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommands absorption() { return new io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommands() {
        public List<io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionSource> snapshot(UUID target) {
            return world.absorptionAccess().find(target).map(value -> io.github.fatalitaya.combatnexus.combat.absorption.internal.NativeAbsorptionState.ledger(value).snapshot()).orElse(List.of());
        }
        public CommandResult<io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionSource> submit(io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand command) { return submit(command, null); }
        public CommandResult<io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionSource> submit(io.github.fatalitaya.combatnexus.combat.absorption.api.AbsorptionCommand command, CombatParent parent) {
            return StateCommands.submit(world, command.requestId(), command.target(), CombatOperation.ABSORPTION, Optional.of(command.source()), parent,
                    () -> io.github.fatalitaya.combatnexus.combat.absorption.internal.NativeAbsorptionApplication.apply(world.absorptionAccess().find(command.target()).orElseThrow(), command));
        }
    }; }
    public io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaResult damageFormula(io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaRequest request) {
        var target = world.level().getEntity(request.target());
        if (!(target instanceof LivingEntity living)) throw new IllegalArgumentException("Formula target unavailable");
        var kernel = world.serverRuntime();
        if (!kernel.claim(CombatOperation.DAMAGE, request.invocation())) throw new IllegalStateException("Formula invocation already consumed");
        var policy = io.github.fatalitaya.combatnexus.combat.internal.registration.CombatExtensions.damageFormula(request.policy());
        long generation = definitions().generation();
        var targetFrame = world.captureInput(living, generation);
        var attacker = request.launch().map(io.github.fatalitaya.combatnexus.combat.projectile.api.ProjectileLaunch::shooter)
                .or(() -> request.attacker().flatMap(id -> id.equals(request.target()) ? Optional.of(targetFrame) : facts(id)));
        var input = new io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy.Input(request.amount(), targetFrame, attacker, request.action());
        var samples = new ArrayList<Float>();
        var active = new boolean[]{true};
        io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy.Samples sampler = new io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy.Samples() {
            private void check(){if(!active[0]||!world.level().getServer().isSameThread())throw new IllegalStateException("Formula sampler expired");if(samples.size()>=256)throw new IllegalStateException("Formula sample budget");}
            public float nextFloat(){check();float sample=living.getRandom().nextFloat();samples.add(sample);return sample;}
            public float shared(NexusId decision,Sharing sharing){check();float sample=world.decisionSamples().sample(request.action().orElseThrow(),request.policy(),decision,sharing==Sharing.TARGET?Optional.of(request.target()):Optional.empty(),()->living.getRandom().nextFloat());samples.add(sample);return sample;}
        };
        try (var read = CombatReadScope.open()) {
            return io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions.decision(new io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaResult(request, policy.calculate(input, sampler), samples, generation, true));
        } catch (RuntimeException failure) {
            return io.github.fatalitaya.combatnexus.combat.internal.coordination.BridgeExecutions.decision(new io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaResult(request,
                    new io.github.fatalitaya.combatnexus.combat.api.bridge.DamageFormulaPolicy.Output(request.amount(), List.of()), samples, generation, false));
        } finally { active[0] = false; }
    }
    public io.github.fatalitaya.combatnexus.combat.health.api.HealthCommands health() { return new io.github.fatalitaya.combatnexus.combat.health.api.HealthCommands() {
        public CommandResult<Float> correct(io.github.fatalitaya.combatnexus.combat.health.api.HealthCorrection command) { return correct(command, null); }
        public CommandResult<Float> correct(io.github.fatalitaya.combatnexus.combat.health.api.HealthCorrection command, CombatParent parent) {
            return StateCommands.submit(world, command.requestId(), command.target(), CombatOperation.HEALTH, Optional.of(command.source()), parent,
                    () -> io.github.fatalitaya.combatnexus.combat.health.internal.NativeHealthApplication.correct(world.healthAccess().target(command.target()), command));
        }
        public CommandResult<io.github.fatalitaya.combatnexus.combat.health.api.LethalReceipt> execute(io.github.fatalitaya.combatnexus.combat.health.api.LethalAttempt command) { return execute(command, null); }
        public CommandResult<io.github.fatalitaya.combatnexus.combat.health.api.LethalReceipt> execute(io.github.fatalitaya.combatnexus.combat.health.api.LethalAttempt command, CombatParent parent) {
            return StateCommands.submit(world, command.requestId(), command.target(), CombatOperation.LETHAL, Optional.of(command.source()), parent, () -> {
                var target = (LivingEntity)world.level().getEntity(command.target());
                var lethal = world.healthAccess().lethal(command);
                if (lethal.isEmpty()) return CommandResult.rejected(CommandRejection.Code.NOT_FOUND, "Unknown lethal damage type");
                var captured = world.captureInput(target, definitions().generation());
                var participants = new LinkedHashMap<UUID, io.github.fatalitaya.combatnexus.combat.api.input.CombatInputFrame>();
                participants.put(command.target(), captured);
                command.attacker().filter(id -> !id.equals(command.target())).map(world.level()::getEntity)
                    .filter(LivingEntity.class::isInstance).map(LivingEntity.class::cast)
                    .ifPresent(attacker -> participants.put(attacker.getUUID(), world.captureInput(attacker, definitions().generation())));
                var managed = command.bypassManagedProtection()
                        ? new io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.ProtectionDecision(0, Optional.empty())
                        : io.github.fatalitaya.combatnexus.combat.spi.CombatBehaviorAccess.protect(world, command.target(), captured.behaviors(), target.getHealth(), 0, new io.github.fatalitaya.combatnexus.combat.api.behavior.ReactionInputs(participants,Optional.of(command.damageType()),lethal.orElseThrow().damageTags()), Optional.of(command.source()));
                return io.github.fatalitaya.combatnexus.combat.health.internal.NativeHealthApplication.execute(lethal.orElseThrow(), command, managed);
            });
        }
    }; }
    public SourceService sources() { return world.sourceInstallations().businessView(); }
    public EffectCommands effects() { return new EffectCommands() {
        public EffectResult fact(UUID request, EffectFact fact) {
            var result = StateCommands.submit(world, request, fact.effect().snapshot().target(), CombatOperation.EFFECT, null, () -> {
                boolean satisfied;
                try (var computation = io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatExecutionScope.computation()) {
                    satisfied = io.github.fatalitaya.combatnexus.effect.internal.EffectMappings.satisfied(fact);
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
                    () -> io.github.fatalitaya.combatnexus.effect.internal.NativeEffects.apply((LivingEntity) world.level().getEntity(command.target()), command));
        }
        public List<EffectView> snapshot(UUID target) {
            return facts(target).map(f -> f.facts().effectViews()).orElse(List.of());
        }
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
                if (request.effect().ownership() != io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED)
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
                if (effect.ownership() == io.github.fatalitaya.combatnexus.effect.api.EffectOwnership.MANAGED)
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
    }; }
    public ShieldCommands shields() { return new ShieldCommands() {
        public List<ShieldPoolSnapshot> snapshot(UUID target) { return world.shields().snapshot(target); }
        public CommandResult<ShieldGrantReceipt> grant(UUID request, UUID target, ShieldGrant grant) { return grant(request, target, grant, null); }
        public CommandResult<ShieldGrantReceipt> grant(UUID request, UUID target, ShieldGrant grant, CombatParent parent) {
            return StateCommands.submit(world, request, target, CombatOperation.SHIELD, parent, () -> CommandResult.applied(world.shields().replace(target, grant)));
        }
        public CommandResult<Boolean> remove(UUID request, UUID target, NexusId source) {
            return StateCommands.submit(world, request, target, CombatOperation.SHIELD, null, () -> { world.shields().remove(target, source, ShieldPoolSnapshot.RemovalReason.REMOVED); return CommandResult.applied(true); });
        }
    }; }
    public RegenerationCommands regeneration() { return new RegenerationCommands() {
        public List<RegenerationPlan> snapshot(UUID target) { return world.regenerationPlans(target); }
        public CommandResult<Boolean> replace(UUID request, RegenerationPlan plan) { return replace(request, plan, null); }
        public CommandResult<Boolean> replace(UUID request, RegenerationPlan plan, CombatParent parent) {
            return StateCommands.submit(world, request, plan.target(), CombatOperation.REGENERATION, parent, () -> { world.regeneration().replace(plan); return CommandResult.applied(true); });
        }
        public CommandResult<Boolean> remove(UUID request, UUID target, NexusId source) {
            return StateCommands.submit(world, request, target, CombatOperation.REGENERATION, null, () -> { world.regeneration().remove(target, source); return CommandResult.applied(true); });
        }
    }; }
}
