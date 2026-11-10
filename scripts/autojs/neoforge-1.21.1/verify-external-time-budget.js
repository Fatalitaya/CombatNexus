(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess"),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Set=Java.loadClass("java.util.Set");Life.beginTick(world);
 var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level);e.setPos(0,512,0);e.setNoGravity(true);e.setNoAi(true);e.addTag("combatnexus_time_budget_probe");level.addFreshEntity(e);
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),session=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,e.getUUID(),Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty());
 var source=cls("api.id.NexusId").parse("probe:budget"),List=Java.loadClass("java.util.List"),ES=cls("effect.api.EffectService");
 world.sourcePersistence(e.getUUID(),source,cls("combat.internal.CombatRuntime$SourcePersistence").PERSISTENT);
 e.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MAX_HEALTH).setBaseValue(10000);e.setHealth(1);
 var plan=new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (cls("combat.api.behavior.BuiltinBehaviors$PeriodicHealing"))(0.25,1))),definition=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").timed(100000),ES.Stacking.REPLACE,cls("effect.api.EffectPolicy").DEFAULT,List.of(),plan,Set.of());
 world.effects().apply(definition.instantiate(e.getUUID(),source,source,api.time().now(e.getUUID(),source)),ES.Stacking.REPLACE);
 var request=UUID.randomUUID(),ticks=Math.floor(world.serverRuntime().limits().maximumPerTick()/2)+25,start=api.time().now(e.getUUID(),cls("api.id.NexusId").parse("probe:budget")),r=session.advance(0,request,ticks);
 if(r.completed()||r.consumed()>=ticks||r.remaining()<=0)throw new Error("advance skipped budget interruption");
 if(String(session.release())!=="PENDING_TIME"||session.released()||!session.time().latest().get().equals(r))throw new Error("release discarded pending increment");
 e.getPersistentData().putString("time_request",String(request));e.getPersistentData().putLong("time_ticks",ticks);e.getPersistentData().putLong("time_start",start);
 var saved=Life.save(world,e.getUUID());Life.clear(world,e.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);Life.restore(world,e.getUUID(),saved);
 if(api.drivers().snapshot(e.getUUID()).get(0).bound())throw new Error("pending owner rebound automatically");
 return JSON.stringify({passed:3,checks:["release preserves pending progress and ownership","budget leaves a truthful pending increment","pending increment survives save and restore without live handle"]});
})()
