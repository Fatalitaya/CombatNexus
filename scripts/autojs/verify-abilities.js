(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var UUID=Java.loadClass("java.util.UUID"),Map=Java.loadClass("java.util.Map"),List=Java.loadClass("java.util.List"),Optional=Java.loadClass("java.util.Optional"),Probe=Java.loadClass("probe.AbilityExtension"),checks=[];
 function id(n){return Probe.id(n);}function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess");Life.beginTick(world);
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),service=api.abilities(),entities=[];
 function cow(x){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(x,512,0);e.setNoGravity(true);e.setNoAi(true);level.addFreshEntity(e);entities.push(e);cls("combat.platform.CombatPersistence").tick(level);return e;}
 var actor=cow(0),one=cow(2),two=cow(4),NS=Java.loadClass("net.minecraft.world.entity.EquipmentSlot"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Registry=Java.loadClass("net.minecraft.core.registries.BuiltInRegistries"),NativeId=cls("platform.NativeIds"),Target=cls("combat.ability.api.AbilityTarget"),Request=cls("combat.ability.api.AbilityRequest"),Params=cls("combat.ability.api.AbilityParameters");
 function changed(slot,stack){actor.setItemSlot(slot,stack);cls("combat.spi.CombatEquipmentAccess").changed(world,actor.getUUID(),cls("platform.NativeEquipment").position(slot),cls("combat.platform.WorldEquipmentObservation").capture(level,stack));}
 function wand(kind){return new Stack(Registry.ITEM.getValue(NativeId.nativeId(id("ability_"+kind+"_wand"))));}
 function grants(kind){return service.grants(actor.getUUID()).stream().filter(function(g){return g.ability().equals(id("ability_"+kind));}).toList();}
 function req(grant,target){return new Request(UUID.randomUUID(),grant,target||new Target.Entity(one.getUUID()),Params.EMPTY);}
 function execute(request){var r=service.execute(request);check(r.receipt().applied(),"ability admitted: "+request.grant().ability());return r.execution().orElseThrow();}
 function custom(kind){var source=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),id("provider"),Optional.of(actor.getUUID()),kind),plan=new (cls("combat.api.source.SourcePlan"))(id("ability_"+kind),Map.of(),Map.of(),Map.of(),Map.of(id("grant"),id("ability_"+kind)),true);var r=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),actor.getUUID(),source,plan));check(r.applied(),"source installation: "+kind);return r.value().orElseThrow();}
 try {
  var unknownSource=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),id("unknown_provider"),Optional.of(actor.getUUID()),"unknown"),unknownPlan=new (cls("combat.api.source.SourcePlan"))(id("unknown"),Map.of(id("effect"),world.domainDefinitions().effects().values().iterator().next()),Map.of(),Map.of(),Map.of(id("missing"),id("missing_ability")),true),sourceCount=api.sources().snapshot(actor.getUUID()).size(),effectCount=api.effects().snapshot(actor.getUUID()).size();
  var unknown=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),actor.getUUID(),unknownSource,unknownPlan));check(!unknown.applied()&&unknown.value().isEmpty()&&service.grants(actor.getUUID()).isEmpty()&&api.sources().snapshot(actor.getUUID()).size()===sourceCount&&api.effects().snapshot(actor.getUUID()).size()===effectCount,"unknown ability reference rejects before source handle or domain writes");
  Probe.reset();one.setHealth(1);two.setHealth(1);changed(NS.MAINHAND,wand("heal"));changed(NS.OFFHAND,wand("heal"));var pair=grants("heal");
  check(pair.size()===2&&!pair.get(0).id().equals(pair.get(1).id()),"two slots retain distinct ability grants");
  check(api.equipment(actor.getUUID()).stream().allMatch(function(f){return f.grants().size()===1&&f.sources().size()===1;}),"equipment facts expose binding source evidence and exact grants");
  var request=req(pair.get(0)),before=String(Life.save(world,actor.getUUID())),calls=Probe.executions;
  for(var i=0;i<4;i++){check(service.evaluate(request).available(),"read-only availability "+i+": "+service.evaluate(request));api.equipment(actor.getUUID());service.grants(actor.getUUID());}
  check(before===String(Life.save(world,actor.getUUID()))&&one.getHealth()===1&&Probe.executions===calls,"queries preserve business state, health and executor calls");
  var first=execute(request);check(String(first.status())==="COMPLETED"&&one.getHealth()===4,"healing capability applies three health");
  check(String(service.evaluate(req(pair.get(0))).reason())==="COOLDOWN"&&service.evaluate(req(pair.get(1))).available(),"default cooldown belongs to its grant");
  execute(req(pair.get(1)));check(one.getHealth()===7,"second equipment grant executes independently");
  var duplicate=service.execute(request);check(!duplicate.receipt().applied()&&one.getHealth()===7,"duplicate request never replays healing");
  check(Probe.expiredContext(),"retained execution callback loses command capability");
  var patched=actor.getMainHandItem().copy(),Patch=cls("combat.api.item.CombatItemBindingPatch"),Builder=cls("combat.api.item.CombatItemBindingBuilder");
  patched.set(cls("combat.platform.CombatDataComponents").PATCH.get(),Patch.replaceAll(new Builder().entryTemplate(id("patched_binding"),id("ability_shared_entry"),cls("api.equipment.EquipmentPosition").MAINHAND).build()));
  changed(NS.MAINHAND,patched);check(!service.evaluate(request).available()&&grants("shared").size()===1,"instance patch invalidates old grant and installs new contribution");
  changed(NS.MAINHAND,Stack.EMPTY);check(!service.evaluate(req(pair.get(0))).available(),"removed equipment invalidates old grant");
  changed(NS.OFFHAND,Stack.EMPTY);changed(NS.MAINHAND,wand("shared"));changed(NS.OFFHAND,wand("shared"));pair=grants("shared");one.setHealth(1);execute(req(pair.get(0)));
  check(String(service.evaluate(req(pair.get(1))).reason())==="COOLDOWN","explicit shared group shares cooldown across equipment");
  changed(NS.MAINHAND,wand("heal"));changed(NS.OFFHAND,Stack.EMPTY);one.setHealth(1);two.setHealth(1);Probe.reset();
  var multi=execute(req(grants("heal").get(0),new Target.Entities(List.of(one.getUUID(),two.getUUID()))));
  check(one.getHealth()===4&&two.getHealth()===4&&multi.commands().size()===2,"multi-target execution applies both domain commands");
  check(Probe.actions().size()===1&&Probe.actions().get(0).equals(multi.action().id()),"multi-target commands belong to the ability action");
  check(multi.commands().stream().allMatch(function(c){return c.context().isPresent()&&c.context().orElseThrow().action().orElseThrow().equals(multi.action());}),"multi-target command receipts retain the action identity");
  changed(NS.MAINHAND,wand("combo"));one.setHealth(10);one.invulnerableTime=0;var combo=execute(req(grants("combo").get(0)));
  check(one.getHealth()<10&&combo.commands().size()===2&&!api.effects().snapshot(one.getUUID()).isEmpty(),"damage followed by Effect executes through domain receipts");
  check(combo.commands().stream().allMatch(function(c){return c.context().isPresent()&&c.context().orElseThrow().action().orElseThrow().equals(combo.action());}),"damage and Effect receipts retain the same action");
  changed(NS.MAINHAND,wand("delayed"));one.setHealth(1);var delayed=execute(req(grants("delayed").get(0)));
  check(String(delayed.status())==="WAITING"&&one.getHealth()===1,"continuation schedules without early healing");
  check(service.cancel(UUID.randomUUID(),actor.getUUID(),delayed.execution()).applied()&&String(service.execution(delayed.execution()).orElseThrow().status())==="CANCELLED","pending continuation cancels explicitly");
  custom("partial");one.setHealth(1);request=req(grants("partial").get(0));var partial=service.execute(request);
  check(!partial.receipt().applied()&&String(partial.execution().orElseThrow().status())==="PARTIAL"&&one.getHealth()===2&&!partial.receipt().journal().applicationCompleted(),"write then throw reports partial application with retained healing");
  service.execute(request);check(one.getHealth()===2,"partial execution is not replayed on repeat request");
  var numbers=api.drivers().bind(Java.loadClass("probe.DriverExtension").numbers,one.getUUID(),Java.loadClass("java.util.Set").of(cls("combat.driver.api.DriverScope").NUMERICAL),Optional.empty());
  try {
   custom("heal");one.setHealth(1);var delegatedRequest=req(grants("heal").get(grants("heal").size()-1));
   var delegated=numbers.withNumericalResolver(Java.loadClass("probe.DriverExtension").fixedNumbers(2,1),function(){return service.execute(delegatedRequest);});
   check(delegated.receipt().applied()&&one.getHealth()===3,"generic healing uses scoped numerical resolution exactly once");
   var child=delegated.execution().orElseThrow().commands().get(0).context().orElseThrow().identity().orElseThrow();
   check(child.depth()>0,"resolved healing retains the real ability parent");
   var ordinary=new (cls("combat.healing.api.HealingRequest"))(UUID.randomUUID(),one.getUUID(),new (cls("combat.api.CombatSource"))(id("outside"),Optional.empty(),Optional.empty()),cls("combat.healing.api.HealingRequest$Reason").SKILL,2);
   check(api.healing().submit(ordinary).rejection().isPresent()&&one.getHealth()===3,"resolver scope cannot authorize later ordinary healing");
  } finally { numbers.close();one.setHealth(2); }
  custom("unknown");request=req(grants("unknown").get(0));var beforeCalls=Probe.executions,unknownResult=service.execute(request);check(!unknownResult.receipt().applied()&&String(unknownResult.execution().orElseThrow().status())==="UNKNOWN"&&one.getHealth()===2,"failure without confirmed evidence remains UNKNOWN");service.execute(request);check(Probe.executions===beforeCalls+1&&one.getHealth()===2&&String(service.evaluate(req(grants("unknown").get(0))).reason())==="COOLDOWN","unknown execution never replays or refunds admitted cooldown");
  custom("unavailable");request=req(grants("unavailable").get(0));check(String(service.evaluate(request).reason())==="EXECUTOR_REFUSED"&&!service.execute(request).receipt().applied(),"executor availability refuses without invocation");
  changed(NS.MAINHAND,wand("periodic"));actor.setHealth(1);var scope=cls("combat.driver.api.DriverScope"),driver=api.drivers().bind(Java.loadClass("probe.DriverExtension").abilities,actor.getUUID(),Java.loadClass("java.util.Set").of(scope.PERIODIC),Optional.empty());
  try {
   var periodic=grants("periodic").get(0),self=new Target.Self();check(!service.execute(req(periodic,self)).receipt().applied(),"automatic periodic grant rejects direct active entry");var trigger=driver.trigger(req(periodic,self));check(trigger.receipt().applied()&&actor.getHealth()===4,"bound periodic owner executes through trigger entry");var repeated=driver.trigger(req(periodic,self));check(!repeated.receipt().applied()&&actor.getHealth()===4,"external periodic trigger retains consumed source trigger limit: "+repeated+" health="+actor.getHealth());
   var Definitions=cls("combat.internal.definition.CombatDefinitions"),Generation=cls("combat.internal.definition.DefinitionGeneration"),kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()),published=world.definitions();
   var binding=api.sources().captureDefinitions(),restoredBinding=api.sources().restoreDefinitions(binding.snapshot());
   check(restoredBinding.snapshot().semanticIdentity().equals(binding.snapshot().semanticIdentity()),"definition binding restores its pure semantic identity");
   var altered=JSON.parse(String(binding.snapshot().content()));Object.keys(altered.executors).forEach(function(key){altered.executors[key].semanticVersion=999;});var incompatible=false;
   try{api.sources().restoreDefinitions(new (cls("combat.api.source.DefinitionBindingSnapshot"))(binding.snapshot().semanticIdentity(),JSON.stringify(altered)));}catch(expected){incompatible=true;}
   check(incompatible,"bound executor semantic mismatch rejects restoration");
   var boundSource=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),id("bound_provider"),Optional.of(actor.getUUID()),"bound"),boundPlan=new (cls("combat.api.source.SourcePlan"))(id("bound"),Map.of(),Map.of(),Map.of(),Map.of(id("bound_grant"),id("ability_heal")),true);
   var boundInstall=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),actor.getUUID(),boundSource,boundPlan),restoredBinding);
   check(boundInstall.applied(),"restored binding installs a real source and grant");
   var boundGrant=service.grants(actor.getUUID()).stream().filter(function(g){return g.source().equals(boundSource);}).findFirst().orElseThrow();
   var reload=function(change){var domains=published.domains();if(change){var values=new (Java.loadClass("java.util.HashMap"))(domains.abilities()),old=values.get(id("ability_heal"));values.put(old.id(),new (cls("combat.ability.api.AbilityDefinition"))(old.id(),old.title(),old.description(),old.targets(),old.executor(),new Params(Map.of("amount",7.0),Map.of()),old.cooldownTicks(),old.sharedCooldown(),old.trigger()));domains=new (cls("combat.api.DomainDefinitions"))(domains.healingRules(),domains.regeneration(),domains.shields(),domains.effects(),values);}Definitions.publish(kernel,new Generation(Definitions.nextGeneration(),published.settings(),published.damage(),published.behaviors(),domains,published.itemBindings()));};
   reload(true);check(!service.evaluate(req(periodic,self)).available(),"successful generation publication invalidates old grant");changed(NS.MAINHAND,actor.getMainHandItem());
   one.setHealth(1);var boundResult=service.execute(req(boundGrant));
   check(boundResult.receipt().applied()&&one.getHealth()===4,"bound grant executes original semantics after latest generation changes");
   var fresh=custom("heal"),freshGrant=grants("heal").stream().filter(function(g){return g.source().instance().equals(fresh.instance());}).findFirst().orElseThrow();
   one.setHealth(1);check(service.execute(req(freshGrant)).receipt().applied()&&one.getHealth()===8,"new unbound source adopts changed ability values");api.sources().revoke(UUID.randomUUID(),fresh);
   var restoredActor=cow(6),copySource=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),id("bound_provider"),Optional.of(restoredActor.getUUID()),"restore");
   check(api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),restoredActor.getUUID(),copySource,boundPlan),restoredBinding).applied(),"bound source installed for lifecycle restore");
   var savedCopy=Life.save(world,restoredActor.getUUID());Life.clear(world,restoredActor.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);Life.restore(world,restoredActor.getUUID(),savedCopy);
   var restoredGrant=service.grants(restoredActor.getUUID()).get(0);one.setHealth(1);
   check(service.execute(req(restoredGrant)).receipt().applied()&&one.getHealth()===4,"source lifecycle restore retains bound values despite changed latest content");
   var savedBound=JSON.parse(String(Life.save(world,actor.getUUID())));
   check(savedBound.sources.some(function(v){return v.binding===String(binding.snapshot().semanticIdentity()) && savedBound.definitionBindings[v.binding];}),"source save retains bound definition values");
   check(api.sources().revoke(UUID.randomUUID(),boundInstall.value().orElseThrow()).applied()&&!service.evaluate(req(boundGrant)).available(),"source revocation invalidates a bound grant immediately");
   var refreshed=grants("periodic").get(0);check(!refreshed.id().equals(periodic.id())&&!driver.trigger(req(refreshed,self)).receipt().applied()&&actor.getHealth()===4,"reload reconciles identity while preserving consumed source trigger counter");
   changed(NS.MAINHAND,Stack.EMPTY);reload();changed(NS.MAINHAND,Stack.EMPTY);check(grants("periodic").isEmpty(),"successful reload never resurrects revoked equipment grants");
  }finally{driver.close();}
  var bindingBase=world.definitions(),bindingKernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()),bindingDefinitions=cls("combat.internal.definition.CombatDefinitions"),bindingGeneration=cls("combat.internal.definition.DefinitionGeneration");
  var bindingActor=cow(8),bindingTarget=cow(10),installedBindings=[];
  try {
   var snapshot=api.sources().captureDefinitions().snapshot();
   var makeBinding=function(additive){var data=JSON.parse(String(snapshot.content()));data.domains.healingRules={};data.domains.healingRules["combatnexus_effect_probe:binding_rule"]={priority:0,phase:"CONTRIBUTIONS",reasons:["SKILL"],sources:[],minimumHealthRatio:0,maximumHealthRatio:1,additive:additive,multiplier:1};return api.sources().restoreDefinitions(new (cls("combat.api.source.DefinitionBindingSnapshot"))(UUID.randomUUID(),JSON.stringify(data)));};
   var bindingA=makeBinding(1),bindingB=makeBinding(2);
   var installBinding=function(binding){var source=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),id("binding_review"),Optional.of(bindingActor.getUUID()),"review"),plan=new (cls("combat.api.source.SourcePlan"))(id("binding_review"),Map.of(),Map.of(),Map.of(),Map.of(id("grant"),id("ability_heal")),true),result=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),bindingActor.getUUID(),source,plan),binding);check(result.applied(),"install independent definition binding");installedBindings.push(result.value().orElseThrow());return service.grants(bindingActor.getUUID()).stream().filter(function(g){return g.source().equals(source);}).findFirst().orElseThrow();};
   var grantB=installBinding(bindingB),sourceB=new (cls("combat.api.CombatSource"))(api.sources().snapshot(bindingActor.getUUID()).get(0).installationKey(),Optional.of(bindingActor.getUUID()),Optional.empty());
   var rules=Map.of(id("binding_rule"),new (cls("combat.healing.api.HealingRuleDefinition"))(0,cls("combat.healing.api.HealingPhase").CONTRIBUTIONS,Java.loadClass("java.util.Set").of(cls("combat.healing.api.HealingRequest$Reason").SKILL),Java.loadClass("java.util.Set").of(),0,1,5,1)),domains=bindingBase.domains();
   bindingDefinitions.publish(bindingKernel,new bindingGeneration(bindingDefinitions.nextGeneration(),bindingBase.settings(),bindingBase.damage(),bindingBase.behaviors(),new (cls("combat.api.DomainDefinitions"))(rules,domains.regeneration(),domains.shields(),domains.effects(),domains.abilities()),bindingBase.itemBindings()));
   var healingRequest=function(source){return new (cls("combat.healing.api.HealingRequest"))(UUID.randomUUID(),bindingTarget.getUUID(),source,cls("combat.healing.api.HealingRequest$Reason").SKILL,3);};
   bindingTarget.setHealth(1);var directRequest=healingRequest(sourceB),direct=api.healing().submit(directRequest);
   check(direct.actualRecovery()===5&&bindingTarget.getHealth()===6,"independent healing retains source binding after rule reload");
   check(api.healing().submit(directRequest).rejection().isPresent()&&bindingTarget.getHealth()===6,"bound healing still rejects duplicate requests");
   var parentGrant=installBinding(bindingA);Probe.healingSourceOverride=sourceB;bindingTarget.setHealth(1);
   var cross=service.execute(req(parentGrant,new Target.Entity(bindingTarget.getUUID())));
   check(cross.receipt().applied()&&bindingTarget.getHealth()===6,"child healing selects binding B under parent binding A");
   check(cross.execution().orElseThrow().commands().get(0).context().orElseThrow().identity().orElseThrow().depth()>0,"cross-binding healing retains parent lineage");
   Probe.healingSourceOverride=new (cls("combat.api.CombatSource"))(id("unbound_review"),Optional.empty(),Optional.empty());parentGrant=installBinding(bindingA);bindingTarget.setHealth(1);
   check(service.execute(req(parentGrant,new Target.Entity(bindingTarget.getUUID()))).receipt().applied()&&bindingTarget.getHealth()===9,"unbound child uses adopted rules instead of parent binding");
   Probe.healingSourceOverride=null;parentGrant=installBinding(bindingA);bindingTarget.setHealth(1);
   check(service.execute(req(parentGrant,new Target.Entity(bindingTarget.getUUID()))).receipt().applied()&&bindingTarget.getHealth()===5,"same-source child retains its own binding");
  } finally {
   Probe.healingSourceOverride=null;
   for(var handle of installedBindings)api.sources().revoke(UUID.randomUUID(),handle);
   bindingDefinitions.publish(bindingKernel,new bindingGeneration(bindingDefinitions.nextGeneration(),bindingBase.settings(),bindingBase.damage(),bindingBase.behaviors(),bindingBase.domains(),bindingBase.itemBindings()));
  }
  return JSON.stringify({passed:checks.length,checks:checks});
 } finally {for(var e of entities){Life.clear(world,e.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);e.discard();}}
})();
