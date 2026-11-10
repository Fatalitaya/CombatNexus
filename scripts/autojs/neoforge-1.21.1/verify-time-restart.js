(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld();cls("combat.platform.CombatPersistence").tick(level);var e=null;for(var value of level.getAllEntities())if(value.getTags().contains("combatnexus_time_restart"))e=value;if(e===null)return JSON.stringify({ready:false});
 var world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess"),UUID=Java.loadClass("java.util.UUID"),Optional=Java.loadClass("java.util.Optional"),Set=Java.loadClass("java.util.Set"),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),source=cls("api.id.NexusId").parse("probe:time_restart"),checks=[];
 function check(v,n){if(!v)throw new Error(n);checks.push(n);}
 var cancelledEntity=level.getEntity(UUID.fromString(RESTART_BEFORE.cancelled.target));if(cancelledEntity===null)return JSON.stringify({ready:false});
 check(!api.drivers().snapshot(e.getUUID()).get(0).bound(),"real process restart retains driver ownership without live handle");
 level.getLevelData().setGameTime(level.getGameTime()+50);Life.beginTick(world);Life.tickRecovery(world);
 check(api.time().now(e.getUUID(),source)===RESTART_BEFORE.clock&&e.getHealth()===2,"restored managed state stays frozen through world advancement");
 var effects=world.effects().snapshot(e.getUUID());check(effects.size()===1&&effects.get(0).snapshot().expiresAtTick().getAsLong()-api.time().now(e.getUUID(),source)===RESTART_BEFORE.remaining,"real restart retains exact remaining effect lifetime");
 var session=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,e.getUUID(),Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty());check(session.time().nextSequence()===1&&session.time().latest().get().completed(),"real restart exposes sequence high watermark");session.advance(0,UUID.fromString(RESTART_BEFORE.request),7);
 check(e.getHealth()===2&&api.time().now(e.getUUID(),source)===RESTART_BEFORE.clock,"request consumed before restart is not replayed after rebind");
 session.advance(1,UUID.randomUUID(),2);check(e.getHealth()===2,"periodic remaining delay survives restart");session.advance(2,UUID.randomUUID(),1);check(e.getHealth()===3,"periodic action resumes at its original remaining boundary");
 var cancelledSession=api.drivers().bind(Java.loadClass("probe.DriverExtension").timeDriver,cancelledEntity.getUUID(),Set.of(cls("combat.driver.api.DriverScope").TIME),Optional.empty());
 var cancelled=cancelledSession.time().latest().orElseThrow();
 check(cancelled.cancelled()&&!cancelled.completed()&&cancelled.consumed()===RESTART_BEFORE.cancelled.consumed&&cancelled.remaining()===RESTART_BEFORE.cancelled.remaining,"real restart retains cancellation and exact unconsumed balance");
 check(cancelledSession.advance(0,UUID.fromString(RESTART_BEFORE.cancelled.request),1200000).equals(cancelled)&&cancelledSession.time().tick()===RESTART_BEFORE.cancelled.clock,"real restart cannot replay cancelled increment");
 check(String(cancelledSession.release())==="RELEASED","restored cancelled increment releases without completing remaining time");
 Life.clear(world,cancelledEntity.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);cancelledEntity.discard();
 Life.clear(world,e.getUUID(),cls("effect.api.EffectService").Removal.WORLD_EXIT);e.discard();return JSON.stringify({ready:true,passed:checks.length,checks:checks});
})()
