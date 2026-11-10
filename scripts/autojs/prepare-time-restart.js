(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess"),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Set=Java.loadClass("java.util.Set"),List=Java.loadClass("java.util.List");Life.beginTick(world);
 for(var old of level.getAllEntities())if(old.entityTags().contains("combatnexus_time_restart"))old.discard();
 var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoGravity(true);e.setNoAi(true);e.addTag("combatnexus_time_restart");level.addFreshEntity(e);cls("combat.platform.CombatPersistence").tick(level);
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),source=cls("api.id.NexusId").parse("probe:time_restart"),session=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,e.getUUID(),Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty());
 world.sourcePersistence(e.getUUID(),source,cls("combat.internal.CombatRuntime$SourcePersistence").PERSISTENT);
 var plan=new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (cls("combat.api.behavior.BuiltinBehaviors$PeriodicHealing"))(1,5))),ES=cls("effect.api.EffectService"),def=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").timed(10000),ES.Stacking.REPLACE,cls("effect.api.EffectPolicy").DEFAULT,List.of(),plan,Set.of());
 world.effects().apply(def.instantiate(e.getUUID(),source,source,api.time().now(e.getUUID(),source)),ES.Stacking.REPLACE);e.setHealth(1);
 var request=UUID.randomUUID();if(!session.advance(0,request,7).completed()||e.getHealth()!==2)throw new Error("time restart fixture did not consume one interval");
 cls("combat.platform.CombatPersistence").save(e);
 var cancelledEntity=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
 cancelledEntity.setPos(1,512,0);cancelledEntity.setNoGravity(true);cancelledEntity.setNoAi(true);cancelledEntity.addTag("combatnexus_time_restart_cancelled");level.addFreshEntity(cancelledEntity);cls("combat.platform.CombatPersistence").tick(level);
 var cancelledSession=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,cancelledEntity.getUUID(),Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty()),cancelRequest=UUID.randomUUID();
 var pending=cancelledSession.advance(0,cancelRequest,1200000);
 if(pending.terminal())throw new Error("cancellation restart fixture did not exhaust budget");
 var cancelled=cancelledSession.cancelAdvance(0,cancelRequest,1200000),cancelClock=cancelledSession.time().tick();
 cls("combat.platform.CombatPersistence").save(cancelledEntity);
 return JSON.stringify({prepared:true,target:String(e.getUUID()),clock:api.time().now(e.getUUID(),source),request:String(request),remaining:9993,cancelled:{target:String(cancelledEntity.getUUID()),request:String(cancelRequest),clock:cancelClock,consumed:cancelled.consumed(),remaining:cancelled.remaining()}});
})()
