(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var UUID=Java.loadClass("java.util.UUID"),Map=Java.loadClass("java.util.Map"),Optional=Java.loadClass("java.util.Optional"),Probe=Java.loadClass("probe.AbilityExtension"),level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess");Life.beginTick(world);
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),service=api.abilities();Probe.reset();
 for(var old of level.getAllEntities())for(var t of old.getTags())if(String(t).indexOf("ability_delay_")===0){Life.clear(world,old.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);old.discard();break;}
 function cow(tag,x){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level);e.setPos(x,512,0);e.setNoGravity(true);e.setNoAi(true);e.addTag(tag);level.addFreshEntity(e);cls("combat.platform.CombatPersistence").tick(level);e.setHealth(1);return e;}
 var ids=[];
 for(var mode of ["complete","cancel","revoke"]){
  var actor=cow("ability_delay_actor_"+mode,ids.length*4),target=cow("ability_delay_target_"+mode,ids.length*4+2),source=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),Probe.id("delay_provider"),Optional.of(actor.getUUID()),mode),plan=new (cls("combat.api.source.SourcePlan"))(Probe.id("ability_delayed"),Map.of(),Map.of(),Map.of(),Map.of(Probe.id("grant"),Probe.id("ability_delayed")),true);
  var installation=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),actor.getUUID(),source,plan));if(!installation.applied())throw new Error("delay source not installed");
  var request=new (cls("combat.ability.api.AbilityRequest"))(UUID.randomUUID(),service.grants(actor.getUUID()).get(0),new (cls("combat.ability.api.AbilityTarget$Entity"))(target.getUUID()),cls("combat.ability.api.AbilityParameters").EMPTY),result=service.execute(request);
  if(!result.receipt().applied()||String(result.execution().orElseThrow().status())!=="WAITING")throw new Error("delay not admitted: "+result+" availability="+service.evaluate(request));
  var snapshot=result.execution().orElseThrow();actor.addTag("ability_execution_"+snapshot.execution());actor.addTag("ability_action_"+snapshot.action().id());
  if(mode==="cancel"&&!service.cancel(UUID.randomUUID(),actor.getUUID(),snapshot.execution()).applied())throw new Error("cancel failed");
  if(mode==="revoke"&&!api.sources().revoke(UUID.randomUUID(),installation.value().orElseThrow()).applied())throw new Error("revoke failed");
  ids.push({mode:mode,execution:String(snapshot.execution()),action:String(snapshot.action().id()),due:Number(snapshot.nextTick().orElseThrow())});
 }
 return JSON.stringify({prepared:ids,waitTicks:8,gameTick:Number(level.getGameTime())});
})();
