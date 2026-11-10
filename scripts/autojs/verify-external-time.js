// Real registered external time driver; isolated development server only.
(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),List=Java.loadClass("java.util.List"),Set=Java.loadClass("java.util.Set"),Map=Java.loadClass("java.util.Map"),Id=cls("api.id.NexusId");
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess"),ES=cls("effect.api.EffectService");Life.beginTick(world);
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),driver=Java.loadClass("probe.DriverExtension").timeDriver,Scope=cls("combat.driver.api.DriverScope"),checks=[],entities=[],sessions=[];
 function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 function rejects(f){try{f();return false;}catch(e){return true;}}
 function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoGravity(true);e.setNoAi(true);level.addFreshEntity(e);entities.push(e);return e;}
 function bind(e,s){var v=api.drivers().bind(driver,e.getUUID(),Set.of(Scope.TIME),s);sessions.push(v);return v;}
 function effect(e,id,ticks,interval){var plan=new (cls("combat.api.behavior.BehaviorPlan"))(0,interval===0?List.of():List.of(new (cls("combat.api.behavior.BuiltinBehaviors$PeriodicHealing"))(1,interval)));
 var def=new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").timed(ticks),ES.Stacking.REPLACE,cls("effect.api.EffectPolicy").DEFAULT,List.of(),plan,Set.of());
 check(world.effects().apply(def.instantiate(e.getUUID(),id,id,api.time().now(e.getUUID(),id)),ES.Stacking.REPLACE),"install "+id);}
 function jump(delta){level.getLevelData().setGameTime(level.getGameTime()+delta);Life.beginTick(world);Life.tickRecovery(world);}
 try{
  var a=cow(),b=cow(),id=Id.parse("probe:external_clock"),session=bind(a,Optional.empty());a.setHealth(1);b.setHealth(1);
  effect(a,id,100,5);effect(b,id,100,5);var start=api.time().now(a.getUUID(),id);
  for(var n=0;n<8;n++){api.time().now(a.getUUID(),id);world.effects().snapshot(a.getUUID());api.drivers().snapshot(a.getUUID());}
  check(api.time().now(a.getUUID(),id)===start&&a.getHealth()===1,"queries do not advance time or execute effects");
  jump(20);check(api.time().now(a.getUUID(),id)===start&&a.getHealth()===1,"managed entity freezes while world advances");
  check(b.getHealth()===2,"unmanaged natural jump consumes one due interval without catch-up");
  var request=UUID.randomUUID(),r=session.advance(0,request,10);check(r.completed()&&r.consumed()===10&&a.getHealth()===3,"explicit advance visits every due periodic interval");
  session.advance(0,request,10);check(a.getHealth()===3,"duplicate advance does not consume time or healing twice");
  check(rejects(function(){session.advance(0,request,11);}),"request identity rejects changed increment");
  session.close();sessions.pop();var released=api.time().now(a.getUUID(),id);jump(1);
  check(api.time().now(a.getUUID(),id)===released+1&&a.getHealth()===3,"release preserves phase without reinstalling benefits");
  session=bind(a,Optional.empty());var saved=Life.save(world,a.getUUID()),before=api.time().now(a.getUUID(),id);
  Life.clear(world,a.getUUID(),ES.Removal.WORLD_EXIT);sessions.pop();Life.restore(world,a.getUUID(),saved);check(rejects(function(){session.advance(2,UUID.randomUUID(),1);}),"old session cannot survive restore");jump(7);
  check(api.time().now(a.getUUID(),id)===before&&!api.drivers().snapshot(a.getUUID()).get(0).bound(),"restored ownership waits for driver rebind");
  session=bind(a,Optional.empty());session.advance(0,request,10);check(api.time().now(a.getUUID(),id)===before,"saved completed request stays consumed after rebind");
  var instance=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),Id.parse("probe:time_provider"),Optional.empty(),"source");
  var handle=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),a.getUUID(),instance,new (cls("combat.api.source.SourcePlan"))(Id.parse("probe:time_source"),Map.of(),Map.of(),Map.of(),Map.of(),true))).value().orElseThrow();
  var source=api.sources().snapshot(a.getUUID()).stream().filter(function(view){return view.source().equals(instance);}).findFirst().orElseThrow().installationKey(),specific=bind(a,Optional.of(source)),base=api.time().now(a.getUUID(),id),specificStart=api.time().now(a.getUUID(),source);
  specific.advance(0,UUID.randomUUID(),3);check(api.time().now(a.getUUID(),source)===specificStart+3&&api.time().now(a.getUUID(),id)===base,"source override advances independently of entity owner");
  session.advance(1,UUID.randomUUID(),2);check(api.time().now(a.getUUID(),source)===specificStart+3,"entity advance excludes externally owned source");
  check(rejects(function(){session.advance(0,request,10);}),"older sequence cannot replay after a newer request");
  check(api.sources().revoke(UUID.randomUUID(),handle).applied(),"source revoke cancels its clock ownership");sessions.pop();check(specific.released()&&String(specific.release())==="RELEASED","source revocation exposes terminal release");check(rejects(function(){specific.advance(2,UUID.randomUUID(),1);}),"revoked source session is stale");
 }finally{for(var i=0;i<entities.length;i++){Life.clear(world,entities[i].getUUID(),ES.Removal.WORLD_EXIT);entities[i].discard();}}
 return JSON.stringify({passed:checks.length,checks:checks});
})()
