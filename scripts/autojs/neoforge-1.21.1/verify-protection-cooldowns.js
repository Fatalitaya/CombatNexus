(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var checks=[];function check(v,l){if(!v)throw new Error(l);checks.push(l);}
    var Id=cls("api.id.NexusId"),type=Id.parse("combatnexus:lethal_protection"),guard=Id.parse("combatnexus:last_stand");
    var Ledger=cls("combat.internal.coordination.ProtectionUseLedger"),ledger=new Ledger();
    check(ledger.available(1,type,1,100),"fresh installation is available");
    ledger.consume(1,type,1200,100);
    check(!ledger.available(1,type,1,100)&&!ledger.available(1,type,1,1299),"same tick and last cooldown tick reject another use");
    check(ledger.available(1,type,1,1300),"exact 1200-tick boundary replenishes use");
    check(ledger.consumed(1).get(type).intValue()===1,"availability query does not mutate consumption");
    var restored=new Ledger();restored.restore(2,ledger.consumed(1),ledger.remaining(1,300),9000);
    check(!restored.available(2,type,1,9999)&&restored.available(2,type,1,10000),"remaining cooldown rebases rather than resetting or expiring offline");
    restored.consume(2,type,1200,10000);
    check(!restored.available(2,type,1,11199)&&restored.available(2,type,1,11200),"successful reuse starts another full cooldown");
    restored.restore(3,ledger.consumed(1),ledger.remaining(1,1400),20000);
    check(restored.available(3,type,1,20000),"already expired cooldown remains ready after save and restore");
    ledger.consume(4,type,0,100);check(!ledger.available(4,type,1,1000000),"zero cooldown explicitly selects one-use installation");
    ledger.remove(4);check(ledger.available(4,type,1,1000000),"actual source removal resets installation state");
    var BC=cls("combat.api.behavior.BehaviorComponent"),Json=Java.loadClass("com.google.gson.JsonParser");
    for(var ticks of [-1,1200001]){
        var rejected=false;try{BC.decode(Json.parseString(JSON.stringify({type:String(type),cooldown_ticks:ticks})).getAsJsonObject());}
        catch(e){rejected=String(e).indexOf("cooldown_ticks")>=0||String(e).indexOf("outside of range")>=0;}
        check(rejected,"out-of-range cooldown rejected: "+ticks);
    }
    var level=AutoJS.server().overworld(),world=cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var definition=world.behaviorDefinitions().get(guard);
    check(String(definition.lifetime().kind())==="PERMANENT"&&definition.plan().components().get(0).cooldownTicks()===1200,"built-in last stand lasts while equipped and defaults to 60 seconds");
    var cow=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level);
    cow.setPos(0,512,0);cow.setNoGravity(true);cow.setNoAi(true);level.addFreshEntity(cow);
    var Life=cls("combat.spi.CombatWorldLifecycleAccess"),Persistence=cls("combat.platform.CombatPersistence"),Codec=cls("combat.internal.persistence.CombatStateCodec");
    try{
        Persistence.tick(level);
        var stack=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").STICK),position=cls("api.equipment.EquipmentPosition").MAINHAND;
        var entry=new (cls("combat.api.definition.EntryTemplateSpec$Builder"))(guard).behavior(guard,guard).build().definition();
        stack.set(cls("combat.platform.CombatDataComponents").PATCH.get(),cls("combat.api.item.CombatItemBindingPatch").replaceAll(new (cls("combat.api.item.CombatItemBindingBuilder"))().entry(guard,entry,position).build()));
        cow.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,stack);
        var reconcile=function(){cls("combat.spi.CombatEquipmentAccess").changed(world,cow.getUUID(),position,cls("combat.platform.WorldEquipmentObservation").capture(level,stack));};
        var hit=function(amount){cow.invulnerableTime=0;cow.hurt(cow.damageSources().generic(),amount);};
        var save=function(){Persistence.save(cow);return String(cow.getPersistentData().getString("combatnexus:combat_state"));};
        reconcile();cow.setHealth(10);hit(1);
        check(world.behaviors().view(cow.getUUID()).get(0).protectionAvailable(),"nonlethal damage does not consume protection");
        check(Codec.decode(save()).behaviors().get(0).protectionRemaining().isEmpty(),"nonlethal damage does not start cooldown");
        cow.setHealth(5);hit(1000);
        check(cow.getHealth()===1&&!cow.isDeadOrDying(),"real native lethal damage is prevented");
        var saved=save(),progress=Codec.decode(saved).behaviors().get(0);
        check(progress.protectionRemaining().get(type).longValue()===1200,"successful protection saves full configured cooldown");
        var installation=world.behaviors().capture(cow.getUUID()).get(0).installation();reconcile();
        check(world.behaviors().capture(cow.getUUID()).get(0).installation()===installation&&!world.behaviors().view(cow.getUUID()).get(0).protectionAvailable(),"unchanged equipment does not reset cooldown");
        Life.clear(world,cow.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);Life.restore(world,cow.getUUID(),saved);reconcile();
        check(!world.behaviors().view(cow.getUUID()).get(0).protectionAvailable()&&Codec.decode(save()).behaviors().get(0).protectionRemaining().get(type).longValue()===1200,"session restore preserves exact cooldown without regranting protection");
        var expired=Json.parseString(save()).getAsJsonObject();expired.getAsJsonArray("behaviors").get(0).getAsJsonObject().getAsJsonObject("protectionRemaining").addProperty(String(type),Java.loadClass("java.lang.Long").valueOf(0));
        Life.clear(world,cow.getUUID(),cls("effect.api.EffectService$Removal").WORLD_EXIT);Life.restore(world,cow.getUUID(),String(expired));reconcile();
        cow.setHealth(5);hit(1000);
        check(cow.getHealth()===1&&Codec.decode(save()).behaviors().get(0).protectionRemaining().get(type).longValue()===1200,"expired protection triggers again and starts a new cooldown");
        cow.setHealth(5);hit(1000);check(cow.isDeadOrDying(),"second lethal hit during cooldown is not blocked");
        return JSON.stringify({passed:checks.length,checks:checks});
    }finally{cow.discard();}
})()
