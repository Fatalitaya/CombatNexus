(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess"),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Set=Java.loadClass("java.util.Set"),e=null;Life.beginTick(world);
 for(var value of level.getAllEntities())if(value.entityTags().contains("combatnexus_time_budget_probe")){e=value;break;}
 if(e===null)throw new Error("budget fixture unavailable");
 try{
 var data=e.getPersistentData(),request=UUID.fromString(data.getString("time_request").orElseThrow()),ticks=data.getLong("time_ticks").orElseThrow(),start=data.getLong("time_start").orElseThrow(),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
 var session=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,e.getUUID(),Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty());
 var status=session.time();if(status.latest().isEmpty()||!status.latest().get().request().equals(request)||status.nextSequence()!==1)throw new Error("pending progress cannot be queried after rebind");
 var r=session.advance(0,request,ticks);
 if(!r.completed()||api.time().now(e.getUUID(),cls("api.id.NexusId").parse("probe:budget"))!==start+ticks)throw new Error("pending advancement lost or duplicated ticks: receipt="+r+",start="+start+",clock="+api.time().now(e.getUUID(),cls("api.id.NexusId").parse("probe:budget"))+",health="+e.getHealth());
 if(e.getHealth()!==1+ticks/4)throw new Error("budget pause skipped or replayed periodic healing: "+e.getHealth()+" for "+ticks);
 session.advance(0,request,ticks);if(api.time().now(e.getUUID(),cls("api.id.NexusId").parse("probe:budget"))!==start+ticks)throw new Error("completed retry replayed");
 var cancelRequest=UUID.randomUUID(),pending=session.advance(1,cancelRequest,1200000);
 if(pending.terminal()||pending.remaining()<=0)throw new Error("cancel fixture did not exhaust budget");
 var beforeCancel=session.time().tick(),healthBeforeCancel=e.getHealth(),rejected=false;
 try{session.cancelAdvance(1,UUID.randomUUID(),1200000);}catch(error){if(String(error).indexOf("IllegalArgumentException")<0)throw error;rejected=true;}
 if(!rejected||!session.time().latest().get().equals(pending))throw new Error("wrong cancellation changed pending request");
 var cancelled=session.cancelAdvance(1,cancelRequest,1200000);
 if(!cancelled.cancelled()||cancelled.completed()||!cancelled.terminal()||cancelled.consumed()!==pending.consumed()||cancelled.remaining()!==pending.remaining())throw new Error("cancellation falsified time progress");
 if(!session.cancelAdvance(1,cancelRequest,1200000).equals(cancelled)||!session.advance(1,cancelRequest,1200000).equals(cancelled)||session.time().tick()!==beforeCancel||e.getHealth()!==healthBeforeCancel)throw new Error("cancelled retry advanced time or replayed healing");
 var saved=Life.save(world,e.getUUID());Life.clear(world,e.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);Life.restore(world,e.getUUID(),saved);
 session=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,e.getUUID(),Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty());
 if(!session.time().latest().get().equals(cancelled)||!session.advance(1,cancelRequest,1200000).equals(cancelled))throw new Error("restore lost cancellation terminal");
 if(String(session.release())!=="RELEASED"||!session.released()||String(session.release())!=="RELEASED")throw new Error("cancelled increment prevented idempotent release");
 return JSON.stringify({passed:9,checks:["budget pause does not skip or replay due healing","pending progress is queryable after rebind","later server budget resumes remaining ticks exactly once","completed pending retry remains idempotent","wrong cancellation preserves request","cancellation retains consumed and remaining progress","cancelled retry cannot advance or heal","restored cancellation stays terminal","cancelled increment permits idempotent release"]});
 }finally{Life.clear(world,e.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);e.discard();}
})()
