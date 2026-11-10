package io.github.fatalitaya.combatnexus.combat.health.platform.mixin;
import net.minecraft.nbt.CompoundTag;
import java.util.Optional;
import io.github.fatalitaya.combatnexus.combat.state.api.StateKey;
import io.github.fatalitaya.combatnexus.combat.state.internal.RuntimeStateStore;
import io.github.fatalitaya.combatnexus.combat.state.spi.NativeRuntimeStates;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class LivingEntityRuntimeStateMixin implements NativeRuntimeStates,io.github.fatalitaya.combatnexus.combat.state.spi.NativeSchedules {
    @Shadow protected boolean dead;
    @Unique private RuntimeStateStore combatnexus$states;
    @Unique private String combatnexus$invalidStates;
    @Unique private io.github.fatalitaya.combatnexus.combat.state.internal.EntityScheduleStore combatnexus$schedules;
    @Unique private String combatnexus$invalidSchedules;
    public io.github.fatalitaya.combatnexus.combat.state.internal.EntityScheduleStore combatnexus$schedules(){
        if(combatnexus$invalidSchedules!=null)throw new IllegalStateException("Schedule restoration failed");
        if(combatnexus$schedules==null)combatnexus$schedules=new io.github.fatalitaya.combatnexus.combat.state.internal.EntityScheduleStore();return combatnexus$schedules;
    }
    @Inject(method="tick",at=@At("TAIL"),require=1)
    private void combatnexus$scheduledTick(CallbackInfo ci){
        if(combatnexus$schedules!=null&&combatnexus$invalidSchedules==null)io.github.fatalitaya.combatnexus.combat.spi.CombatScheduleAccess.tick((LivingEntity)(Object)this);
    }
    public RuntimeStateStore combatnexus$states() {
        if(combatnexus$invalidStates!=null)throw new IllegalStateException("Runtime state restoration failed");
        if(combatnexus$states==null)combatnexus$states=new RuntimeStateStore();return combatnexus$states;
    }
    @Inject(method="die",at=@At("TAIL"),require=1)
    private void combatnexus$stateDeath(net.minecraft.world.damagesource.DamageSource source,CallbackInfo ci) {
        if(dead&&combatnexus$states!=null&&combatnexus$invalidStates==null)combatnexus$states.clear(StateKey.Boundary.DEATH,Optional.empty());
        if(dead&&combatnexus$schedules!=null&&combatnexus$invalidSchedules==null)combatnexus$schedules.clear(StateKey.Boundary.DEATH,Optional.empty());
    }
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"),require=1)
    private void combatnexus$saveStates(CompoundTag output,CallbackInfo ci) {
        if(combatnexus$invalidStates!=null)output.putString("combatnexus_runtime_states",combatnexus$invalidStates);
        else if(combatnexus$states!=null)output.putString("combatnexus_runtime_states",combatnexus$states.encode(true));
        if(combatnexus$invalidSchedules!=null)output.putString("combatnexus_schedules",combatnexus$invalidSchedules);
        else if(combatnexus$schedules!=null)output.putString("combatnexus_schedules",combatnexus$schedules.encode(true));
    }
    @Inject(method="readAdditionalSaveData",at=@At("HEAD"),require=1)
    private void combatnexus$loadStates(CompoundTag input,CallbackInfo ci) {
        combatnexus$states=new RuntimeStateStore();combatnexus$invalidStates=null;
        String encoded=(input.contains("combatnexus_runtime_states", 8) ? input.getString("combatnexus_runtime_states") : "[]");
        try { combatnexus$states.restore(encoded); }
        catch(RuntimeException failure) { combatnexus$invalidStates=encoded; }
        combatnexus$schedules=new io.github.fatalitaya.combatnexus.combat.state.internal.EntityScheduleStore();combatnexus$invalidSchedules=null;
        String scheduled=(input.contains("combatnexus_schedules", 8) ? input.getString("combatnexus_schedules") : "[]");
        try {combatnexus$schedules.restore(scheduled);}
        catch(RuntimeException failure){combatnexus$invalidSchedules=scheduled;}
    }
}
