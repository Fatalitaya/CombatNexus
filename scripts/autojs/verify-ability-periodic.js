(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld(),UUID=Java.loadClass("java.util.UUID"),actor=level.getEntity(UUID.fromString(PERIODIC_BEFORE.actor)),Probe=Java.loadClass("probe.AbilityExtension"),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),checks=[];function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 if(PERIODIC_PHASE==="trigger"){
  check(actor.getHealth()===1,"PERIODIC-only takeover suppresses automatic execution while world time advances");
  var result=Probe.periodicSession.trigger(new (cls("combat.ability.api.AbilityRequest"))(UUID.randomUUID(),api.abilities().grants(actor.getUUID()).get(0),new (cls("combat.ability.api.AbilityTarget$Self"))(),cls("combat.ability.api.AbilityParameters").EMPTY));
  check(result.receipt().applied()&&actor.getHealth()===4,"external periodic trigger consumes one due execution");Probe.periodicSession.close();Probe.periodicSession=null;
 }else if(PERIODIC_PHASE==="early")check(actor.getHealth()===4,"release does not repeat the externally consumed due execution on next tick");
 else if(PERIODIC_PHASE==="due")check(actor.getHealth()===7,"next interval executes exactly once without SOURCE counter masking");
 else if(PERIODIC_PHASE==="cleanup"){
  if(Probe.periodicSession!==null){Probe.periodicSession.close();Probe.periodicSession=null;}
  if(actor!==null){cls("combat.spi.CombatWorldLifecycleAccess").clear(cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),actor.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);actor.discard();}
 }
 return JSON.stringify({passed:checks.length,checks:checks});
})();
