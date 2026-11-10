package io.github.fatalitaya.combatnexus.combat.time.internal;

import static io.github.fatalitaya.combatnexus.combat.time.internal.BusinessTimeState.*;

import io.github.fatalitaya.combatnexus.api.id.NexusId;
import io.github.fatalitaya.combatnexus.combat.api.transaction.*;
import io.github.fatalitaya.combatnexus.combat.internal.CombatRuntime;
import io.github.fatalitaya.combatnexus.combat.internal.transaction.*;
import io.github.fatalitaya.combatnexus.combat.time.api.*;
import java.util.*;

/** Internal time owner. Driver session authentication is performed by the driver service. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class WorldBusinessTime implements BusinessTime {
    private final BusinessTimeState storage=new BusinessTimeState();
    private final CombatRuntime world;
    private UUID processingTarget;
    private NexusId processingSource;
    private boolean processing, deferred;
    public WorldBusinessTime(CombatRuntime world) { this.world=Objects.requireNonNull(world); }
    private EntityTime existing(UUID target) {
        world.requireActive(); Objects.requireNonNull(target);
        var value=storage.entities.get(target);
        return value!=null && world.participantIdentity(target).filter(value.identity::equals).isPresent() ? value : null;
    }
    private EntityTime state(UUID target) {
        var old=existing(target); if(old!=null)return old;
        var identity=world.participantIdentity(target).orElseThrow(()->new IllegalArgumentException("Time target unavailable"));
        return storage.create(identity,world.gameTick(),world.level().getEntity(target).tickCount);
    }
    private long entityNow(EntityTime state) { return state.entity.now(world.gameTick()); }
    private NexusId clockSource(UUID target,NexusId source) {
        var behaviorSource=world.behaviorControl().clockSource(target,source);
        return world.sourceInstallations().identity(target,behaviorSource)
            .map(io.github.fatalitaya.combatnexus.combat.internal.coordination.SourceInstallations::key).orElse(behaviorSource);
    }
    @Override public long now(UUID target,NexusId source) {
        Objects.requireNonNull(source); var state=existing(target);if(state==null)return world.gameTick();
        long parent=entityNow(state);var lane=state.sources.get(source);
        if(lane==null)lane=state.sources.get(clockSource(target,source));
        return lane==null?parent:lane.now(parent);
    }
    @Override public long phase(UUID target,NexusId source,long nativePhase) {
        var state=existing(target);return state==null?nativePhase:BusinessTimeState.phase(state,now(target,source));
    }
    private Lane lane(EntityTime state, Optional<NexusId> source) {
        return BusinessTimeState.lane(state,source,world.gameTick());
    }
    public void acquire(UUID target,Optional<NexusId> source,NexusId driver) {
        CombatMutationGuard.requireWorldWritePhase();Objects.requireNonNull(driver);Objects.requireNonNull(source);
        BusinessTimeState.acquire(state(target),source,driver,world.gameTick());
    }
    public void release(UUID target,Optional<NexusId> source,NexusId driver) {
        CombatMutationGuard.requireWorldWritePhase();
        BusinessTimeState.release(require(target,source,driver),source,world.gameTick());
    }
    private EntityTime require(UUID target,Optional<NexusId> source,NexusId driver) {
        var state=existing(target);var lane=state==null?null:source.isEmpty()?state.entity:state.sources.get(source.get());
        if(lane==null||!Objects.equals(lane.driver,driver)||!lane.bound)throw new IllegalStateException("Time driver is not bound");
        return state;
    }
    @Override public boolean advancing(UUID target,NexusId source) {
        var state=existing(target);NexusId resolved=state==null?source:state.sources.containsKey(source)?source:clockSource(target,source);
        var lane=state==null?null:state.sources.get(resolved);
        NexusId effective=lane!=null&&lane.driver!=null?lane.driver:state==null?null:state.entity.driver;
        if(!processing)return effective==null;
        if(!target.equals(processingTarget))return false;
        if(processingSource!=null)return processingSource.equals(resolved);
        return lane==null||lane.driver==null;
    }
    @Override public boolean mayExecute() {
        if(!processing)return true;
        var server=world.serverRuntime();boolean available=server.currentTickCount()<server.limits().maximumPerTick();
        if(!available)deferred=true;return available;
    }
    public TimeAdvanceReceipt advance(UUID target,Optional<NexusId> source,NexusId driver,long sequence,UUID request,long ticks) {
        world.requireActive();CombatExecutionScope.requireSubmissionPhase();
        if(CombatExecutionScope.isActive()||CombatDispatchScope.isActive()||processing)throw new IllegalStateException("Time advance requires an idle driver entry");
        if(sequence<0||ticks<1||ticks>1200000)throw new IllegalArgumentException("Invalid time increment");
        var state=require(target,source,driver);var lane=lane(state,source);Objects.requireNonNull(request);
        var value=BusinessTimeState.begin(state,source,driver,sequence,request,ticks);
        if(value.cancelled())return BusinessTimeState.receipt(value);
        while(value.consumed()<ticks) {
            var server=world.serverRuntime();
            if(server.currentTickCount()>=server.limits().maximumPerTick())break;
            CombatLineage lineage=null;
            if(!value.slicePending()) {
                lineage=server.requestLineage(CombatOperation.STATE,null);var admission=server.admit(lineage);
                if(!admission.admitted())break;
            }
            try {
                if(!value.slicePending()){value=BusinessTimeState.startSlice(state,lane,value);}
                processing=true;processingTarget=target;processingSource=source.orElse(null);deferred=false;
                world.advanceBusinessTime();
                if(existing(target)!=state || state.advances.get(source.orElse(null))==null || state.advances.get(source.orElse(null)).cancelled()) {
                    value=new SavedTime.Advance(sequence,request,value.source(),driver,ticks,value.consumed(),true,true);
                    break;
                }
                if(deferred)break;
                value=BusinessTimeState.completeSlice(state,value);
            } finally { processing=false;processingTarget=null;processingSource=null;if(lineage!=null)server.finish(lineage); }
        }
        return BusinessTimeState.receipt(value);
    }
    public TimeAdvanceReceipt cancel(UUID target,Optional<NexusId> source,NexusId driver,long sequence,UUID request,long ticks) {
        world.requireActive();CombatExecutionScope.requireSubmissionPhase();
        if(CombatExecutionScope.isActive()||CombatDispatchScope.isActive()||processing)throw new IllegalStateException("Time cancellation requires an idle driver entry");
        var state=require(target,source,driver);
        return BusinessTimeState.receipt(BusinessTimeState.cancel(state,source,driver,sequence,request,ticks));
    }
    public TimeStatus status(UUID target,Optional<NexusId> source,NexusId driver) {
        var state=require(target,source,driver);var lane=source.isEmpty()?state.entity:state.sources.get(source.get());
        var latest=Optional.ofNullable(state.advances.get(source.orElse(null)));
        return new TimeStatus(lane.now(source.isEmpty()?world.gameTick():entityNow(state)),
            latest.map(value->Math.addExact(value.sequence(),1)).orElse(0L),latest.map(BusinessTimeState::receipt));
    }
    public SavedTime save(UUID target) {
        var state=existing(target);if(state==null)return new SavedTime(world.gameTick(),world.level().getEntity(target).tickCount-world.gameTick(),null,List.of(),List.of());
        return BusinessTimeState.save(state,world.gameTick());
    }
    public UUID processingTarget() {
        if(!processing)throw new IllegalStateException("No active time slice");
        return processingTarget;
    }
    public void restore(UUID target,SavedTime saved) {
        CombatMutationGuard.requireWorldWritePhase();var identity=world.participantIdentity(target).orElseThrow();
        storage.entities.put(target,BusinessTimeState.restore(identity,saved,world.gameTick()));
    }
    public void sourceRemoved(UUID target,NexusId source) {
        var state=existing(target);if(state==null)return;
        state.sources.remove(source);
        state.advances.remove(source);
    }
    public void clear(UUID target) { storage.entities.remove(target); }
    public void close() { storage.entities.clear(); }
}
