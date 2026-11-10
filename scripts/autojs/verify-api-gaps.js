(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var List=Java.loadClass("java.util.List"),Set=Java.loadClass("java.util.Set"),OL=Java.loadClass("java.util.OptionalLong"),UUID=Java.loadClass("java.util.UUID");
 var Probe=Java.loadClass("probe.ApiGapProbe"),Id=cls("api.id.NexusId"),ES=cls("effect.api.EffectService"),EI=cls("effect.api.EffectInstance"),BP=cls("combat.api.behavior.BehaviorPlan");
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
 cls("combat.spi.CombatWorldLifecycleAccess").beginTick(world);
 var checks=[],entities=[];function check(ok,name){if(!ok)throw new Error(name);checks.push(name);}
 function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
 function install(e,name,component,priority){var id=Probe.id(name);var effect=new EI(new (cls("effect.api.EffectSnapshot"))(e.getUUID(),id,id,0,OL.of(level.getGameTime()+200),Set.of()),List.of(),cls("effect.api.EffectOwnership").MANAGED,new BP(priority,List.of(component)));check(String(api.effects().apply(new (cls("effect.api.EffectApplication"))(UUID.randomUUID(),effect,ES.Stacking.REPLACE)).outcome())==="APPLIED","install "+name);}
 function result(e,amount,kind){e.invulnerableTime=0;var b=cls("combat.damage.api.damage.DamageRequest").builder(level,e,cls("combat.damage.platform.source.NativeDamageSources").from(e.damageSources().generic()),amount);if(kind)b.kind(kind);return cls("combat.damage.api.DamageModuleApi").submitDamage(b.build());}
 function hit(e,amount,kind){return result(e,amount,kind).settlement().orElseThrow();}
 function absorption(e){var source=new (cls("combat.api.CombatSource"))(Probe.id("absorption"),Java.loadClass("java.util.Optional").empty(),Java.loadClass("java.util.Optional").empty());check(api.absorption().submit(new (cls("combat.absorption.api.AbsorptionCommand"))(UUID.randomUUID(),e.getUUID(),source,cls("combat.absorption.api.AbsorptionCommand$Operation").GRANT,2,2)).applied(),"grant absorption");}
 function shield(e,name,amount){check(api.shields().grant(UUID.randomUUID(),e.getUUID(),Probe.shield(name,amount)).applied(),"grant "+name);}
 try{
  var zero=cow(),z=hit(zero,0);check(z.successfulHit()&&!z.causedDamage()&&String(z.contactResult())==="ACCEPTED","admitted zero damage is a successful contact without damage");
  check(Probe.zeroAuthority,"zero successful hit grants scoped reaction authority");
  var immune=cow();immune.setInvulnerable(true);var immunity=result(immune,1);check(String(immunity.contactResult())==="IMMUNE"&&immunity.settlement().isEmpty(),"immune request cannot become successful contact");
  var filtered=cow();install(filtered,"filtered",new (Java.loadClass("probe.ApiGapProbe$Intercept"))(),0);
  hit(filtered,1,cls("combat.damage.api.damage.DamageRequestKind").ENVIRONMENTAL);check(filtered.getHealth()===9,"request kind can decline interception");
  check(String(hit(filtered,1).contactResult())==="INTERCEPTED","declined protection preserves its use");
  var v=cow();install(v,"first",new (Java.loadClass("probe.ApiGapProbe$Intercept"))(),100);install(v,"second",new (Java.loadClass("probe.ApiGapProbe$Intercept"))(),0);
  check(hit(v,0).successfulHit(),"zero contact does not consume positive-damage protection");
  shield(v,"existing",3);absorption(v);var initial=v.getHealth();
  var first=hit(v,4);check(v.getHealth()===initial&&v.getAbsorptionAmount()===2&&first.evidence().shieldLoss()===0&&String(first.contactResult())==="INTERCEPTED","interception precedes shield and absorption writes: "+v.getHealth()+" / "+v.getAbsorptionAmount()+" / "+first.evidence().shieldLoss()+" / "+first.contactResult());
  var second=hit(v,4);check(String(second.contactResult())==="INTERCEPTED","unselected protection keeps its use");
  var third=hit(v,4);check(third.evidence().shieldLoss()===3&&v.getAbsorptionAmount()===1,"exhausted protections allow resource loss");
  var threshold=cow();install(threshold,"apple",new (Java.loadClass("probe.ApiGapProbe$Threshold"))(),0);
  var boundary=hit(threshold,8);check(threshold.getHealth()===2&&boundary.evidence().shieldLoss()===0,"exact twenty percent does not react");
  threshold.setHealth(10);shield(threshold,"old",2);absorption(threshold);
  var reactive=hit(threshold,13);check(threshold.getHealth()===10&&threshold.getAbsorptionAmount()===0&&reactive.evidence().shieldLoss()===11,"old shield and absorption precede newly granted shield on the same request");
  check(reactive.evidence().beforeApplication().steps().size()===1,"threshold grant retains child command evidence");
  var failed=cow();install(failed,"failed",new (Java.loadClass("probe.ApiGapProbe$Failure"))(),0);
  var stopped=false;try{hit(failed,1);}catch(e){stopped=String(e).indexOf("Damage guard application stopped")>=0;}var failure=Probe.last;check(stopped&&failed.getHealth()===10&&!failure.successfulHit()&&!failure.evidence().applicationCompleted(),"rejected reactive command stops parent damage");
  var after=hit(failed,1);check(failed.getHealth()===9,"failed reaction remains consumed");
 }finally{for(var e of entities){cls("combat.spi.CombatWorldLifecycleAccess").clear(world,e.getUUID(),ES.Removal.WORLD_EXIT);e.discard();}}
 return JSON.stringify({passed:checks.length,checks:checks});
})()
