(function(){
    var mods=Java.loadClass("net.neoforged.fml.ModList").get();
    if(!mods.isLoaded("apothic_attributes"))return JSON.stringify({passed:0,skipped:"optional mod absent"});
    function cls(n){return Java.loadClass("io.github.fatalitaya.combatnexus."+n);}
    var level=AutoJS.server().overworld(),api=cls("combat.api.CombatAccess").find(level).orElseThrow(),checks=[],entities=[],listener=null;
    var UUID=Java.loadClass("java.util.UUID"),Id=Java.loadClass("io.github.fatalitaya.combatnexus.api.id.NexusId"),Optional=Java.loadClass("java.util.Optional");
    function check(v,s){if(!v)throw new Error(s);checks.push(s);}
    function cow(){var e=Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);e.setPos(0,512,0);e.setNoAi(true);e.setNoGravity(true);level.addFreshEntity(e);entities.push(e);return e;}
    function F(x){return Java.loadClass("java.lang.Float").parseFloat(String(x));}
    try{
        var attacker=cow(),victim=cow(),Attr=Java.loadClass("dev.shadowsoffire.apothic_attributes.api.ALObjects$Attributes");
        var defenseAttacker=cow(),defenseTarget=cow(),NativeAttr=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
        defenseAttacker.getAttribute(Attr.CRIT_CHANCE).setBaseValue(0);
        defenseTarget.getAttribute(NativeAttr.MAX_HEALTH).setBaseValue(100);
        defenseTarget.getAttribute(NativeAttr.ARMOR).setBaseValue(20);
        var defenseHit=function(shred,pierce,toughness){
            defenseAttacker.getAttribute(Attr.ARMOR_SHRED).setBaseValue(shred);
            defenseAttacker.getAttribute(Attr.ARMOR_PIERCE).setBaseValue(pierce);
            defenseTarget.getAttribute(NativeAttr.ARMOR_TOUGHNESS).setBaseValue(toughness);
            defenseTarget.setHealth(100);defenseTarget.invulnerableTime=0;
            api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level,defenseTarget,
                cls("combat.damage.platform.source.NativeDamageSources").from(defenseTarget.damageSources().mobAttack(defenseAttacker)),10)
                .logicalAttacker(defenseAttacker).directEntity(defenseAttacker).build());
            return 100-defenseTarget.getHealth();
        };
        check(Math.abs(defenseHit(0,0,0)-5)<0.001,"zero native penetration keeps CombatNexus armor formula");
        check(Math.abs(defenseHit(0.25,2,0)-150/23)<0.001,"native ratio then flat penetration transforms CombatNexus armor input");
        var bypass=Java.loadClass("dev.shadowsoffire.apothic_attributes.api.ALCombatRules").getBypassResistance(10,20,10);
        var armor=20*(1-0.25*(1-bypass))-2*(1-bypass),effective=Math.max(0,armor-10/4.5);
        check(Math.abs(defenseHit(0.25,2,10)-150/(effective+15))<0.001,"native toughness bypass resistance precedes CombatNexus mitigation");
        check(Math.abs(defenseHit(1,100,0)-10)<0.001,"native excess penetration cannot introduce negative armor damage bonus");
        defenseTarget.getAttribute(NativeAttr.ARMOR).setBaseValue(0);
        var protectionItem=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").ELYTRA);
        protectionItem.enchant(level.registryAccess().lookupOrThrow(Java.loadClass("net.minecraft.core.registries.Registries").ENCHANTMENT)
            .getOrThrow(Java.loadClass("net.minecraft.world.item.enchantment.Enchantments").PROTECTION),4);
        defenseTarget.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").CHEST,protectionItem);
        defenseAttacker.getAttribute(Attr.PROT_SHRED).setBaseValue(0.5);
        defenseAttacker.getAttribute(Attr.PROT_PIERCE).setBaseValue(1);
        check(Math.abs(defenseHit(0,0,0)-500/53.5)<0.001,"protection ratio then flat penetration precedes resistance conversion");
        var physicalResistance=defenseTarget.getAttribute(cls("platform.NativeAttributeAccess").resolve(cls("attribute.api.CombatAttributes").resistancePhysical()));
        physicalResistance.setBaseValue(10);
        check(Math.abs(defenseHit(0,0,0)-500/63.5)<0.001,"protection penetration leaves independent channel resistance intact");
        defenseAttacker.getAttribute(Attr.PROT_PIERCE).setBaseValue(100);
        check(Math.abs(defenseHit(0,0,0)-500/60)<0.001,"excess protection penetration clamps only protection score to zero");
        physicalResistance.setBaseValue(0);
        defenseTarget.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").CHEST,Java.loadClass("net.minecraft.world.item.ItemStack").EMPTY);
        defenseTarget.getAttribute(NativeAttr.ARMOR).setBaseValue(20);
        var Definitions=cls("combat.internal.definition.CombatDefinitions"),Generation=cls("combat.internal.definition.DefinitionGeneration");
        var kernel=cls("combat.internal.transaction.CombatServerRuntime").of(AutoJS.server()),original=Definitions.forRuntime(kernel);
        var Settings=cls("combat.config.CombatServerSettings"),Compatibility=cls("combat.damage.config.VanillaCompatibilitySettings"),Mode=cls("combat.damage.config.VanillaReductionCompatibilityMode");
        var adoptMode=function(mode,suppress){Definitions.publish(kernel,new Generation(Definitions.nextGeneration(),
            new Settings(original.settings().formulas(),new Compatibility(mode,suppress,suppress,suppress,suppress),original.settings().safety(),original.settings().equipment()),
            original.damage(),original.behaviors(),original.domains(),original.itemBindings()));};
        try {
            for(var nativeMode of [Mode.COOPERATIVE,Mode.CONFIGURABLE]) {
                adoptMode(nativeMode,false);
                var nativeLoss=defenseHit(0.25,2,0);
                var expectedNative=Java.loadClass("dev.shadowsoffire.apothic_attributes.api.ALCombatRules").getDamageAfterArmor(
                    defenseTarget,defenseTarget.damageSources().mobAttack(defenseAttacker),10,20,0);
                check(Math.abs(nativeLoss-expectedNative)<0.001,"native-owned armor consumes penetration exactly once in "+nativeMode);
            }
            adoptMode(Mode.CONFIGURABLE,true);
            check(Math.abs(defenseHit(0.25,2,0)-150/23)<0.001,"configurable CombatNexus-owned armor consumes penetration once");
        } finally {
            Definitions.publish(kernel,new Generation(Definitions.nextGeneration(),original.settings(),original.damage(),original.behaviors(),original.domains(),original.itemBindings()));
        }
        attacker.getAttribute(Attr.CRIT_CHANCE).setBaseValue(2.25);attacker.getAttribute(Attr.CRIT_DAMAGE).setBaseValue(3);
        var random=Java.loadClass("net.minecraft.util.RandomSource").create(7123),expected=10,chance=2.25,multiplier=3,samples=[],multipliers=[];
        while(true){var sample=random.nextFloat();samples.push(sample);if(!(sample<=chance&&multiplier>1))break;
            chance--;expected=F(expected+F(10*F(multiplier-1)));multipliers.push(multiplier);multiplier=F(multiplier*F(0.85));}
        victim.getRandom().setSeed(7123);
        var request=new (cls("combat.api.bridge.DamageFormulaRequest"))(UUID.randomUUID(),Id.parse("apothic_attributes:multi_critical"),victim.getUUID(),Optional.of(attacker.getUUID()),10,Optional.empty(),Optional.empty());
        var result=api.damageFormula(request);
        check(result.completed()&&Math.abs(result.output().amount()-expected)<0.00001,"multi-critical equals native float algorithm");
        check(result.samples().size()===samples.length&&result.output().multipliers().size()===multipliers.length,"multi-critical records every sample and multiplier");
        for(var i=0;i<samples.length;i++)check(result.samples().get(i).floatValue()===samples[i],"recorded critical sample "+i);
        var duplicate=false;try{api.damageFormula(request);}catch(e){duplicate=true;}check(duplicate,"formula invocation cannot reroll");
        victim.getRandom().setSeed(7123);
        var event=new (Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"))(victim,
            new (Java.loadClass("net.neoforged.neoforge.common.damagesource.DamageContainer"))(victim.damageSources().mobAttack(attacker),10));
        new (Java.loadClass("dev.shadowsoffire.apothic_attributes.impl.AttributeEvents"))().apothCriticalStrike(event);
        check(Math.abs(event.getAmount()-expected)<0.00001,"native critical entry replaced exactly once");
        var over=cow(),overVictim=cow();over.getAttribute(Attr.CRIT_CHANCE).setBaseValue(0);over.getAttribute(Attr.OVERHEAL).setBaseValue(1);
        api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level,overVictim,cls("combat.damage.platform.source.NativeDamageSources").from(overVictim.damageSources().mobAttack(over)),4).logicalAttacker(over).directEntity(over).build());
        check(over.getAbsorptionAmount()===4,"Overheal original post-hit route grants once");
        check(api.absorption().snapshot(over.getUUID()).size()===1&&api.absorption().snapshot(over.getUUID()).get(0).granted()===4,"Overheal source owns the actual native absorption");
        var leech=cow(),leechTarget=cow();leech.getAttribute(Attr.CRIT_CHANCE).setBaseValue(0);
        leech.getAttribute(Attr.LIFE_STEAL).setBaseValue(0.5);leech.setHealth(5);
        leechTarget.hurtServer(level,leechTarget.damageSources().mobAttack(leech),4);
        check(Math.abs(leech.getHealth()-7)<0.001,"native life steal replaced by one confirmed primary settlement heal");
        leechTarget.invulnerableTime=0;leechTarget.setHealth(1);leech.setHealth(5);
        leechTarget.hurtServer(level,leechTarget.damageSources().mobAttack(leech),10);
        check(Math.abs(leech.getHealth()-5.5)<0.001,"native life steal caps gain to confirmed remaining health");
        var leechVictim=cow(),Kinds=cls("combat.damage.api.damage.DamageRequestKind"),Request=cls("combat.damage.api.damage.DamageRequest"),Sources=cls("combat.damage.platform.source.NativeDamageSources");
        var leechHit=function(kind){leechVictim.invulnerableTime=0;leechVictim.setHealth(leechVictim.getMaxHealth());leech.setHealth(5);
            return api.damage(Request.builder(level,leechVictim,Sources.from(leechVictim.damageSources().mobAttack(leech)),4).logicalAttacker(leech).directEntity(leech).kind(kind).build());};
        for(var kind of [Kinds.SWEEP]) {
            leechHit(kind);check(leech.getHealth()===5,"native life steal excludes "+kind);
        }
        leechVictim.invulnerableTime=0;leech.setHealth(5);
        leechVictim.hurtServer(level,leechVictim.damageSources().thorns(leech),4);
        check(leech.getHealth()===5,"native thorns grants no life steal");
        var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS,Consumer=Java.loadClass("java.util.function.Consumer"),Priority=Java.loadClass("net.neoforged.bus.api.EventPriority");
        listener=new Consumer({accept:function(event){if(event.getEntity()===leechVictim)leech.getAttribute(Attr.LIFE_STEAL).setBaseValue(1);}});
        bus.addListener(Priority.LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre"),listener);
        leechHit(Kinds.PRIMARY);check(Math.abs(leech.getHealth()-7)<0.001,"life steal ratio frozen before native damage callbacks");
        bus.unregister(listener);listener=null;leech.getAttribute(Attr.LIFE_STEAL).setBaseValue(0.5);
        listener=new Consumer({accept:function(event){if(event.getEntity()===leech)event.setCanceled(true);}});
        bus.addListener(Priority.NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"),listener);
        leechHit(Kinds.PRIMARY);check(leech.getHealth()===5&&leechVictim.getHealth()===leechVictim.getMaxHealth()-4,"cancelled life steal cannot fall back to native healing or undo damage");
        bus.unregister(listener);listener=null;
        leechVictim.getAttribute(NativeAttr.MAX_ABSORPTION).setBaseValue(10);leechVictim.setAbsorptionAmount(10);
        leechHit(Kinds.PRIMARY);check(leech.getHealth()===5,"absorption-only damage grants no life steal");
        leechVictim.setAbsorptionAmount(0);
        var shotOwner=cow(),shotTarget=cow(),shot=Java.loadClass("net.minecraft.world.entity.EntityType").ARROW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        entities.push(shot);shot.setOwner(shotOwner);shotOwner.getAttribute(Attr.PROJECTILE_DAMAGE).setBaseValue(2);
        shotOwner.getAttribute(Attr.CRIT_CHANCE).setBaseValue(0);
        shotOwner.getAttribute(Attr.ARMOR_SHRED).setBaseValue(1);shotOwner.getAttribute(Attr.PROT_SHRED).setBaseValue(1);shotOwner.getAttribute(Attr.PROT_PIERCE).setBaseValue(1);
        api.projectiles().capture(UUID.randomUUID(),shotOwner.getUUID(),shot);
        shot.setPos(0,512,0);shot.setNoGravity(true);level.addFreshEntity(shot);
        shotOwner.getAttribute(Attr.PROJECTILE_DAMAGE).setBaseValue(7);
        shotOwner.getAttribute(Attr.ARMOR_SHRED).setBaseValue(0);shotOwner.getAttribute(Attr.PROT_SHRED).setBaseValue(0);shotOwner.getAttribute(Attr.PROT_PIERCE).setBaseValue(0);
        shotTarget.getAttribute(NativeAttr.ARMOR).setBaseValue(20);
        shotTarget.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").CHEST,protectionItem);
        var projectileEvent=function(owner){return new (Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"))(shotTarget,
            new (Java.loadClass("net.neoforged.neoforge.common.damagesource.DamageContainer"))(shotTarget.damageSources().arrow(shot,owner),3));};
        var pe=projectileEvent(shotOwner);new (Java.loadClass("dev.shadowsoffire.apothic_attributes.impl.AttributeEvents"))().projDmg(pe);
        check(pe.getAmount()===3,"native projectile multiplier is suppressed for managed damage");
        var hpBefore=shotTarget.getHealth();shotTarget.hurtServer(level,shotTarget.damageSources().arrow(shot,shotOwner),3);
        check(Math.abs(hpBefore-shotTarget.getHealth()-6)<0.001,"actual projectile damage applies frozen multiplier once: loss="+(hpBefore-shotTarget.getHealth())+" shred="+api.projectiles().snapshot(shot).get().shooter().attributes().valueOrZero(Id.parse("apothic_attributes:armor_shred")));
        shotOwner.discard();pe=projectileEvent(null);new (Java.loadClass("dev.shadowsoffire.apothic_attributes.impl.AttributeEvents"))().projDmg(pe);
        check(pe.getAmount()===3,"native projectile multiplier stays suppressed with unavailable shooter");
        shotTarget.setHealth(shotTarget.getMaxHealth());hpBefore=shotTarget.getHealth();shotTarget.invulnerableTime=0;shotTarget.hurtServer(level,shotTarget.damageSources().arrow(shot,null),3);
        check(Math.abs(hpBefore-shotTarget.getHealth()-6)<0.001,"armor and protection penetration survive unavailable shooter through launch attribution");
        if(mods.isLoaded("apotheosis")){
            var mage=cow(),Items=Java.loadClass("net.minecraft.world.item.Items"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
            var helmet=new Stack(Items.DIAMOND_HELMET),Socket=Java.loadClass("dev.shadowsoffire.apotheosis.socket.SocketHelper");
            var gem=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemRegistry").INSTANCE.holder(Java.loadClass("io.github.fatalitaya.combatnexus.platform.NativeIds").nativeId(Id.parse("apotheosis:the_end/mageslayer"))).get();
            var gemStack=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemItem").createStack(gem,Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.Purity").PERFECT);
            Socket.setSockets(helmet,1);helmet=Socket.socketGemInItem(helmet,gemStack);mage.setItemSlot(Slot.HEAD,helmet);
            var magic=function(){mage.invulnerableTime=0;return api.damage(cls("combat.damage.api.damage.DamageRequest").builder(level,mage,cls("combat.damage.platform.source.NativeDamageSources").from(mage.damageSources().magic()),4).build());};
            mage.setHealth(5);var mageResult=magic(),plan=mageResult.settlement().orElseThrow().evidence().beforeApplication();
            check(plan.steps().size()===1&&plan.completed(),"MageSlayer native hook retains child evidence");
            check(Math.abs(mage.getHealth()-3.8)<0.001,"MageSlayer heals before reduced damage exactly once");
            mage.setHealth(mage.getMaxHealth());mageResult=magic();plan=mageResult.settlement().orElseThrow().evidence().beforeApplication();
            var fullStep=plan.steps().get(0),fullJournal=fullStep.journal().entries();
            check(!fullStep.applied()&&fullStep.rejection().isEmpty()&&fullStep.journal().applicationCompleted()
                &&fullJournal.size()===1&&fullJournal.get(0).before().orElseThrow()===mage.getMaxHealth()
                &&fullJournal.get(0).confirmedAmount()===0&&plan.continueParent()
                &&Math.abs(mage.getHealth()-(mage.getMaxHealth()-2.6))<0.001,"MageSlayer full-health child records zero recovery and continues reduction");
            var bus=Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS,Consumer=Java.loadClass("java.util.function.Consumer"),Priority=Java.loadClass("net.neoforged.bus.api.EventPriority");
            var cancelledHeals=0;
            listener=new Consumer({accept:function(event){if(event.getEntity()===mage){cancelledHeals++;event.setCanceled(true);}}});
            bus.addListener(Priority.NORMAL,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingHealEvent"),listener);
            mage.setHealth(5);mageResult=magic();plan=mageResult.settlement().orElseThrow().evidence().beforeApplication();
            var cancelledStep=plan.steps().get(0);
            check(cancelledHeals===1&&!cancelledStep.applied()&&cancelledStep.rejection().isEmpty()
                &&cancelledStep.journal().entries().stream().noneMatch(function(e){return e.confirmedAmount()>0;})
                &&plan.continueParent()&&Math.abs(mage.getHealth()-2.4)<0.001,"MageSlayer cancelled child still keeps frozen damage reduction");
            bus.unregister(listener);listener=null;
            listener=new Consumer({accept:function(event){if(event.getEntity()===mage)event.setCanceled(true);}});
            bus.addListener(Priority.LOWEST,false,Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"),listener);
            mage.setHealth(5);mageResult=magic();
            check(Math.abs(mage.getHealth()-6.4)<0.001&&mageResult.settlement().orElseThrow().evidence().beforeApplication().steps().size()===1,"MageSlayer parent cancellation retains committed child without replay");
            bus.unregister(listener);listener=null;
            var archer=cow(),bow=new Stack(Items.BOW),GemItem=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemItem");
            archer.getAttribute(Attr.CRIT_CHANCE).setBaseValue(0);
            var blood=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemRegistry").INSTANCE.holder(Java.loadClass("io.github.fatalitaya.combatnexus.platform.NativeIds").nativeId(Id.parse("apotheosis:the_nether/blood_lord"))).get();
            Socket.setSockets(bow,1);bow=Socket.socketGemInItem(bow,GemItem.createStack(blood,Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.Purity").FLAWED));
            archer.setItemSlot(Slot.MAINHAND,bow);
            var arrow=Java.loadClass("net.minecraft.world.entity.EntityType").ARROW.create(level,Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);entities.push(arrow);arrow.setOwner(archer);
            var bloodInstance=Socket.getGems(bow).gems().get(0),beforeHealth=archer.getHealth();
            bloodInstance.onProjectileFired(archer,arrow);
            check(Math.abs(arrow.baseDamage-3.2)<0.001&&Math.abs(archer.getHealth()-(beforeHealth-archer.getMaxHealth()*0.2))<0.001,"BloodyArrow applies one native damage cost and one arrow multiplier");
            var launch=api.projectiles().snapshot(arrow).orElseThrow();
            var explanations=api.sourceDescriptions(launch.shooter());
            check(explanations.stream().anyMatch(function(e){return String(e.source())==="apotheosis:the_nether/blood_lord";}),"source descriptions use frozen gem slot and purity");
            archer.setItemSlot(Slot.MAINHAND,Stack.EMPTY);
            check(!launch.shooter().facts().equipment(Java.loadClass("io.github.fatalitaya.combatnexus.api.equipment.EquipmentPosition").MAINHAND).isEmpty(),"projectile launch equipment survives shooter swap");
            bloodInstance.onProjectileFired(archer,arrow);
            check(Math.abs(arrow.baseDamage-3.2)<0.001,"BloodyArrow cooldown survives unequip and prevents repeat");
            archer.getAttribute(Attr.CRIT_CHANCE).setBaseValue(7);
            var frozenFormula=api.damageFormula(new (cls("combat.api.bridge.DamageFormulaRequest"))(UUID.randomUUID(),Id.parse("apothic_attributes:multi_critical"),mage.getUUID(),Optional.of(archer.getUUID()),2,Optional.of(launch.action()),Optional.of(launch)));
            check(frozenFormula.output().multipliers().isEmpty(),"projectile critical calculation retains launch attributes after swap");
            archer.discard();
            for(var shotTarget of [mage,overVictim]) {
                var collision=new (Java.loadClass("net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent"))(shotTarget,
                    new (Java.loadClass("net.neoforged.neoforge.common.damagesource.DamageContainer"))(shotTarget.damageSources().arrow(arrow,archer),2));
                new (Java.loadClass("dev.shadowsoffire.apothic_attributes.impl.AttributeEvents"))().apothCriticalStrike(collision);
                check(collision.getAmount()===2,"projectile native critical retains frozen source after removal on target "+shotTarget.getUUID());
            }
            var affix=Java.loadClass("dev.shadowsoffire.apotheosis.affix.AffixRegistry").INSTANCE.holder(Java.loadClass("io.github.fatalitaya.combatnexus.platform.NativeIds").nativeId(Id.parse("apotheosis:melee/executing")));
            var rarity=Java.loadClass("dev.shadowsoffire.apotheosis.loot.RarityRegistry").INSTANCE.holder(Java.loadClass("io.github.fatalitaya.combatnexus.platform.NativeIds").nativeId(Id.parse("apotheosis:mythic")));
            var instance=new (Java.loadClass("dev.shadowsoffire.apotheosis.affix.AffixInstance"))(affix,1,rarity,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").DIAMOND_SWORD));
            victim.setHealth(1);
            victim.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").OFFHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").TOTEM_OF_UNDYING));
            instance.doPostAttack(attacker,victim);
            check(victim.isAlive()&&victim.getHealth()===1&&victim.getOffhandItem().isEmpty(),"Executing routes to shared protection and consumes one totem");
            instance.doPostAttack(attacker,victim);
            check(!victim.isAlive(),"Executing kills after native protection consumed");
            var history=api.bridges().history(),seen={};
            for(var h=0;h<history.size();h++){var invocation=history.get(h);seen[String(invocation.capability())]=true;
                check(String(invocation.nativeContribution())==="SUPPRESSED","native invocation suppressed: "+invocation.capability());}
            check(seen["apotheosis:bloody_arrow"]&&seen["apotheosis:mageslayer"]&&seen["apotheosis:executing"]&&seen["apothic_attributes:overheal"]&&seen["apothic_attributes:multi_critical"],"replacement invocations expose per-call evidence");
        }
    } finally {if(listener!==null)Java.loadClass("net.neoforged.neoforge.common.NeoForge").EVENT_BUS.unregister(listener);for(var i=0;i<entities.length;i++)entities[i].discard();}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
