(function(){
 function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
 var cost=null,checks=[],Id=cls("api.id.NexusId"),List=Java.loadClass("java.util.List"),Map=Java.loadClass("java.util.HashMap"),Set=Java.loadClass("java.util.Set"),Json=Java.loadClass("com.google.gson.JsonParser"),Ops=Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE;
 function id(s){return Id.parse(s.indexOf(":")>=0?s:"equipment_probe:"+s);}function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
 function decode(type,value){return cls(type).CODEC.parse(Ops,Json.parseString(JSON.stringify(value))).getOrThrow();}
 var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level),Life=cls("combat.spi.CombatWorldLifecycleAccess");Life.beginTick(world);
 var Definitions=cls("combat.internal.definition.CombatDefinitions"),Generation=cls("combat.internal.definition.DefinitionGeneration"),kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()),original=world.definitions();
 var shields=new Map(original.domains().shields()),Shield=cls("combat.shield.api.ShieldDefinition");
 function shield(capacity){return new Shield(capacity,0,Set.of(id("minecraft:player_attack")),Set.of(),1000,1,Shield.Stacking.REPLACE_SAME_SOURCE);}
 shields.put(id("shield"),shield(10));
 var effects=new Map(original.domains().effects());effects.put(id("instant"),new (cls("effect.api.EffectDefinition"))(0,cls("effect.api.EffectLifetime").instant(),cls("effect.api.EffectService$Stacking").REPLACE,cls("effect.api.EffectPolicy").DEFAULT,List.of(),new (cls("combat.api.behavior.BehaviorPlan"))(0,List.of(new (cls("combat.api.behavior.BuiltinBehaviors$InstantHealing"))(1))),Set.of()));
 var domains=new (cls("combat.api.DomainDefinitions"))(original.domains().healingRules(),original.domains().regeneration(),shields,effects,original.domains().abilities());
 function publish(scope){var settings=original.settings();Definitions.publish(kernel,new Generation(Definitions.nextGeneration(),new (cls("combat.config.CombatServerSettings"))(settings.formulas(),settings.vanillaCompatibility(),settings.safety(),new (cls("combat.config.EquipmentSettings"))(cls("combat.config.EquipmentSettings$StackingScope")[scope])),original.damage(),original.behaviors(),domains,original.itemBindings()));}
 var entity=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);entity.setPos(0,512,0);entity.setNoGravity(true);entity.setNoAi(true);level.addFreshEntity(entity);cls("combat.platform.CombatPersistence").tick(level);
 var Slot=cls("api.equipment.EquipmentPosition"),NS=Java.loadClass("net.minecraft.world.entity.EquipmentSlot"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items"),Builder=cls("combat.api.item.CombatItemBindingBuilder"),Patch=cls("combat.api.item.CombatItemBindingPatch"),Component=cls("combat.platform.CombatDataComponents").PATCH.get();
 var entry={id:"equipment_probe:mixed",display:{},stacking:"unique_entry",contributions:[{id:"equipment_probe:damage",kind:"DAMAGE",source:"weapon",rule:{id:"equipment_probe:damage",phase:"base_modification",operations:[{type:"combatnexus:add_base_damage",channel:"combatnexus:physical",value:3}]}},{id:"equipment_probe:attribute",kind:"ATTRIBUTE",attribute:"combatnexus:lifesteal",amount:.1,operation:"ADD_VALUE"},{id:"equipment_probe:guard",kind:"BEHAVIOR",definition:"combatnexus:last_stand"},{id:"equipment_probe:shield",kind:"SHIELD",definition:"equipment_probe:shield",amount:10,duration_ticks:1000},{id:"equipment_probe:recovery",kind:"REGENERATION",definition:"combatnexus:field_recovery"},{id:"equipment_probe:instant",kind:"EFFECT",definition:"equipment_probe:instant"}]};
 function affix(name,rarity,members){return decode("combat.api.affix.AffixDefinition",{id:"equipment_probe:"+name,display:{},rarity:rarity,stacking:"highest_rarity",stacking_group:"equipment_probe:vampire",members:members||[{id:"equipment_probe:first",entry:entry}]});}
 function item(def){var stack=new Stack(Items.DIAMOND_SWORD);stack.set(Component,Patch.replaceAll(new Builder().affix(id("binding"),def,Slot.MAINHAND,Slot.OFFHAND).build()));return stack;}
 function changed(slot,stack){entity.setItemSlot(slot,stack);cls("combat.spi.CombatEquipmentAccess").changed(world,entity.getUUID(),cls("platform.NativeEquipment").position(slot),cls("combat.platform.WorldEquipmentObservation").capture(level,stack));}
 function refresh(){changed(NS.MAINHAND,entity.getMainHandItem());}
 function pools(){return world.shields().snapshot(entity.getUUID());}function selected(){return world.equipmentSelection(entity.getUUID()).selected().size();}
 var malformed=JSON.parse(JSON.stringify(entry));malformed.contributions[0].unexpected=true;
 check(cls("combat.api.entry.EntryDefinition").CODEC.parse(Ops,Json.parseString(JSON.stringify(malformed))).error().isPresent(),"inline codec rejects unknown contribution fields");
 var nested={id:"probe:outer",display:{},members:[{id:"probe:member",affix:{}}]};
 check(cls("combat.api.affix.AffixDefinition").CODEC.parse(Ops,Json.parseString(JSON.stringify(nested))).error().isPresent(),"nested affix is rejected by the instance codec");
 var low=item(affix("low","common")),high=item(affix("high","epic"));
 try{
  publish("ENTITY_EQUIPMENT");entity.setHealth(1);changed(NS.MAINHAND,low);
  check(entity.getHealth()===2,"first selection applies instant contribution exactly once");
  check(selected()===1&&pools().size()===1&&JSON.parse(String(Life.save(world,entity.getUUID()))).recovery.length===1,"mixed affix installs damage, attribute, behavior, shield and recovery as one selected binding");
  var victim=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);victim.setPos(2,512,0);victim.setNoAi(true);victim.setNoGravity(true);level.addFreshEntity(victim);cls("combat.platform.CombatPersistence").tick(level);
  try{var beforeHealth=victim.getHealth();victim.hurtServer(level,entity.damageSources().mobAttack(entity),1);check(Math.abs(victim.getHealth()-(beforeHealth-4))<.001,"selected mixed affix damage contribution executes through the native damage pipeline");}finally{Life.clear(world,victim.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);victim.discard();}
  var installation=world.equipmentSelection(entity.getUUID()).candidates().get(0).installation(),poolSource=pools().get(0).source().id(),deadline=pools().get(0).expiresAtTick().getAsLong();
  var facade=cls("combat.api.CombatAccess").find(level).orElseThrow();
  check(Java.loadClass("java.util.Objects").equals(facade.entities(),facade.entities())&&Java.loadClass("java.util.Objects").equals(facade.effects(),facade.effects())&&Java.loadClass("java.util.Objects").equals(facade.cooldowns(),facade.cooldowns())&&Java.loadClass("java.util.Objects").equals(facade.scheduler(),facade.scheduler())&&Java.loadClass("java.util.Objects").equals(facade.states(),facade.states()),"domain facade returns stable command services");
  var stableSelection=world.equipmentSelection(entity.getUUID()),System=Java.loadClass("java.lang.System");
  for(var stableTick=0;stableTick<100;stableTick++)refresh();
  check(System.identityHashCode(stableSelection)===System.identityHashCode(world.equipmentSelection(entity.getUUID())),"unchanged equipment reuses compiled selection across 100 observations");
  var frozen=cls("combat.platform.NativeEntityCapture").capture(entity),oldCount=frozen.equipment().get(Slot.MAINHAND).count();
  var stackCopy=frozen.equipment().get(Slot.MAINHAND).copyStack();stackCopy.setCount(oldCount+1);
  check(frozen.equipment().get(Slot.MAINHAND).count()===oldCount,"returned native copy cannot mutate frozen capture");
  entity.getMainHandItem().setCount(oldCount+1);
  var nextCapture=cls("combat.platform.NativeEntityCapture").capture(entity);
  check(frozen.equipment().get(Slot.MAINHAND).count()===oldCount&&nextCapture.equipment().get(Slot.MAINHAND).count()===oldCount+1,"same tick requests sample new stack state without changing previous input");
  entity.getMainHandItem().setCount(oldCount);
  var captured=world.equipmentSelection(entity.getUUID()).capture(new (cls("combat.api.input.CapturedItem"))(low).facts(),Slot.MAINHAND);
  check(captured.affixes().get().get(0).entries().get(0).contributions().size()===1&&captured.bindingPatch().equals(Patch.EMPTY),"projectile capture contains only selected damage contributions");
  world.shields().reserve(entity.getUUID(),91001,id("minecraft:player_attack"),6).commit();check(pools().get(0).balance()===4,"shield fixture consumes six units");
  entity.setHealth(5);entity.invulnerableTime=0;entity.hurtServer(level,entity.damageSources().generic(),1000);
  check(entity.getHealth()===1&&!world.behaviors().view(entity.getUUID()).get(0).protectionAvailable(),"guard fixture consumes one-shot protection");
  changed(NS.OFFHAND,high);check(selected()===1&&pools().size()===2,"higher rarity wins across slots while losing shield remains owned");
  check(world.sourceInstallations().snapshot(entity.getUUID()).stream().filter(function(s){return s.suspended();}).count()===5,"all persistent loser contributions are suspended together");
  var hold=pools().stream().filter(function(p){return p.source().id().equals(poolSource);}).findFirst().get();check(hold.balance()===4&&hold.expiresAtTick().getAsLong()===deadline,"competition preserves consumed shield balance and deadline");
  var healthBeforeResume=entity.getHealth();changed(NS.OFFHAND,Stack.EMPTY);check(entity.getHealth()===healthBeforeResume,"resuming does not replay completed instant healing");check(selected()===1&&pools().size()===1&&pools().get(0).balance()===4,"winning again resumes existing shield without refilling");
  check(!world.behaviors().view(entity.getUUID()).get(0).protectionAvailable(),"winning again does not reset consumed protection");
  changed(NS.MAINHAND,low.copy());check(world.equipmentSelection(entity.getUUID()).candidates().get(0).installation().equals(installation),"equivalent stack observation preserves installation UUID");
  changed(NS.OFFHAND,high);publish("ITEM");refresh();check(selected()===2&&world.behaviors().capture(entity.getUUID()).size()===2,"ITEM scope independently selects both slot sources");
  publish("ENTITY_EQUIPMENT");refresh();check(selected()===1&&world.behaviors().capture(entity.getUUID()).size()===1,"configuration adoption reselects whole affixes across equipment");
  check(captured.affixes().get().get(0).id().equals(id("low")),"previous projectile damage facts survive scope and equipment changes");
  changed(NS.OFFHAND,Stack.EMPTY);shields.put(id("shield"),shield(3));domains=new (cls("combat.api.DomainDefinitions"))(domains.healingRules(),domains.regeneration(),shields,domains.effects(),domains.abilities());publish("ENTITY_EQUIPMENT");refresh();check(pools().get(0).balance()===3&&pools().get(0).expiresAtTick().getAsLong()===deadline,"reload capacity decrease clamps balance without resetting lifetime");
  shields.put(id("shield"),shield(30));domains=new (cls("combat.api.DomainDefinitions"))(domains.healingRules(),domains.regeneration(),shields,domains.effects(),domains.abilities());publish("ENTITY_EQUIPMENT");refresh();check(pools().get(0).balance()===3,"reload capacity increase does not replenish balance");
  changed(NS.MAINHAND,Stack.EMPTY);check(pools().isEmpty()&&world.effects().snapshot(entity.getUUID()).isEmpty()&&JSON.parse(String(Life.save(world,entity.getUUID()))).recovery.length===0,"uninstall revokes all domain contributions");
  changed(NS.MAINHAND,low);check(!world.equipmentSelection(entity.getUUID()).candidates().get(0).installation().equals(installation)&&pools().get(0).balance()===10,"actual re-equip creates a new installation and first grant");
  changed(NS.MAINHAND,Stack.EMPTY);var repeated=item(affix("repeated","epic",[{id:"equipment_probe:first",entry:entry},{id:"equipment_probe:second",entry:entry}]));changed(NS.MAINHAND,repeated);
  check(pools().size()===2&&world.sourceInstallations().snapshot(entity.getUUID()).size()===10,"affix members bypass independent entry competition and retain distinct sources");
  var repeatCapture=world.equipmentSelection(entity.getUUID()).capture(new (cls("combat.api.input.CapturedItem"))(repeated).facts(),Slot.MAINHAND);
  check(!repeatCapture.affixes().get().get(0).entries().get(0).contributions().get(0).id().equals(repeatCapture.affixes().get().get(0).entries().get(1).contributions().get(0).id()),"duplicate template members have distinct captured damage identities");
  var tie=item(affix("tie","epic"));changed(NS.OFFHAND,tie);check(world.equipmentSelection(entity.getUUID()).active(Slot.MAINHAND).size()===1,"equal rarity uses stable equipment position order");
  var arrow=Java.loadClass("net.minecraft.world.entity.EntityType").ARROW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);arrow.setOwner(entity);arrow.setPos(0,514,0);arrow.setNoGravity(true);level.addFreshEntity(arrow);
  var launch=cls("combat.api.CombatAccess").find(level).orElseThrow().projectiles().capture(Java.loadClass("java.util.UUID").randomUUID(),entity.getUUID(),arrow).value().orElseThrow();
  changed(NS.MAINHAND,Stack.EMPTY);changed(NS.OFFHAND,Stack.EMPTY);
  var arrowVictim=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);arrowVictim.setPos(3,512,0);arrowVictim.setNoGravity(true);arrowVictim.setNoAi(true);level.addFreshEntity(arrowVictim);cls("combat.platform.CombatPersistence").tick(level);
  try{var arrowHealth=arrowVictim.getHealth();arrowVictim.hurtServer(level,arrowVictim.damageSources().arrow(arrow,entity),1);check(Math.abs(arrowVictim.getHealth()-(arrowHealth-7))<.001,"projectile executes both captured affix members after shooter unequips: health="+arrowVictim.getHealth()+" before="+arrowHealth);check(world.sourceInstallations().snapshot(entity.getUUID()).isEmpty(),"projectile damage does not reinstall persistent equipment contributions");}finally{arrow.discard();Life.clear(world,arrowVictim.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);arrowVictim.discard();}
  entity.setHealth(1);changed(NS.MAINHAND,repeated);
  var standalone=new Stack(Items.DIAMOND_SWORD);standalone.set(Component,Patch.replaceAll(new Builder().entry(id("standalone"),decode("combat.api.entry.EntryDefinition",entry),Slot.OFFHAND).build()));changed(NS.OFFHAND,standalone);
  check(selected()===2&&pools().size()===3,"standalone Entry competes separately from identical Affix members: selected="+selected()+" pools="+pools().size());
  var savedEquipment=cls("combat.internal.persistence.CombatStateCodec").decode(Life.save(world,entity.getUUID())).equipment();
  var observation=new (cls("combat.api.platform.EquipmentObservation"))({alive:function(){return true;},item:function(target,slot){
   var bindings=new (Java.loadClass("java.util.ArrayList"))(),itemId=null;
   for(var value of savedEquipment)if(value.slot().equals(slot)){bindings.add(value.binding());itemId=value.item();}
   var Optional=Java.loadClass("java.util.Optional");
   return itemId===null?Optional.empty():Optional.of(new (cls("combat.api.behavior.EquipmentItem"))(itemId,new (cls("combat.api.item.CombatItemBindingSpec"))(bindings),Optional.empty()));
  }});
  var projectionController=new (cls("combat.internal.coordination.EquipmentBehaviorController"))(world,observation);projectionController.restore(entity.getUUID(),savedEquipment);
  cost=JSON.parse(String(Java.loadClass("probe.PresentationCost").measure(function(){return projectionController.save(entity.getUUID());},function(){return projectionController.projection(entity.getUUID());})));
  cost.equipment=savedEquipment.size();cost.contributionPlans=0;for(var value of savedEquipment)cost.contributionPlans+=value.contributions().size();
  var Cost=Java.loadClass("probe.PresentationCost"),observed=new (Java.loadClass("java.util.HashMap"))();
  for(var nativeSlot of NS.values())observed.put(cls("platform.NativeEquipment").position(nativeSlot),cls("combat.platform.WorldEquipmentObservation").capture(level,entity.getItemBySlot(nativeSlot)));
  cost.bindingCompileOnly=JSON.parse(String(Cost.sample(function(){return cls("combat.internal.definition.EquipmentSelection").resolve(observed,world.definitions());},200,1000)));
  cost.forcedRecomputeObservation=JSON.parse(String(Cost.equipment(world,entity.getUUID(),false)));
  cost.cachedEquipmentObservation=JSON.parse(String(Cost.equipment(world,entity.getUUID(),true)));
  cost.noSubscriberFlush=JSON.parse(String(Cost.sample(function(){cls("presentation.platform.CombatPresentationNetwork").flushMirrors(level);return null;},200,1000)));
  cost.detailedPanelCapture=JSON.parse(String(Cost.sample(function(){return cls("presentation.platform.NativeEntityCombatMirror").capture(world,entity);},200,1000)));
  var current=cls("presentation.platform.NativeEntityCombatMirror").capture(world,entity),previous=current.deepCopy(),oldRows=[];
  for(var value of projectionController.save(entity.getUUID()))oldRows.push({installation:String(value.installation()),slot:String(value.slot().getName()),binding:String(value.binding().id()),definition:String(value.binding().definition()),state:value.selected()?"ACTIVE":"COMPETING"});
  previous=String(Java.loadClass("probe.PresentationCost").withEquipment(String(current),JSON.stringify(oldRows)));check(previous===String(current),"equipment projection preserves complete supplement content");
  var payloadBytes=function(json){
   var Payload=cls("presentation.platform.network.CombatPresentationPayload"),payload=new Payload(Java.loadClass("java.util.UUID").randomUUID(),world.sessionId(),cls("platform.NativeIds").nexus(level.dimension().identifier()),world.definitions().generation(),entity.getId(),entity.getUUID(),entity.combatnexus$incarnation(),1,false,String(json));
   var buffer=new (Java.loadClass("net.minecraft.network.RegistryFriendlyByteBuf"))(Java.loadClass("io.netty.buffer.Unpooled").buffer(),level.registryAccess());
   try{Payload.STREAM_CODEC.encode(buffer,payload);return buffer.readableBytes();}finally{buffer.release();}
  }
  cost.oldPayloadBytes=payloadBytes(previous);cost.newPayloadBytes=payloadBytes(current);
  check(cost.newAllocatedBytes<cost.oldAllocatedBytes&&cost.newPayloadBytes===cost.oldPayloadBytes,"equipment projection reduces allocations and preserves encoded payload bytes");
 }finally{Life.clear(world,entity.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);entity.discard();Definitions.publish(kernel,new Generation(Definitions.nextGeneration(),original.settings(),original.damage(),original.behaviors(),original.domains(),original.itemBindings()));}
 return JSON.stringify({passed:checks.length,checks:checks,cost:cost});
})()
