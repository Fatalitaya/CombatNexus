(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var UUID=Java.loadClass("java.util.UUID"),Map=Java.loadClass("java.util.Map"),List=Java.loadClass("java.util.List"),Set=Java.loadClass("java.util.Set"),Optional=Java.loadClass("java.util.Optional"),Probe=Java.loadClass("probe.GenericCombatExtension"),checks=[];
 function id(n){return Probe.id(n);}function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess");Life.beginTick(world);
 var api=cls("combat.api.CombatAccess").find(level).orElseThrow(),service=api.abilities(),actor=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
 actor.setPos(0,512,0);actor.setNoGravity(true);actor.setNoAi(true);level.addFreshEntity(actor);cls("combat.platform.CombatPersistence").tick(level);
 var target=actor.getUUID(),source=new (cls("combat.api.CombatSource"))(id("generic"),Optional.of(target),Optional.empty()),slots=cls("combat.ability.api.AbilityCooldownSlot"),Command=cls("combat.ability.api.AbilityCooldownCommand"),Target=cls("combat.ability.api.AbilityTarget"),Params=cls("combat.ability.api.AbilityParameters"),Request=cls("combat.ability.api.AbilityRequest"),Reinput=cls("combat.ability.api.AbilityReinput");
 var clock=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,target,Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty()),sequence=0;
 function advance(ticks){check(clock.advance(sequence++,UUID.randomUUID(),ticks).completed(),"business time advance "+sequence);}
 function install(name){var instance=new (cls("combat.api.source.SourceInstance"))(UUID.randomUUID(),id("generic_provider"),Optional.of(target),"probe");var plan=new (cls("combat.api.source.SourcePlan"))(id(name),Map.of(),Map.of(),Map.of(),Map.of(id("grant"),id(name)),true);var handle=api.sources().install(new (cls("combat.api.source.SourceInstallation"))(UUID.randomUUID(),target,instance,plan)).value().orElseThrow();var grant=service.grants(target).stream().filter(function(g){return g.source().instance().equals(instance.instance());}).findFirst().orElseThrow();return {handle:handle,grant:grant};}
 var skill=new slots.Skill(id("window")),group=new slots.Shared(id("window_group"));
 function change(slot,op,ticks){return service.cooldown(new Command(UUID.randomUUID(),target,source,slot,op,ticks));}
 function clear(){change(skill,Command.Operation.CLEAR,0);change(group,Command.Operation.CLEAR,0);}
 function execute(g){return service.execute(new Request(UUID.randomUUID(),g,new Target.Entities(List.of()),Params.EMPTY));}
 function input(execution,params){return service.reinput(new Reinput(UUID.randomUUID(),target,execution,new Target.Self(),params));}
 try{
  var first=install("window"),peer=install("window_peer");Probe.world=api;Probe.react=true;Probe.consumed=0;Probe.callbacks=0;Probe.facts.clear();
  var address=new (cls("combat.state.api.StateAddress"))(target,Java.loadClass("probe.PlanExample").RETAINED_STATE,Optional.empty());api.states().write(UUID.randomUUID(),address,source,Java.loadClass("java.lang.Integer").valueOf(4));
  var result=execute(first.grant),execution=result.execution().orElseThrow().execution();
  check(result.receipt().applied()&&String(result.execution().orElseThrow().waiting())==="INPUT"&&result.execution().orElseThrow().remainingInputs()===2,"empty-target release opens input window");
  check(Probe.consumed===1&&api.states().read(address).isEmpty(),"release reaction atomically consumes once despite observer exception");
  check(String(api.states().take(UUID.randomUUID(),address,source,Probe.retained).rejection().orElseThrow().code())==="INVALID_PHASE","retained parent expires after reaction");
  check(Probe.facts.size()===2&&String(Probe.facts.get(0).kind())==="ADMITTED"&&String(Probe.facts.get(1).kind())==="RELEASED","admission and release facts delivered in FIFO order");
  check(!execute(peer.grant).receipt().applied()&&service.cooldown(target,skill).remaining()===0,"shared occupancy blocks another execution before end cooldown");
  var rejected=input(execution,new Params(Map.of("reject",1.0),Map.of()));check(!rejected.receipt().applied()&&service.execution(execution).orElseThrow().remainingInputs()===2,"executor refusal does not consume input");
  var repeat=new Reinput(UUID.randomUUID(),target,execution,new Target.Self(),Params.EMPTY);check(service.reinput(repeat).receipt().applied()&&service.execution(execution).orElseThrow().remainingInputs()===1,"accepted changed target consumes one opportunity");
  check(!service.reinput(repeat).receipt().applied()&&service.execution(execution).orElseThrow().remainingInputs()===1,"duplicate input cannot consume another opportunity");
  advance(4);
  check(input(execution,Params.EMPTY).receipt().applied()&&String(service.execution(execution).orElseThrow().status())==="COMPLETED","final input completes execution");
  check(service.cooldown(target,skill).remaining()===10&&service.cooldown(target,group).remaining()===20,"independent and shared cooldowns start together at execution end");
  check(Probe.facts.size()===3&&String(Probe.facts.get(2).kind())==="TERMINAL","input steps do not republish initial release");
  change(skill,Command.Operation.CLEAR,0);check(!execute(first.grant).receipt().applied()&&service.cooldown(target,group).remaining()===20,"clearing independent slot leaves shared constraint");
  clear();change(skill,Command.Operation.START,30);change(skill,Command.Operation.START,5);check(service.cooldown(target,skill).remaining()===30,"start does not overwrite running cooldown");
  change(skill,Command.Operation.RESET,3);check(service.cooldown(target,skill).remaining()===3,"reset sets exact remaining duration");clear();
  result=execute(first.grant);execution=result.execution().orElseThrow().execution();change(skill,Command.Operation.RESET,50);advance(5);
  check(!input(execution,Params.EMPTY).receipt().applied()&&String(service.execution(execution).orElseThrow().closeReason().orElseThrow())==="EXPIRED","exact deadline closes input without callback");
  check(service.cooldown(target,skill).remaining()===45&&service.cooldown(target,group).remaining()===20,"automatic end cooldown preserves longer external reset");
  clear();result=execute(first.grant);execution=result.execution().orElseThrow().execution();Probe.failInput=true;var failed=input(execution,Params.EMPTY);Probe.failInput=false;
  check(!failed.receipt().applied()&&String(failed.execution().orElseThrow().status())==="PARTIAL"&&service.cooldown(target,skill).remaining()===10,"failure after release retains success and starts end cooldown");
  clear();result=execute(first.grant);execution=result.execution().orElseThrow().execution();api.sources().revoke(UUID.randomUUID(),first.handle);
  check(String(service.execution(execution).orElseThrow().status())==="CANCELLED"&&service.cooldown(target,skill).remaining()===10,"source revoke closes window and retains player cooldown");
  var replacement=install("window");check(!execute(replacement.grant).receipt().applied(),"new weapon instance cannot evade player skill cooldown");
  var resetRequest=new Command(UUID.randomUUID(),target,source,skill,Command.Operation.RESET,37);check(service.cooldown(resetRequest).applied(),"typed cooldown reset admitted");change(skill,Command.Operation.RESET,41);check(!service.cooldown(resetRequest).applied()&&service.cooldown(target,skill).remaining()===41,"duplicate cooldown request cannot overwrite later reset");
  var readScope=cls("combat.api.transaction.CombatReadScope").open();try{check(!change(skill,Command.Operation.CLEAR,0).applied(),"cooldown mutation refused during read-only phase");}finally{readScope.close();}
  clear();execution=execute(replacement.grant).execution().orElseThrow().execution();
  var kernel=world.serverRuntime(),base=kernel.definitions().orElseThrow(),Definitions=cls("combat.internal.definition.CombatDefinitions"),Generation=cls("combat.internal.definition.DefinitionGeneration");
  Definitions.publish(kernel,new Generation(Definitions.nextGeneration(),base.settings(),base.damage(),base.behaviors(),base.domains(),base.itemBindings()));
  check(String(service.execution(execution).orElseThrow().status())==="CANCELLED"&&service.cooldown(target,skill).remaining()===10,"definition generation replacement closes input and starts end cooldown");
  Probe.react=false;
  var ES=cls("effect.api.EffectService"),Control=cls("effect.api.EffectControl"),Category=cls("effect.api.ControlCategory"),Policy=cls("effect.api.EffectPolicy"),Application=cls("effect.api.EffectApplication"),Mode=cls("effect.api.EffectApplicationPolicy");
  var effect=function(name,origin,category,immune,dispellable,levelValue){var policy=new Policy(Policy.Category.NEUTRAL,dispellable,Set.of(id("control_tag")),new Control(category,Set.of(),Set.of(),immune),Mode.NORMAL);var def=new (cls("effect.api.EffectDefinition"))(levelValue,cls("effect.api.EffectLifetime").timed(levelValue>0?50:100),ES.Stacking.STRONGER_WITH_HIDDEN.id(),policy,List.of(),cls("combat.api.behavior.BehaviorPlan").NONE,Set.of(),cls("effect.api.EffectDisplay").NONE);return def.instantiate(target,id(name),id(origin),api.time().now(target,id(origin)));};
  var immunity=effect("immunity","one",Set.of(),Set.of(Category.MOVEMENT,Category.ACTION,Category.PERCEPTION),true,0),other=effect("immunity","two",Set.of(),Set.of(Category.MOVEMENT),true,0);
  check(api.effects().apply(new Application(UUID.randomUUID(),immunity,ES.Stacking.REPLACE)).rejection().isEmpty(),"install control immunity contribution");api.effects().apply(new Application(UUID.randomUUID(),other,ES.Stacking.REPLACE));
  for(var category of Category.values()){var control=effect("control_"+String(category).toLowerCase(),"control",Set.of(category),Set.of(),true,0);check(api.effects().apply(new Application(UUID.randomUUID(),control,ES.Stacking.REPLACE)).rejection().isPresent(),"ordinary "+category+" control rejected");}
  var Native=cls("effect.api.NativeEffectCommand");
  check(!api.effects().nativeEffect(new Native(UUID.randomUUID(),target,source,cls("api.id.NexusId").parse("minecraft:blindness"),Native.Operation.APPLY,60,0,false,true,true)).applied(),"native blindness respects perception immunity");
  check(api.effects().nativeEffect(new Native(UUID.randomUUID(),target,source,cls("api.id.NexusId").parse("minecraft:weakness"),Native.Operation.APPLY,60,0,false,true,true)).applied(),"unclassified negative effect remains applicable");
  actor.setDeltaMovement(0,0,0);actor.knockback(1,1,0);check(actor.getDeltaMovement().lengthSqr()===0,"native knockback respects movement immunity");
  var controlled=effect("control","control",Set.of(Category.MOVEMENT),Set.of(),false,1);check(api.effects().apply(new Application(UUID.randomUUID(),controlled,ES.Stacking.REPLACE).force()).rejection().isEmpty(),"explicit force bypasses immunity");
  var cleanse=new (cls("effect.api.EffectCleanse"))(UUID.randomUUID(),target,Set.of(Category.MOVEMENT),Set.of());check(!api.effects().cleanse(cleanse).value().orElseThrow().get(0).removed(),"force does not override separate nondispellable policy");
  world.effects().remove(target,id("control"),id("control"),ES.Removal.REMOVED);
  world.effects().remove(target,id("immunity"),id("one"),ES.Removal.REMOVED);check(world.effects().immune(target,id("anything"),Set.of(),Set.of(Category.MOVEMENT)),"revoking one immunity source retains another");
  var weaker=effect("layered","layers",Set.of(Category.MOVEMENT),Set.of(),true,0),stronger=effect("layered","layers",Set.of(Category.MOVEMENT),Set.of(),true,2);
  api.effects().apply(new Application(UUID.randomUUID(),weaker,ES.Stacking.STRONGER_WITH_HIDDEN).force());api.effects().apply(new Application(UUID.randomUUID(),stronger,ES.Stacking.STRONGER_WITH_HIDDEN).force());
  var clean=api.effects().cleanse(new (cls("effect.api.EffectCleanse"))(UUID.randomUUID(),target,Set.of(Category.MOVEMENT),Set.of())).value().orElseThrow();check(clean.size()===2&&clean.get(0).removed()&&clean.get(1).removed(),"cleanse removes both active and hidden control layers");
  var Move=cls("combat.action.api.EntityCommand$Displace"),Vec=cls("api.geometry.Vector3");check(!api.entities().submit(new Move(UUID.randomUUID(),target,source,new Vec(1,0,0),Move.Mode.VELOCITY,Move.Origin.EXTERNAL_CONTROL)).applied(),"external displacement respects movement immunity");
  check(api.entities().submit(new Move(UUID.randomUUID(),target,source,new Vec(1,0,0),Move.Mode.VELOCITY,Move.Origin.VOLUNTARY)).applied(),"voluntary movement is not rejected by control immunity");
  check(!api.effects().nativeEffect(new Native(UUID.randomUUID(),target,source,cls("api.id.NexusId").parse("minecraft:slowness"),Native.Operation.APPLY,60,0,false,true,true)).applied(),"native slowness is classified as movement control");
  check(api.effects().nativeEffect(new Native(UUID.randomUUID(),target,source,cls("api.id.NexusId").parse("minecraft:slowness"),Native.Operation.FORCE_APPLY,60,0,false,true,true)).applied(),"explicit native force applies through immunity");
  check(api.effects().cleanse(new (cls("effect.api.EffectCleanse"))(UUID.randomUUID(),target,Set.of(Category.MOVEMENT),Set.of())).value().orElseThrow().get(0).removed(),"forced native control remains ordinarily dispellable");
  return JSON.stringify({passed:checks.length,checks:checks});
 }finally{Probe.react=false;Probe.failInput=false;Probe.world=null;Life.clear(world,target,cls("effect.api.EffectService").Removal.WORLD_EXIT);actor.discard();}
})();
