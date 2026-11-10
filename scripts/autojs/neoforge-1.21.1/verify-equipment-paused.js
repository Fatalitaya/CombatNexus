(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld(),e=null;for(var value of level.getAllEntities())if(value.getTags().contains("equipment_restart"))e=value;
 if(e===null)throw new Error("Missing equipment entity");var world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),state=JSON.parse(String(cls("combat.spi.CombatWorldLifecycleAccess").save(world,e.getUUID()))),checks=[];
 function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
 check(level.getGameTime()===RESTART_BEFORE.gameTick+25,"paused equipment observes twenty-five real world ticks");
 check(e.getHealth()===1,"paused periodic healing does not execute or catch up");
 check(state.shields.length===1,"short-lived paused shield expires on its original deadline");
 check(state.shields[0].balance===7,"paused shield does not recover");
 check(state.recovery[0].accrued===.35&&state.recovery[0].ticks===7,"paused recovery retains fractional progress without accumulating");
 var phase=null;for(var b of state.behaviors)if(b.periodicRemaining["combatnexus:periodic_healing"]!==undefined)phase=b.periodicRemaining["combatnexus:periodic_healing"];
 var guard=state.behaviors.filter(function(b){return b.effect==="combatnexus:last_stand";})[0];
 check(guard.protectionRemaining["combatnexus:lethal_protection"]===1175,"suspended protection cooldown advances with twenty-five actual world ticks");
 check(phase===2,"paused periodic phase advances past missed pulse without replay");
 var beforeShield=null;for(var pool of RESTART_BEFORE.state.shields)if(pool.grant.source.id===state.shields[0].grant.source.id)beforeShield=pool;
 check(beforeShield!==null&&state.shields[0].grant.expiresAtTick===beforeShield.grant.expiresAtTick,"suspension does not extend shield deadline");
 cls("combat.platform.CombatPersistence").save(e);return JSON.stringify({prepared:true,passed:checks.length,checks:checks,gameTick:level.getGameTime(),health:e.getHealth(),state:state});
})()
