(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess");
 AutoJS.server().overworld().setChunkForced(0,0,true);Life.beginTick(world);
 for(var old of level.getAllEntities())if(old.getTags().contains("equipment_restart"))old.discard();
 var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level);e.setPos(0,256,0);e.setNoGravity(true);e.setNoAi(true);e.addTag("equipment_restart");level.addFreshEntity(e);cls("combat.platform.CombatPersistence").tick(level);
 var Id=cls("api.id.NexusId"),Json=Java.loadClass("com.google.gson.JsonParser"),Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,Builder=cls("combat.api.item.CombatItemBindingBuilder"),Patch=cls("combat.api.item.CombatItemBindingPatch"),Slot=cls("api.equipment.EquipmentPosition"),NS=Java.loadClass("net.minecraft.world.entity.EquipmentSlot"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
 function item(high){var contributions=high?[{id:"probe:unused",kind:"ATTRIBUTE",attribute:"combatnexus:lifesteal",amount:0,operation:"ADD_VALUE"}]:[{id:"probe:periodic",kind:"BEHAVIOR",definition:"combatnexus:periodic_healing"},{id:"probe:guard",kind:"BEHAVIOR",definition:"combatnexus:last_stand"},{id:"probe:shield",kind:"SHIELD",definition:"combatnexus:physical_guard",amount:10,duration_ticks:1000},{id:"probe:recovery",kind:"REGENERATION",definition:"combatnexus:field_recovery"},{id:"probe:expired",kind:"SHIELD",definition:"combatnexus:physical_guard",amount:3,duration_ticks:10}];
 var affix=cls("combat.api.affix.AffixDefinition").CODEC.parse(Ops,Json.parseString(JSON.stringify({id:high?"probe:high":"probe:low",display:{},rarity:high?"epic":"common",stacking:"highest_rarity",stacking_group:"probe:restart",members:[{id:"probe:member",entry:{id:"probe:mixed",display:{},contributions:contributions}}]}))).getOrThrow();var stack=new Stack(Items.DIAMOND_SWORD);stack.set(cls("combat.platform.CombatDataComponents").PATCH.get(),Patch.replaceAll(new Builder().affix(Id.parse("probe:binding"),affix,Slot.MAINHAND,Slot.OFFHAND).build()));return stack;}
 function equip(slot,stack){e.setItemSlot(slot,stack);cls("combat.spi.CombatEquipmentAccess").changed(world,e.getUUID(),cls("platform.NativeEquipment").position(slot),cls("combat.platform.WorldEquipmentObservation").capture(level,stack));}
 equip(NS.MAINHAND,item(false));e.setHealth(5);e.invulnerableTime=0;e.hurt(e.damageSources().generic(),1000);if(e.getHealth()!==1)throw new Error("Protection not consumed");
 var state=JSON.parse(String(Life.save(world,e.getUUID())));for(var pool of state.shields)if(pool.grant.expiresAtTick>level.getGameTime()+100)pool.balance=7;
 state.recovery[0].accrued=.35;state.recovery[0].ticks=7;
 for(var behavior of state.behaviors)if(behavior.periodicRemaining["combatnexus:periodic_healing"]!==undefined)behavior.periodicRemaining["combatnexus:periodic_healing"]=7;
 Life.clear(world,e.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);Life.restore(world,e.getUUID(),JSON.stringify(state));equip(NS.OFFHAND,item(true));
 cls("combat.platform.CombatPersistence").save(e);
 return JSON.stringify({prepared:true,gameTick:level.getGameTime(),state:JSON.parse(String(Life.save(world,e.getUUID()))),health:e.getHealth()});
})()
