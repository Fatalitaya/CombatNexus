// Disposable server only. An isolated Nether runtime uses a failing contribution port; no reflection.
(function () {
    Java.loadClass("io.github.fatalitaya.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    function cls(n) { return Java.loadClass("io.github.fatalitaya.combatnexus." + n); }
    var Id=Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId"), List=Java.loadClass("java.util.List");
    var level=AutoJS.server()["getLevel(net.minecraft.resources.ResourceKey)"](Java.loadClass("net.minecraft.world.level.Level").NETHER);
    var Lifecycle=cls("combat.platform.CombatWorldLifecycle"), Access=cls("combat.spi.CombatWorldLifecycleAccess");
    var world=null, equipment=null, captureAfterClose=null, Example=Java.loadClass("probe.EffectExtension");
    function effectEvents(){
        cls("combat.internal.transaction.CombatCompletionQueue").drainIfSafe();
        var events=[],all=Example.moments();
        for(var i=0;i<all.size();i++){var m=all.get(i);if(m instanceof cls("combat.api.action.CombatMoment$Effect")&&m.target().equals(entity.getUUID()))events.push(m);}
        return events;
    }
    var original=new (cls("attribute.internal.EffectAttributeContributions"))(new (cls("attribute.platform.NativeAttributeWorldAccess"))(level));
    var ES=cls("effect.api.EffectService"), checks=[], entity=null;
    function check(ok,n) { if(!ok)throw new Error(n); checks.push(n); }
    function rejects(f,n) { var failed=false;try{f();}catch(e){failed=true;}check(failed,n); }
    var Port=cls("effect.spi.EffectContributionPort"), Prepared=cls("effect.spi.EffectContributionPort$Prepared");
    var Receipt=cls("effect.spi.EffectContributionPort$Receipt"), Failure=Java.loadClass("java.lang.IllegalStateException");
    var failApply=true, failCompensation=false, immune=false, attempts=0;
    var port=new Port({prepare:function(change) {
        var nativePrepared=original.prepare(change), installation=!change.after().isEmpty();
        return new Prepared({apply:function() {
            var receipt=nativePrepared.apply();
            if(installation) { attempts++; if(failApply)return new Receipt(receipt.writes(),false,new Failure("ownership probe")); }
            return receipt;
        },reconcile:function() {
            if(failCompensation)return new Receipt(List.of(),false,new Failure("blocked compensation"));
            return nativePrepared.reconcile();
        }});
    }});
    var kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server());
    var retainedWorld=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level), oldSession=retainedWorld.worldSession();
    var oldLineage=cls("combat.api.transaction.CombatLineage").root(cls("combat.api.transaction.CombatOperation").HEALING);
    check(kernel.admit(oldLineage).admitted(),"unload probe admitted");
    var oldTicket=kernel.ticket(oldLineage);oldTicket.bindWorld(oldSession);
    Lifecycle.unload(new (Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Unload"))(level));
    rejects(function(){oldTicket.requireActive();},"world unload expires existing ticket");
    rejects(function(){retainedWorld.effects();},"world unload invalidates retained runtime");
    check(kernel.world(oldSession).isEmpty(),"world lookup cannot revive unloaded runtime");
    kernel.finish(oldLineage);
    try {
        var Conditions=cls("combat.regeneration.spi.RegenerationConditions"), State=cls("combat.regeneration.spi.RegenerationConditions$State");
        level.combatnexus$bindWorldSession(new (cls("combat.api.transaction.WorldSession"))(
            cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()).session(),
            cls("platform.NativeIds").nexus(level.dimension().identifier())));
        world=new (cls("combat.internal.CombatRuntime"))(kernel,level.combatnexus$worldSession(),new (Java.loadClass("java.util.function.LongSupplier"))({getAsLong:function(){return level.getGameTime();}}),level,cls("combat.internal.platform.PlatformWorldLifecycle").healingApplication(new (cls("combat.platform.WorldHealingPort"))(level)),
            new Conditions({read:function(){return new State(true,true);}}),
            function(){return immune;},port,new (cls("combat.platform.WorldEquipmentObservation"))(level),new (cls("combat.platform.WorldEntityMotionAccess"))(level),new (cls("combat.platform.WorldAbsorptionAccess"))(level),new (cls("combat.platform.WorldHealthAccess"))(level),new (cls("combat.projectile.platform.WorldProjectileDamageAccess"))(level),new (cls("combat.platform.WorldDamageTypes"))(level),new (cls("combat.platform.WorldParticipantStates"))(level));
        check(world.worldSession()!==oldSession,"reloaded dimension receives new session identity");
        check(kernel.world(oldSession).isEmpty(),"new runtime cannot revive old world identity");
        Access.beginTick(world);
        equipment=new (cls("combat.internal.coordination.EquipmentBehaviorController"))(world,new (cls("combat.platform.WorldEquipmentObservation"))(level));
        entity=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        level.getChunk(0,0); entity.setPos(0,100,0); check(level.addFreshEntity(entity),"probe entity added");
        check(level.getEntity(entity.getUUID())!==null,"probe entity visible to world services");
        var target=entity.getUUID(), Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
        var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"), Bindings=cls("combat.api.item.CombatItemBindingSpec");
        var item=new Stack(Java.loadClass("net.minecraft.world.item.Items").STICK);
        item.set(cls("combat.platform.CombatDataComponents").PATCH.get(),cls("combat.api.item.CombatItemBindingPatch").replaceAll(new (cls("combat.api.item.CombatItemBindingBuilder"))().entry(Id.parse("combatnexus:ownership"),new (cls("combat.api.definition.EntryTemplateSpec$Builder"))(Id.parse("combatnexus:ownership")).effect(Id.parse("combatnexus:effect"),Id.parse("combatnexus:agility")).build().definition(),cls("api.equipment.EquipmentPosition").MAINHAND).build()));
        // The controller retries against the actual slot, rather than the failed event's stale stack.
        entity.setItemSlot(Slot.MAINHAND,item);

        equipment.changed(target,cls("platform.NativeEquipment").position(Slot.MAINHAND),cls("combat.platform.WorldEquipmentObservation").capture(AutoJS.server().overworld(),item));
        check(world.effects().snapshot(target).isEmpty(),"compensated failure never commits active effect");
        check(effectEvents().length===0,"compensated failed commit publishes no public Effect event");
        var generation=cls("combat.internal.definition.CombatDefinitions").forRuntime(Java.loadClass("io.github.fatalitaya.combatnexus.combat.internal.transaction.CombatServerRuntime").of((AutoJS.server()))).generation();
        failApply=false; equipment.tick(generation);
        check(world.effects().snapshot(target).size()===1&&attempts===2,"unchanged equipment retries after compensation");
        check(effectEvents().length===1,"successful retry publishes one public Effect event");
        var grantOrigin=world.effects().snapshot(target).get(0).display().provenance();
        check(grantOrigin.entity().equals(target)&&String(grantOrigin.item())==="minecraft:stick"&&grantOrigin.slot()===cls("api.equipment.EquipmentPosition").MAINHAND,"equipment grant records actual holder item and slot independently of stacking key");
        var source=world.effects().snapshot(target).get(0).snapshot().source();
        world.effects().remove(target,Id.parse("combatnexus:agility"),source,ES.Removal.DISPELLED);
        equipment.changed(target,cls("platform.NativeEquipment").position(Slot.MAINHAND),cls("combat.platform.WorldEquipmentObservation").capture(AutoJS.server().overworld(),item.copy())); equipment.tick(generation);
        check(world.effects().snapshot(target).isEmpty()&&attempts===2,"successful grant is not replenished after dispel");
        check(effectEvents().length===2&&String(effectEvents()[1].managedChange().get().removal().get())==="DISPELLED","direct service removal publishes one committed reason");
        equipment.changed(target,cls("platform.NativeEquipment").position(Slot.MAINHAND),cls("combat.platform.WorldEquipmentObservation").capture(AutoJS.server().overworld(),Stack.EMPTY));
        failApply=true; failCompensation=true;
        equipment.changed(target,cls("platform.NativeEquipment").position(Slot.MAINHAND),cls("combat.platform.WorldEquipmentObservation").capture(AutoJS.server().overworld(),item));
        check(world.sourceInstallations().snapshot(target).stream().anyMatch(function(s){return s.pending();}),"partial installation remains tracked");
        var before=attempts; equipment.tick(generation);
        check(attempts===before&&!world.effects().pendingSources(target).isEmpty(),"pending compensation blocks another installation");
        check(effectEvents().length===2,"failed compensation publishes no committed event");
        entity.setItemSlot(Slot.MAINHAND,Stack.EMPTY);
        failApply=false; failCompensation=false; equipment.tick(generation);
        check(world.effects().pendingSources(target).isEmpty()&&world.effects().snapshot(target).isEmpty(),"unequip reconciles attempted ownership without reinstalling stale equipment");

        immune=true; entity.setItemSlot(Slot.MAINHAND,item);
        equipment.changed(target,cls("platform.NativeEquipment").position(Slot.MAINHAND),cls("combat.platform.WorldEquipmentObservation").capture(AutoJS.server().overworld(),item));
        check(world.effects().snapshot(target).isEmpty(),"false apply result is not recorded as successful installation");
        immune=false; equipment.tick(generation);
        check(world.effects().snapshot(target).size()===1,"rejected grant retries when immunity ends");

        rejects(function(){world.regeneration().recordDamage(target,Java.loadClass("java.lang.Long").MAX_VALUE);},"business view cannot inject future damage time");
        rejects(function(){cls("combat.spi.CombatBehaviorAccess").recordRecoveryDamage(world,null);},"recovery SPI requires a live settlement callback");
        var now=level.getGameTime(), snapshot=world.captureAttributes(entity,List.of());
        var capture=function(){return world.panel(entity,snapshot);};
        captureAfterClose=capture;
        // Tracking mirrors replaced the request/throttle service; keep projection budgets and owner checks.
        check(typeof world.panels==="undefined","removed request facade cannot reset tracking cadence or close world");
        var panel=capture();
        check(panel!==null,"owner projection delivers authoritative snapshot");
        var Projector=cls("presentation.api.CombatPanelProjector");
        check(String(Projector.project(capture(),now,function(e){return world.timeControl().advancing(e.target(),e.source().orElse(e.definition()));}))===String(Projector.project(panel,now,function(e){return world.timeControl().advancing(e.target(),e.source().orElse(e.definition()));})),"same-tick projection is stable without advancing lifecycle");
        rejects(function(){world.panel(entity,snapshot,function(){throw new Error("projection budget");});},"projection budget cannot be bypassed");
        check(String(Projector.project(capture(),now,function(e){return world.timeControl().advancing(e.target(),e.source().orElse(e.definition()));}))===String(Projector.project(panel,now,function(e){return world.timeControl().advancing(e.target(),e.source().orElse(e.definition()));})),"failed projection budget preserves authoritative state");
    } finally {
        failApply=false; failCompensation=false;
        try {
            if(entity!==null) { Access.clear(world,entity.getUUID(),ES.Removal.WORLD_EXIT);entity.discard(); }
            if(equipment!==null)equipment.close();
            if(world!==null)Access.close(world);
        } finally {level.combatnexus$bindWorldSession(null);Lifecycle.load(new (Java.loadClass("net.neoforged.neoforge.event.level.LevelEvent$Load"))(level));}
    }
    rejects(function(){captureAfterClose();},"retained projection rejects after owner closes");
    return JSON.stringify({passed:checks.length,checks:checks});
})()
