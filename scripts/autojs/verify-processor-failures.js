(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var checks=[];function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 var level=AutoJS.server().overworld(),Probe=Java.loadClass("probe.ProcessorFailureProbe");
 var entity=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
 entity.setPos(0,512,0);entity.setNoGravity(true);entity.setNoAi(true);level.addFreshEntity(entity);
 cls("combat.platform.CombatPersistence").tick(level);
 var kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server());
 try {
  Probe.target=entity.getUUID();Probe.continued=0;
  for(var i=0;i<70;i++){entity.setHealth(10);entity.invulnerableTime=0;entity.hurtServer(level,entity.damageSources().generic(),1);}
  check(Math.abs(entity.getHealth()-9)<.001,"failed apply restores damage workspace and removes appended healing plan");
  check(Probe.continued===70,"subsequent processor runs after both callback failures");
  var failures=kernel.processorFailures();check(failures.size()===128,"server session retains exactly 128 latest failures");
  var predicate=null,apply=null;for(var failure of failures){if(String(failure.callback())==="canHandle")predicate=failure;else if(String(failure.processor())==="probe:apply_failure")apply=failure;}
  check(String(predicate.processor())==="probe:can_handle_failure"&&String(predicate.callback())==="canHandle","predicate failure identifies processor and callback");
  check(String(apply.processor())==="probe:apply_failure"&&String(apply.callback())==="apply"&&String(apply.phase())==="BASE_MODIFICATION","apply failure identifies processor and phase");
  check(apply.request()===predicate.request()&&String(apply.exceptionType())==="java.lang.IllegalArgumentException"&&String(apply.message()).length===512,"diagnostic request correlation and message truncation");
 } finally {Probe.target=null;entity.discard();}
 return JSON.stringify({passed:checks.length,checks:checks,checkpointCost:JSON.parse(String(Probe.checkpointCost))});
})()
