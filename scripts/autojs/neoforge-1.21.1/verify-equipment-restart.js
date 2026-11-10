(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld();cls("combat.platform.CombatPersistence").tick(level);var e=null;for(var value of level.getAllEntities())if(value.getTags().contains("equipment_restart"))e=value;if(e===null)return JSON.stringify({ready:false});
 var world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess"),state=JSON.parse(String(Life.save(world,e.getUUID()))),checks=[];
 function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
 check(AutoJS.server().tickRateManager().isFrozen()&&level.getGameTime()===RESTART_BEFORE.gameTick,"fresh process compares exact frozen equipment state");
 check(world.equipmentSelection(e.getUUID()).selected().size()===1,"restoration publishes authoritative selection before the first damage request");
 check(state.shields[0].balance===7&&state.shields[0].grant.expiresAtTick===RESTART_BEFORE.state.shields[0].grant.expiresAtTick,"restart retains paused shield balance and remaining lifetime");
 check(state.recovery[0].ticks===7&&state.recovery[0].accrued===.35,"restart retains paused recovery fraction");
 check(JSON.stringify(state.behaviors)===JSON.stringify(RESTART_BEFORE.state.behaviors),"restart retains consumed guard and exact periodic phase");
 check(cls("combat.internal.persistence.CombatStateCodec").decode(JSON.stringify(state)).equipment().equals(cls("combat.internal.persistence.CombatStateCodec").decode(JSON.stringify(RESTART_BEFORE.state)).equipment()),"restart retains equipment installation identities and selection");
 check(state.sources.filter(function(s){return s.suspended;}).length===5,"restart restores all suspended source flags");
 var NS=Java.loadClass("net.minecraft.world.entity.EquipmentSlot"),empty=Java.loadClass("net.minecraft.world.item.ItemStack").EMPTY;e.setItemSlot(NS.OFFHAND,empty);cls("combat.spi.CombatEquipmentAccess").changed(world,e.getUUID(),cls("api.equipment.EquipmentPosition").OFFHAND,cls("combat.platform.WorldEquipmentObservation").capture(level,empty));
 var resumed=JSON.parse(String(Life.save(world,e.getUUID())));
 check(e.getHealth()===1&&resumed.shields.length===1&&resumed.shields[0].balance===7,"resumption after process restart does not replay health or shield grants");
 check(JSON.stringify(resumed.behaviors)===JSON.stringify(state.behaviors),"resumption retains consumed protection and periodic phase");
 check(resumed.sources.every(function(s){return !s.suspended;}),"winning again reactivates every surviving contribution");
 Life.clear(world,e.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);e.discard();return JSON.stringify({ready:true,passed:checks.length,checks:checks});
})()
