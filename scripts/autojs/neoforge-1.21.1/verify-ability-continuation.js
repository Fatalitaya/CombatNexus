(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var UUID=Java.loadClass("java.util.UUID"),Probe=Java.loadClass("probe.AbilityExtension"),level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess"),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),checks=[],entities=[];
 function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 function find(tag){for(var e of level.getAllEntities())if(e.getTags().contains(tag)){entities.push(e);return e;}throw new Error("Missing staged entity "+tag);}
 function tag(e,prefix){for(var t of e.getTags())if(String(t).indexOf(prefix)===0)return UUID.fromString(String(t).substring(prefix.length));throw new Error("Missing tag "+prefix);}
 try {
  for(var mode of ["complete","cancel","revoke"]){
   var actor=find("ability_delay_actor_"+mode),target=find("ability_delay_target_"+mode),snapshot=api.abilities().execution(tag(actor,"ability_execution_")).orElseThrow(),action=tag(actor,"ability_action_");
   if(mode==="complete"){
    check(String(snapshot.status())==="COMPLETED"&&snapshot.step()===1&&target.getHealth()===5,"delayed continuation applies exactly once after due tick");
    check(snapshot.action().id().equals(action)&&Probe.actions().stream().filter(function(a){return a.equals(action);}).count()===2,"initial and resumed callbacks preserve the same action");
    check(snapshot.commands().stream().allMatch(function(c){return c.context().orElseThrow().action().orElseThrow().id().equals(action);}),"continuation command receipts retain the original action");
   } else check(String(snapshot.status())==="CANCELLED"&&target.getHealth()===1,"pending "+mode+" preserves already-applied state without future healing");
  }
  check(Probe.expiredContext(),"resumed callback capability expires");return JSON.stringify({passed:checks.length,checks:checks});
 } finally {for(var e of entities){Life.clear(world,e.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);e.discard();}}
})();
