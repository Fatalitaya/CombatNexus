// This suite exercises the 26.1.2 item-blocking API.
(function(){
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var Example=Java.loadClass("probe.EffectExtension"),Block=Java.loadClass("probe.ItemBlockExtension");
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow();
    var oldDifficulty=level.getDifficulty();
    var UUID=Java.loadClass("java.util.UUID"),Id=cls("api.id.NexusId"),Limit=cls("combat.api.action.TriggerLimit");
    var Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
    var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS,Priority=Java.loadClass("net.neoforged.bus.api.EventPriority");
    var Consumer=Java.loadClass("java.util.function.Consumer"),checks=[],settlements=[],mode="normal",installation=null;
    var player=new (Java.loadClass("net.minecraft.server.level.ServerPlayer"))(AutoJS.server(),level,new (Java.loadClass("com.mojang.authlib.GameProfile"))(UUID.randomUUID(),"CNBlockVerifier"),Java.loadClass("net.minecraft.server.level.ClientInformation").createDefault());
    player.connection=Example.localConnection(player);player.connection.markClientLoaded();player.setPos(0,512,0);level.addNewPlayer(player);
    var attacker=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);attacker.setNoAi(true);attacker.setNoGravity(true);attacker.setPos(0,512,1);level.addFreshEntity(attacker);
    var listener=new Consumer({accept:function(event){settlements.push(event.snapshot());}});
    var blockListener=new Consumer({accept:function(event){
        if(!event.getEntity().getUUID().equals(player.getUUID()))return;
        if(mode==="cancel")event.setBlocked(false);
        if(mode==="partial")event.setBlockedDamage(2);
        if(mode==="zero")event.setBlockedDamage(0);
    }});
    var failureListener=new Consumer({accept:function(event){if(mode==="throw"&&event.getEntity().getUUID().equals(player.getUUID()))throw new Error("Intentional native failure after block");}});
    bus.addListener(Priority.NORMAL,false,cls("combat.damage.platform.api.event.DamageSettledEvent"),listener);
    bus.addListener(Priority.LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent"),blockListener);
    bus.addListener(Priority.LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre"),failureListener);
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    function result(start){for(var k=start;k<settlements.length;k++)if(settlements[k].target().entity().equals(player.getUUID()))return settlements[k];throw new Error("Missing block settlement");}
    function reset(){player.setHealth(10);player.setPos(0,512,0);player.invulnerableTime=0;attacker.setPos(0,512,1);attacker.setHealth(attacker.getMaxHealth());attacker.invulnerableTime=0;}
    function hit(amount){return player.hurtServer(level,player.damageSources().mobAttack(attacker),amount);}
    function raise(shield){
        var Components=Java.loadClass("net.minecraft.core.component.DataComponents"),Blocks=Java.loadClass("net.minecraft.world.item.component.BlocksAttacks"),blocking=shield.get(Components.BLOCKS_ATTACKS);
        shield.set(Components.BLOCKS_ATTACKS,new Blocks(0,blocking.disableCooldownScale(),blocking.damageReductions(),blocking.itemDamage(),blocking.bypassedBy(),blocking.blockSound(),blocking.disableSound()));
        player.stopUsingItem();player.setItemSlot(Slot.OFFHAND,shield);player.setYRot(0);player.setYHeadRot(0);player.startUsingItem(Java.loadClass("net.minecraft.world.InteractionHand").OFF_HAND);}
    try {
        AutoJS.server().setDifficulty(Java.loadClass("net.minecraft.world.Difficulty").NORMAL,true);
        var attributes=cls("attribute.api.CombatAttributes"),nativeAttributes=cls("platform.NativeAttributeAccess");
        player.getAttribute(nativeAttributes.resolve(attributes.critChance())).setBaseValue(0);
        attacker.getAttribute(nativeAttributes.resolve(attributes.critChance())).setBaseValue(0);
        player.getAttribute(nativeAttributes.resolve(attributes.dodgeChance())).setBaseValue(0);
        attacker.getAttribute(nativeAttributes.resolve(attributes.dodgeChance())).setBaseValue(0);
        check(level.getEntity(attacker.getUUID())===attacker,"attacker is registered in the prepared entity lookup area");
        var shield=new Stack(Items.SHIELD);player.setNoGravity(true);raise(shield);
        Block.target=player.getUUID();Block.calls=0;Block.fail=false;
        installation=Block.install(level,player.getUUID(),0.5,Limit.ONCE);
        check(installation.applied(),"public source installs custom after-block behavior and built-in reflection");
        player.getAttribute(cls("platform.NativeAttributeAccess").resolve(cls("attribute.api.CombatAttributes").lifesteal())).setBaseValue(1);
        reset();var start=settlements.length,oldDurability=shield.getDamageValue(),calls=Block.calls;
        check(!hit(4),"full block with abilities remains an unsuccessful native damage hit");
        var snapshot=result(start),receipt=snapshot.evidence().itemBlock().orElseThrow();
        check(snapshot.itemBlocked()&&!snapshot.successfulHit()&&receipt.blocked()===4&&receipt.remaining()===0&&String(receipt.hand())==="OFFHAND"&&String(receipt.item())==="minecraft:shield","full block retains separate item hand and damage facts: "+receipt+" blocked="+snapshot.itemBlocked()+" hit="+snapshot.successfulHit()+" cancelled="+snapshot.cancelled());
        check(Block.calls===calls+1&&Block.durabilityAtCallback>oldDurability&&player.getHealth()===11,"custom callback runs after durability and can submit child healing");
        check(attacker.getHealth()===attacker.getMaxHealth()-2,"reflection uses blocked amount without health loss: "+attacker.getHealth());
        check(Block.retained.childAuthority().isEmpty()&&Block.retained.combatAuthority().isEmpty(),"block callback authority expires");
        var reflected=null;for(var k=start;k<settlements.length;k++)if(String(settlements[k].requestKind())==="REFLECTED")reflected=settlements[k];
        check(reflected!==null&&reflected.lineage().parentDamageId().getAsLong()===snapshot.damageId()&&!reflected.triggerPolicy().reflectionAllowed()&&!reflected.triggerPolicy().procAllowed(),"reflection preserves parent lineage and suppresses recursion and procs");
        reset();mode="partial";start=settlements.length;calls=Block.calls;
        check(hit(6),"partial block retains successful residual damage");snapshot=result(start);receipt=snapshot.evidence().itemBlock().orElseThrow();
        check(receipt.incoming()===6&&receipt.blocked()===2&&receipt.remaining()===4&&snapshot.evidence().healthLoss()===4&&Block.calls===calls+1&&player.getHealth()===7&&attacker.getHealth()===attacker.getMaxHealth()-1,"partial damage settles before one callback and reflects only blocked damage");
        reset();mode="cancel";calls=Block.calls;start=settlements.length;hit(4);
        check(!result(start).itemBlocked()&&Block.calls===calls&&attacker.getHealth()===attacker.getMaxHealth(),"cancelled block grants no block ability");
        reset();mode="zero";calls=Block.calls;start=settlements.length;hit(4);
        check(!result(start).itemBlocked()&&Block.calls===calls,"zero blocked amount grants no block ability");
        reset();mode="normal";Block.fail=true;calls=Block.calls;hit(4);Block.fail=false;
        check(Block.calls===calls+1&&player.getHealth()===10&&attacker.getHealth()===attacker.getMaxHealth()-2,"failing callback preserves block and later reflection executes once");
        reset();start=settlements.length;calls=Block.calls;
        var arrow=Java.loadClass("net.minecraft.world.entity.EntityType").ARROW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);arrow.setOwner(attacker);arrow.setPos(0,512,1);
        try {
            player.hurtServer(level,player.damageSources().arrow(arrow,attacker),4);
            check(result(start).itemBlocked()&&Block.calls===calls+1&&attacker.getHealth()===attacker.getMaxHealth()-2&&player.getHealth()===11,"projectile block reflects to shooter without lifesteal");
            reset();start=settlements.length;calls=Block.calls;
            var problems=Java.loadClass("net.minecraft.util.ProblemReporter").DISCARDING;
            var out=Java.loadClass("net.minecraft.world.level.storage.TagValueOutput").createWithContext(problems,level.registryAccess());arrow.saveWithoutId(out);
            var arrowTag=out.buildResult();arrowTag.putByte("PierceLevel",1);
            arrow.load(Java.loadClass("net.minecraft.world.level.storage.TagValueInput")["create(net.minecraft.util.ProblemReporter,net.minecraft.core.HolderLookup$Provider,net.minecraft.nbt.CompoundTag)"](problems,level.registryAccess(),arrowTag));
            player.hurtServer(level,player.damageSources().arrow(arrow,attacker),4);
            check(!result(start).itemBlocked()&&Block.calls===calls&&attacker.getHealth()===attacker.getMaxHealth(),"piercing bypass produces no item-block ability");
        } finally {arrow.discard();}
        reset();mode="throw";start=settlements.length;calls=Block.calls;var failed=false;
        try{hit(4);}catch(expected){failed=true;}mode="normal";snapshot=result(start);
        check(failed&&!snapshot.itemBlocked()&&snapshot.evidence().itemBlock().isPresent()&&!snapshot.evidence().itemBlock().get().completed()&&Block.calls===calls&&attacker.getHealth()===attacker.getMaxHealth(),"native failure retains incomplete block evidence without benefits");
        reset();shield.setDamageValue(shield.getMaxDamage()-1);calls=Block.calls;start=settlements.length;hit(4);snapshot=result(start);
        check(shield.isEmpty()&&snapshot.itemBlocked()&&String(snapshot.evidence().itemBlock().get().item())==="minecraft:shield"&&Block.calls===calls+1,"breaking shield retains item identity and completed callback");
        check(api.sources().revoke(UUID.randomUUID(),installation.value().orElseThrow()).applied(),"source revocation removes block capability");installation=null;
        raise(new Stack(Items.SHIELD));reset();calls=Block.calls;hit(4);
        check(Block.calls===calls&&attacker.getHealth()===attacker.getMaxHealth(),"revoked source cannot react to later blocks");
        installation=Block.install(level,player.getUUID(),0.5,new Limit(Limit.Scope.SOURCE,1));check(installation.applied(),"source-limited reflection installs");
        reset();hit(4);check(attacker.getHealth()===attacker.getMaxHealth()-2,"first block consumes reflection source allowance");
        reset();hit(4);check(attacker.getHealth()===attacker.getMaxHealth(),"later block does not restore consumed source allowance");
        player.stopUsingItem();reset();start=settlements.length;calls=Block.calls;hit(4);
        check(!result(start).itemBlocked()&&Block.calls===calls,"holding a shield without raising it grants no block ability");
    } finally {
        AutoJS.server().setDifficulty(oldDifficulty,true);
        bus.unregister(listener);bus.unregister(blockListener);bus.unregister(failureListener);Block.target=null;Block.fail=false;
        if(installation!==null&&installation.value().isPresent())api.sources().revoke(UUID.randomUUID(),installation.value().get());
        player.discard();attacker.discard();
    }
    return JSON.stringify({passed:checks.length,checks:checks});
})()
